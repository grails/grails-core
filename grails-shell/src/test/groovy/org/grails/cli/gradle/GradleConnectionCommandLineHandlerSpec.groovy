package org.grails.cli.gradle

import org.grails.cli.profile.ProjectContext

import spock.lang.Specification

class GradleConnectionCommandLineHandlerSpec extends Specification {

    def "should list gradle commands"() {
        given:
        ProjectContext projectContext = Mock(ProjectContext)
        GradleConnectionCommandLineHandler gradleHandler = new GradleConnectionCommandLineHandler()
        when:
        def commands = gradleHandler.listCommands(projectContext)
        println commands
        then:
        projectContext.getBaseDir() >> new File("src/test/resources/gradle-sample")
        commands.size() > 0
    }

}
