package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class PartialMockWithManyToManySpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains BlogUser, Post
    }

    @Issue('GRAILS-10022')
    def "Test that a partially mocked domain containing a many-to-many doesn't produce an error"() {
        given: "A user with posts in the db"
            BlogUser chuck = new BlogUser(loginId: "chuck_norris").save(failOnError: true, flush: true)

        when: "An associated domain that has a many-to-many relationship to another entity is"
            def p = new Post(content: "blah")
            chuck.addToPosts(p)
            chuck.save(flush: true)
            chuck.discard()
            chuck = BlogUser.findByLoginId("chuck_norris")

        then: "The domain model is persisted without issue"
            chuck != null
            chuck.posts.size() == 1

    }
}

@Entity
class BlogUser {

    String loginId

    static hasMany = [ posts : Post, tags : Tag, following : User ]
}

@Entity
class Post {

    String content

    static belongsTo = [ user : BlogUser ]

    static hasMany = [ tags : Tag ]
}
@Entity
class Tag {

    String name
    User user

    static hasMany = [ posts : Post ]
    static belongsTo = [ User, Post ]

}
