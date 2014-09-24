package org.grails.cli

import java.lang.reflect.Field

import jnr.posix.POSIXFactory
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Shared
import spock.lang.Specification

import static net.sf.expectit.matcher.Matchers.*

class GrailsCliSpec extends Specification {
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    GrailsCli cli
    @Shared File previousUserDir
    @Shared Map originalStreams
    
    def setup() {
        cli = new GrailsCli(ansiEnabled: false)
        chdir(tempFolder.getRoot())
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
    
    def setupSpec() {
        originalStreams = [out: System.out, in: System.in, err: System.err]
        previousUserDir = new File("").absoluteFile
        disableFileCanonCaches()
        System.setProperty("jansi.passthrough", "true")
    }
    
    def cleanupSpec() {
        System.setIn(originalStreams.in)
        System.setOut(originalStreams.out)
        System.setErr(originalStreams.err)
        chdir(previousUserDir)
    }

    def "should create new application"() {
        when:
        File appdir = createApp()
        then:
        assert appdir.exists() 
        assert new File(appdir, "application.properties").exists()
    }    

    private File createApp() {
        cli.execute('create-app','newapp','--profile=web')
        File appdir = new File(tempFolder.getRoot(), 'newapp')
        return appdir
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
        expect.sendLine("help")
        def grailsPrompt = ~/.+/
        expect.expect(matches(grailsPrompt))
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        then:
        retval == 0
    }
    
    def "should provide help in interactive mode"() {
        when:
        File appdir = createApp()
        assert new File(appdir, "application.properties").exists()
        chdir(appdir)
        int retval = -1
        ExpectBuilder expectBuilder = createExpectsBuilderWithSystemInOut()
        Thread cliThread = new Thread({-> retval=cli.execute()} as Runnable)
        cliThread.start()
        Expect expect = expectBuilder.build()
        expect.sendLine("help")
        expect.sendLine("exit")
        expect.close()
        cliThread.join()
        then:
        retval == 0
    }

    private ExpectBuilder createExpectsBuilderWithSystemInOut() {
        PipedInputStream emulatedSystemIn = new PipedInputStream()
        PipedOutputStream commandsOutput = new PipedOutputStream(emulatedSystemIn)
        System.setIn(emulatedSystemIn)

        PipedInputStream systemOutInput = new PipedInputStream()
        PrintStream emulatedSystemOut = new PrintStream(new PipedOutputStream(systemOutInput), true)
        System.setOut(emulatedSystemOut)
        System.setErr(emulatedSystemOut)

        new ExpectBuilder().withInputs(systemOutInput).withOutput(commandsOutput)
    }
}
