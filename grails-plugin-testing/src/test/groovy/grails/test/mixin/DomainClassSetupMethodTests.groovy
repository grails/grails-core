package grails.test.mixin

import org.junit.Test

/**
 * Tests the behavior of creating data in a setup method
 */
@TestFor(BookController)
@Mock(Book)
class DomainClassSetupMethodTests {

    void setUp() {
        new Book(title:"The Stand", pages:100).save()
    }

    @Test
    void testSaveInSetup() {
        assert Book.count() == 1
    }
}
