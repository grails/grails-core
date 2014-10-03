package org.codehaus.groovy.grails.project.creation

import grails.util.BuildSettings
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import spock.lang.Specification

class GrailsProjectCleanerSpec extends Specification {

    private static STAGING_DIR = new File('/staging/dir')

    private grailsAntMock,
            buildSettingsMock,
            listenerMock,
            grailsProjectCleaner

    def setup() {
        grailsAntMock = GroovyMock(GrailsConsoleAntBuilder)
        buildSettingsMock = new BuildSettings()
        listenerMock = Stub(GrailsBuildEventListener)
    }

    def 'test that exploded war is deleted'() {
        given:
        grailsProjectCleaner = new GrailsProjectCleaner(buildSettingsMock, listenerMock)
        grailsProjectCleaner.ant = grailsAntMock

        when:
        grailsProjectCleaner.cleanExplodedWar()

        then:
        1 * grailsAntMock.delete(_)
    }

    def 'test that exploded war directory is used'() {
        given:
        buildSettingsMock.projectWarExplodedDir = STAGING_DIR
        grailsProjectCleaner = new GrailsProjectCleaner(buildSettingsMock, listenerMock)
        grailsProjectCleaner.ant = grailsAntMock

        when:
        grailsProjectCleaner.cleanExplodedWar()

        then:
        1 * grailsAntMock.delete({ it.dir == STAGING_DIR })
    }
}
