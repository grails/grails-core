package org.grails.cli.profile

import org.grails.cli.GrailsCliSpec
import org.grails.cli.profile.git.GitProfileRepository
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class DefaultProfileSpec extends Specification {
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    DefaultProfile profile
    
    def setup() {
        GitProfileRepository profileRepository = new GitProfileRepository()
        GrailsCliSpec.setupProfileRepositoryForTesting(profileRepository, new File(tempFolder.newFolder(), "repository"), new File("").absoluteFile)
        File webProfileDirectory = new File(profileRepository.profilesDirectory, 'profiles/web')
        assert webProfileDirectory.exists()
        profile = DefaultProfile.create(profileRepository, 'web', webProfileDirectory)
    }
    
    def "should contain 5 commands"() {
        when:
        def commands = profile.getCommands([:] as ProjectContext)
        then:
        commands.size() == 17
        commands*.name as Set == ['clean', 'compile', 'console', 'create-controller', 'create-domain-class', 'create-integration-test', 'create-script', 'create-service', 'create-taglib', 'create-unit-test', 'dependency-report', 'gradle', 'package', 'run-app', 'test-groovy', 'war'] as Set
    }
}
