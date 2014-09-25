package org.grails.cli

import static net.sf.expectit.matcher.Matchers.*
import grails.build.logging.GrailsConsole

import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

import jnr.posix.POSIXFactory
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.Result

import org.codehaus.groovy.runtime.InvokerHelper
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class GrailsCliSpec extends Specification {
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    GrailsCli cli
    @Shared File previousUserDir
    @Shared Map originalStreams
    
    PipedOutputStream expectCommandsPipe
    PipedOutputStream expectSystemOutPipe
    
    def setup() {
        GrailsConsole.removeInstance()
        cli = new GrailsCli(ansiEnabled: false, defaultInputMask: 0)
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

    private Result expectPrompt(Expect expect) {
        expect.expect(contains("grails> "))
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
    
    private int executeInInteractiveMode(Closure closure) {
        File appdir = createApp()
        chdir(appdir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        Thread cliThread = new Thread({-> retval=cli.execute()} as Runnable)
        cliThread.start()
        Expect expect = expectBuilder.build()
        closure.call(expect)
        expect.send("\340\000") // Operation.KILL_WHOLE_LINE
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        retval
    }
    
    def "should create new application"() {
        when:
        File appdir = createApp()
        then:
        assert appdir.exists() 
        assert new File(appdir, "application.properties").exists()
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
        def helpContent
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
            helpContent = expectPrompt(expect).before
        }        
        then:
        retval == 0
        helpContent == '''
create-controller\tCreates a controller
create-domain\tCreates a domain class
create-service\tCreates a service
create-taglib\tCreates a tag library
detailed usage with help [command]
'''
    }
    
    def "should provide detailed help commands in interactive mode"() {
        when:
        def helpContent
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("help create-controller")
            helpContent = expectPrompt(expect).before
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
            message = expectPrompt(expect).before
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
    
    @Ignore
    def "should create-controller in default package"() {
        when:
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.sendLine("create-controller ShoppingBasket")
            message = expectPrompt(expect).before
        }
        then:
        retval == 0
        message == '''Creating controller.
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
            message = expect.expect(anyString()).group()
        }
        then:
        retval == 0
        message == '''  

create-controller   create-domain       create-service      create-taglib       
grails> create-'''
    }
    
    @Ignore
    def "should complete commands fully if only match"() {
        when:
        def message
        int retval = executeInInteractiveMode { Expect expect ->
            expectPrompt(expect)
            expect.send("create-c")
            sleep(500)
            expect.send("\t")
            sleep(100)
            message = expect.expect(anyString()).group()
        }
        then:
        retval == 0
        message == '''

grails> create-controller '''
    }
}
