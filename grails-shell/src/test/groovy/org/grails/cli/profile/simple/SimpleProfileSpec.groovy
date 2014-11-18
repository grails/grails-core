package org.grails.cli.profile.simple

import org.grails.cli.profile.git.GitProfileRepository

import spock.lang.Specification

class SimpleProfileSpec extends Specification {
    SimpleProfile profile
    
    def setup() {
        GitProfileRepository profileRepository = new GitProfileRepository(initialized:true, profilesDirectory: new File('src/test/resources/profiles-repository'))
        profile = SimpleProfile.create(profileRepository, 'web', new File('src/test/resources/profiles-repository/profiles/web'))
    }
    
    def "should contain 5 commands"() {
        when:
        def commands = profile.getCommandLineHandlers()*.listCommands().flatten()
        then:
        commands.size() == 5
        commands*.name as Set == ['create-controller', 'create-domain', 'create-service', 'create-taglib', 'run-app'] as Set
    }
}
