package grails.test.mixin.domain

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Specification

class DomainClassUnitTestMixinTests extends Specification implements DataTest {

    void testBackReferenceAssignment() {
        mockDomains Writer, Publication

        when:
        def publication = new Publication(title: 'Some Paper')
        def writer = new Writer(name: 'Some Writer')

        writer.addToPublications(publication)

        then:
        publication.ghostWriter == null
        writer.is(publication.writer)
    }

    void testWithTransaction() {
        mockDomain Writer
        def bodyInvoked = false

        when:
        def w = new Writer(name: "Stephen King")
        w.save(flush:true)

        Writer.withTransaction {
            bodyInvoked = true
        }

        then:
        bodyInvoked
    }
}

@Entity
class Writer {
    String name
    static hasMany = [publications: Publication]
}

@Entity
class Publication {
    String title
    static belongsTo = [writer: Writer]
    Writer ghostWriter
}
