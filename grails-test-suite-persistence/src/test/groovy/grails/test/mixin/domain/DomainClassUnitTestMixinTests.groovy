package grails.test.mixin.domain

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import org.junit.Test

@TestMixin(DomainClassUnitTestMixin)
class DomainClassUnitTestMixinTests {

    @Test
    void testBackReferenceAssignment() {
        mockDomains Writer, Publication

        def publication = new Publication(title: 'Some Paper')
        def writer = new Writer(name: 'Some Writer')

        writer.addToPublications(publication)

        assert publication.ghostWriter == null
        assert writer.is(publication.writer)
    }

    @Test
    void testWithTransaction() {
        mockDomain Writer
        def bodyInvoked = false

        def w = new Writer(name: "Stephen King")
        w.save(flush:true)

        Writer.withTransaction {
            bodyInvoked = true
        }

        assert bodyInvoked
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
