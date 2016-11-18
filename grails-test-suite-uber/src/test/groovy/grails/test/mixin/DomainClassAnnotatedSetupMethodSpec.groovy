package grails.test.mixin

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Before
import spock.lang.Specification

class DomainClassAnnotatedSetupMethodSpec extends Specification implements ControllerUnitTest<BookController>, DataTest {

    void setupSpec() {
        mockDomain Book
    }

    @Before
    void createBook() {
        assert new Book(title:"The Stand", pages:100).save(flush:true) != null
    }

    void testSaveInSetup() {
        expect:
        Book.count() == 1
    }
}
