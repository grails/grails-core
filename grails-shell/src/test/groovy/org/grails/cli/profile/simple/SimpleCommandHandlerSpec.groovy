package org.grails.cli.profile.simple

import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.DefaultProfile;
import org.grails.cli.profile.git.GitProfileRepository

import spock.lang.Specification

class SimpleCommandHandlerSpec extends Specification {
    Iterable<CommandLineHandler> commandHandlers
    
    def setup() {
        GitProfileRepository profileRepository = new GitProfileRepository(initialized:true, profilesDirectory: new File('src/test/resources/profiles-repository'))
        def profile = DefaultProfile.create(profileRepository, 'web', new File('src/test/resources/profiles-repository/profiles/web'))
        commandHandlers = profile.getCommandLineHandlers(null)
    }
    
    def "should have commands"() {
        expect:
        commandHandlers*.listCommands(null).flatten().size() == 5
    }
    
    def "commands should have descriptions"() {
        expect:
        commandHandlers*.listCommands(null).flatten().every { CommandDescription description ->
            description.description
        }
    }
    
    def "commands should have usage instructions"() {
        expect:
        commandHandlers*.listCommands(null).flatten().every { CommandDescription description ->
            description.usage
        }
    }
    
    

}
