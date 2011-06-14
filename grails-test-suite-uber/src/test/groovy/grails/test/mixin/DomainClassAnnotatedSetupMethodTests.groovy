package grails.test.mixin

import org.junit.Before
import org.junit.Test

@TestFor(BookController)
@Mock(Book)
class DomainClassAnnotatedSetupMethodTests {

    @Before
    void addBooks() {
        assert new Book(title:"The Stand", pages:100).save(flush:true) != null
    }

    @Test
    void testSaveInSetup() {
        //assert Book.count() == 1
    }
}
