package org.grails.cli

import static net.sf.expectit.matcher.Matchers.*
import grails.build.logging.GrailsConsole

import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

import jline.console.KeyMap
import jnr.posix.POSIXFactory
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class GrailsCliSpec extends Specification {
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    GrailsCli cli
    File currentAppDir
    @Shared File previousUserDir
    @Shared Map originalStreams
    
    PipedOutputStream expectCommandsPipe
    PipedOutputStream expectSystemOutPipe
    
    def setup() {
        System.setProperty("grails.show.stacktrace", "true")
        GrailsConsole.removeInstance()
        cli = new GrailsCli(ansiEnabled: false, defaultInputMask: 0)
        cli.profileRepository.initialized = true
        cli.profileRepository.profilesDirectory = new File(previousUserDir, 'src/test/resources/profiles-repository').absoluteFile
        
        chdir(tempFolder.getRoot())
    }

    def setupSpec() {
        originalStreams = [out: System.out, in: System.in, err: System.err]
        previousUserDir = new File("").absoluteFile
        disableFileCanonCaches()
        System.setProperty("jansi.passthrough", "true")
        System.setProperty("jline.terminal", TestTerminal.name)
    }
    
    def cleanup() {
        expectCommandsPipe?.close()
        expectSystemOutPipe?.close()

        GrailsConsole.removeInstance()
                
        System.setIn(originalStreams.in)
        System.setOut(originalStreams.out)
        System.setErr(originalStreams.err)
        chdir(previousUserDir)
    }
    
    private static void chdir(File newDir) {
        String path = newDir.absolutePath
        System.setProperty("user.dir", path)
        // use native library (jnr-posix) to change working directory
        // in Java it's not possible to reliably change the working directory (changing user.dir isn't enough)
        // http://bugs.java.com/view_bug.do?bug_id=4045688
        POSIXFactory.getPOSIX().chdir(path)
    }
    
    private static void disableFileCanonCaches() {
        Class fileSystemClazz = Class.forName("java.io.FileSystem")
        ["useCanonCaches","useCanonPrefixCache"].each { String fieldName ->
            System.setProperty("sun.io.$fieldName", "false")
            Field field = fileSystemClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            field.setBoolean(null, false)
        }
    }

    private String expectPrompt(Expect expect) {
        expect.expect(contains("grails> ")).before
    }
    
    private String expectAnyOutput(Expect expect) {
        expect.expect(anyString()).group()
    }

    void println(Object value) {
        def out = originalStreams.out ?: System.out
        out.println(InvokerHelper.toString(value))
    }
    
    private ExpectBuilder createExpectsBuilderWithSystemInOut() {
        PipedInputStream emulatedSystemIn = new PipedInputStream()
        expectCommandsPipe = new PipedOutputStream(emulatedSystemIn)
        System.setIn(emulatedSystemIn)

        PipedInputStream systemOutInput = new PipedInputStream()
        expectSystemOutPipe = new PipedOutputStream(systemOutInput)
        PrintStream emulatedSystemOut = new PrintStream(expectSystemOutPipe, true)
        System.setOut(emulatedSystemOut)
        System.setErr(emulatedSystemOut)

        ExpectBuilder expectBuilder = new ExpectBuilder().withInputs(systemOutInput).withOutput(expectCommandsPipe).withTimeout(20, TimeUnit.SECONDS)
        expectBuilder.withEchoOutput(originalStreams.err).withEchoInput(originalStreams.out)
        expectBuilder
    }

    private File createApp() {
        cli.execute('create-app','newapp','--profile=web')
        File appdir = new File(tempFolder.getRoot(), 'newapp')
        return appdir
    }
    
    private int executeInInteractiveMode(boolean exitByDefault = true, Closure closure) {
        currentAppDir = createApp()
        chdir(currentAppDir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        // redirect System.in, System.out, System.err in GrailsConsole
        def systemIn = System.in
        def systemOut = System.out
        def systemErr = System.err
        GrailsConsole.getInstance().reinitialize(systemIn, systemOut, systemErr)
        Thread cliThread = new Thread({-> 
                try {
                    retval=cli.execute()
                } catch (Throwable t) {
                    t.printStackTrace(originalStreams.err)
                }
            } as Runnable, "cli-thread")
        cliThread.start()
        Expect expect = expectBuilder.build()
        closure.call(expect)
        if(exitByDefault) {
            expect.send("\340\000") // Operation.KILL_WHOLE_LINE
            expect.sendLine("exit")
        }
        expect.close()
        cliThread.join()
        retval
    }
    
    def "should create new application"() {
        when:
        File appdir = createApp()
        then:
        assert appdir.exists() 
        assert new File(appdir, "grails-app").exists()
    }    

    def "should fail with retval 1 when creating app for non-existing profile"() {
        when:
        def bytesOut = new ByteArrayOutputStream()
        def printStream = new PrintStream(bytesOut, true)
        System.setOut(printStream)
        System.setErr(printStream)
        int retval = cli.execute('create-app','newapp','--profile=no_such_profile')
        def msg = bytesOut.toString()
        then:
        retval == 1
        msg == 'Cannot find profile no_such_profile\n'
    }
    
    def "should start and exit interactive mode"() {
        when:
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
        }        
        then:
        retval == 0
    }
    
    def "should provide help for all commands in interactive mode"() {
        when:
        def helpContent
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("help")
            helpContent = expectPrompt(expect)
        }        
        then:
        retval == 0
        helpContent == '''
create-controller\tCreates a controller
create-domain\tCreates a domain class
create-service\tCreates a service
create-taglib\tCreates a tag library
gradle\tRuns the gradle build
run-app\tRuns the application
detailed usage with help [command]
'''
    }
    
    def "should provide detailed help commands in interactive mode"() {
        when:
        def helpContent
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("help create-controller")
            helpContent = expectPrompt(expect)
        }        
        then:
        retval == 0
        helpContent == '''
create-controller\tCreates a controller
create-controller [controller name]
Creates a controller class and an associated unit test
'''
    }
    
    def "should show error message when create-controller is executed without proper arguments"() {
        when:
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("create-controller")
            message = expectPrompt(expect)
        }
        then:
        retval == 0
        message == '''
Error |
Expecting an argument to create-controller.
create-controller [controller name]
Creates a controller class and an associated unit test
'''
    }
    
    def "should create-controller in default package"() {
        when:
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("create-controller ShoppingBasket")
            message = expectPrompt(expect)
        }
        then:
        retval == 0
        message == '''
Creating grails-app/controllers/newapp/ShoppingBasket.groovy
Creating src/test/groovy/newapp/ShoppingBasketSpec.groovy
'''
        new File(currentAppDir, 'grails-app/controllers/newapp/ShoppingBasket.groovy').text == '''package newapp
class ShoppingBasket {

    def index() { }
}
'''
    }
    
    
    def "should complete available commands up to longest match"() {
        when:
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.send("cr")
            sleep(500)
            expect.send("\t")
            sleep(100)
            message = expectAnyOutput(expect)
        }
        then:
        retval == 0
        message == '''  

create-controller   create-domain       create-service      create-taglib       
grails> create-'''
    }
    
    def "should complete commands fully if only match"() {
        when:
        def message
        cli.defaultInputMask = null
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.send("create-c")
            sleep(500)
            expect.send("\t")
            sleep(100)
            message = expectAnyOutput(expect)
        }
        then:
        retval == 0
        message == '''create-c        create-controller '''
    }
    
    def "should exit when CTRL-D is pressed"() {
        when:
        int retval = executeInInteractiveMode(false) { Expect expect ->
            expectPrompt(expect)
            expect.send(String.valueOf(KeyMap.CTRL_D))
        }
        then:
        retval == 0
    }
    
    @Unroll
    def "should not exit when command throws exception - showStacktrace:#showStacktrace"() {
        when:
        if(showStacktrace) GrailsConsole.getInstance().stacktrace = true
        CommandLineHandler handler = [handleCommand: { ExecutionContext context ->
            if(context.commandLine.commandName == 'broken-command') { 
                throw new RuntimeException("This is broken.")
            }
            return false
        }] as CommandLineHandler
        cli.profileRepository.getProfile('web').getCommandLineHandlers(null) << handler
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("broken-command")
            message = expectPrompt(expect)
        }
        then:
        retval == 0
        message in expectedMessage
        where:
        showStacktrace << [false, true]
        expectedMessage << [~'''
Error \\|
Caught exception This is broken\\. \\(Use --stacktrace to see the full trace\\)
''', ~'''
Error \\|
Caught exception This is broken. \\(NOTE: Stack trace has been filtered. Use --verbose to see entire trace.\\)
java.lang.RuntimeException: This is broken.
\tat org.grails.cli.GrailsCliSpec.*
\tat .*
\tat .*
\tat .*
\tat org.grails.cli.GrailsCli.execute\\(GrailsCli.groovy:\\d+\\)
\tat .*

Error \\|
Caught exception This is broken.
''']
    }
}
