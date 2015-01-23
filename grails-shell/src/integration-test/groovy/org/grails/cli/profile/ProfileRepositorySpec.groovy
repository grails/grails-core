package org.grails.cli.profile

import org.grails.cli.GrailsCliSpec
import org.grails.cli.profile.git.GitProfileRepository
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class ProfileRepositorySpec extends Specification {
    GitProfileRepository profileRepository
    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()
    
    def setup() {
        profileRepository = new GitProfileRepository()
        GrailsCliSpec.setupProfileRepositoryForTesting(profileRepository, new File(tempFolder.newFolder(), "repository"), new File("").absoluteFile)
    }
    
    def "should return profile"() {
        expect:
        profileRepository.getProfile('web') != null
    }
    
    def "should return null if profile doesn't exist"() {
        expect:
        profileRepository.getProfile('unknown') == null
    }
    
}
