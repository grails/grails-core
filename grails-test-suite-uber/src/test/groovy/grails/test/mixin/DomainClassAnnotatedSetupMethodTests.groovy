package grails.test.mixin

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class DomainClassAnnotatedSetupMethodTests extends Specification implements DomainUnitTest<Book> {

    void setup() {
        new Book(title:"The Stand", pages:100).save(flush:true)
    }

    void testSaveInSetup() {
        expect:
        Book.count() == 1
    }
}
