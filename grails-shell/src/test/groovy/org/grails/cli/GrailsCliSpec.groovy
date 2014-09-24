package org.grails.cli

import static net.sf.expectit.matcher.Matchers.*
import grails.build.logging.GrailsConsole;

import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

import jnr.posix.POSIXFactory
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.Result

import org.codehaus.groovy.runtime.InvokerHelper
import org.junit.Rule
import org.junit.rules.TemporaryFolder

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
        cli = new GrailsCli(ansiEnabled: false)
        chdir(tempFolder.getRoot())
    }

    def setupSpec() {
        originalStreams = [out: System.out, in: System.in, err: System.err]
        previousUserDir = new File("").absoluteFile
        disableFileCanonCaches()
        System.setProperty("jansi.passthrough", "true")
        System.setProperty("jline.terminal", "jline.UnsupportedTerminal")
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

        ExpectBuilder expectBuilder = new ExpectBuilder().withInputs(systemOutInput).withOutput(expectCommandsPipe).withTimeout(5, TimeUnit.SECONDS)
        expectBuilder.withEchoOutput(originalStreams.err).withEchoInput(originalStreams.out)
        expectBuilder
    }

    private File createApp() {
        cli.execute('create-app','newapp','--profile=web')
        File appdir = new File(tempFolder.getRoot(), 'newapp')
        return appdir
    }
    
    def "should create new application"() {
        when:
        File appdir = createApp()
        then:
        assert appdir.exists() 
        assert new File(appdir, "application.properties").exists()
    }    

    def "should start and exit interactive mode"() {
        when:
        File appdir = createApp()
        assert new File(appdir, "application.properties").exists()
        chdir(appdir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        Thread cliThread = new Thread({-> retval=cli.execute()} as Runnable)
        cliThread.start()
        Expect expect = expectBuilder.build()
        expectPrompt(expect)
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        then:
        retval == 0
    }
    
    def "should provide help for all commands in interactive mode"() {
        when:
        File appdir = createApp()
        assert new File(appdir, "application.properties").exists()
        chdir(appdir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        Thread cliThread = new Thread({-> retval=cli.execute()} as Runnable)
        cliThread.start()
        Expect expect = expectBuilder.build()
        expectPrompt(expect)
        expect.sendLine("help")
        def helpContent = expectPrompt(expect).before
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        then:
        retval == 0
        helpContent == '''create-controller\tCreates a controller
create-domain\tCreates a domain class
create-service\tCreates a service
create-taglib\tCreates a tag library
detailed usage with help [command]
'''
    }
    
    def "should provide detailed help commands in interactive mode"() {
        when:
        File appdir = createApp()
        assert new File(appdir, "application.properties").exists()
        chdir(appdir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        Thread cliThread = new Thread({-> retval=cli.execute()} as Runnable)
        cliThread.start()
        Expect expect = expectBuilder.build()
        expectPrompt(expect)
        expect.sendLine("help create-controller")
        def helpContent = expectPrompt(expect).before
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        then:
        retval == 0
        helpContent == '''create-controller\tCreates a controller
create-controller [controller name]
Creates a controller class and an associated unit test
'''
    }
}
