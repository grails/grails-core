package org.grails.cli

import java.lang.reflect.Field

import jnr.posix.POSIXFactory

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
    
    def setup() {
        cli = new GrailsCli()
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
        cli.run('create-app','newapp','--profile=web')
        File appdir = new File(tempFolder.getRoot(), 'newapp')
        return appdir
    }
    
    def "should start interactive mode"() {
        when:
        File appdir = createApp()
        assert new File(appdir, "application.properties").exists()
        chdir(appdir)
        int retval = cli.run()
        then:
        retval == 0
    }
}
