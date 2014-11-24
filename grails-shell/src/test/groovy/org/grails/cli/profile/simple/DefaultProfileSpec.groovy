package org.grails.cli.profile.simple

import org.grails.cli.profile.DefaultProfile
import org.grails.cli.profile.git.GitProfileRepository

import spock.lang.Specification

class DefaultProfileSpec extends Specification {
    DefaultProfile profile
    
    def setup() {
        GitProfileRepository profileRepository = new GitProfileRepository(initialized:true, profilesDirectory: new File('src/test/resources/profiles-repository'))
        profile = DefaultProfile.create(profileRepository, 'web', new File('src/test/resources/profiles-repository/profiles/web'))
    }
    
    def "should contain 5 commands"() {
        when:
        def commands = profile.getCommandLineHandlers()*.listCommands().flatten()
        then:
        commands.size() == 6
        commands*.name as Set == ['test-groovy','create-controller', 'create-domain', 'create-service', 'create-taglib', 'run-app'] as Set
    }
}
