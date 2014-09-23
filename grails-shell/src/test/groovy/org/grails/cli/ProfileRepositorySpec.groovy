package org.grails.cli;

import spock.lang.Specification

class ProfileRepositorySpec extends Specification {
    ProfileRepository profileRepository
    
    def setup() {
        profileRepository = new ProfileRepository(initialized:true, profilesDirectory: new File('src/test/resources/profiles-repository'))
    }
    
    def "should return profile"() {
        expect:
        profileRepository.getProfile('web') != null
    }
    
}
