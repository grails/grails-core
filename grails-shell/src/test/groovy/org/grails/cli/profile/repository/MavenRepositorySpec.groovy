package org.grails.cli.profile.repository

import spock.lang.Specification



/**
 * @author graemerocher
 */
class MavenRepositorySpec extends Specification {


    void "Test resolve profile"() {
        given:"A maven profile repository"
        def repo = new MavenProfileRepository()

        when:"We resolve the web profile"
        def profile = repo.getProfile("web")

        then:"The profile is not null"
        profile != null
        profile.name == 'web'
    }

    void "Test list all profiles"() {
        given:"A maven profile repository"
        def repo = new MavenProfileRepository()

        when:"We resolve the web profile"
        def profiles = repo.allProfiles

        then:"The profiles are not null or empty"
        profiles
    }
}
