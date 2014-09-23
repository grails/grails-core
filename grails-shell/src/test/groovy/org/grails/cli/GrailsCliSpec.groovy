package org.grails.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class GrailsCliSpec extends Specification {
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    GrailsCli cli
    String previousUserDir
    
    def setup() {
        cli = new GrailsCli()
        previousUserDir = System.getProperty("user.dir")
        System.setProperty("user.dir", tempFolder.getRoot().getAbsolutePath())
    }
    
    def cleanup() {
        System.setProperty("user.dir", previousUserDir)
    }

    def "should create new application"() {
        when:
        cli.run('create-app','newapp','--profile=web')
        File appdir = new File(tempFolder.getRoot(), 'newapp')
        then:
        appdir.exists() 
    }    
}
