package grails.test.mixin

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

/**
 * Tests the behavior of creating data in a setup method
 */
class DomainClassSetupMethodTests extends Specification implements DomainUnitTest<Book> {

    void setup() {
        new Book(title:"The Stand", pages:100).save()
    }


    void testSaveInSetup() {
        expect:
        Book.count() == 1
    }
}
