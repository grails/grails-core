package grails.test.mixin.domain

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

@TestFor(Writer)
@Mock(Publication)
class DomainClassUnitTestMixinTests {

    void testBackReferenceAssignment() {
        def publication = new Publication(title: 'Some Paper')
        def writer = new Writer(name: 'Some Writer')
        
        writer.addToPublications(publication)
        
        assert publication.ghostWriter == null
        assert writer.is(publication.writer)
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
