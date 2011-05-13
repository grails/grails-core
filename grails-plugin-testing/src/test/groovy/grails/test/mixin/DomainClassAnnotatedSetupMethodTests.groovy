package grails.test.mixin

import org.junit.Before
import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 13/05/2011
 * Time: 14:52
 * To change this template use File | Settings | File Templates.
 */
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
