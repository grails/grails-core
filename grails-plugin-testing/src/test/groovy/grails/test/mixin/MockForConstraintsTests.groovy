package grails.test.mixin

import org.junit.Test

 /**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 15/04/2011
 * Time: 15:03
 * To change this template use File | Settings | File Templates.
 */
@TestFor(ConstrainedBook)
class MockForConstraintsTests {

    @Test
    void testConstraints() {

        def existingBook = new ConstrainedBook(title: "Misery", author: "Stephen King")

        mockForConstraintsTests(ConstrainedBook, [ existingBook ])

        // Validation should fail if both properties are null.
        def book = new ConstrainedBook()

        assert !book.validate()
        assert "nullable" == book.errors["title"]
        assert "nullable" == book.errors["author"]



        // So let's demonstrate the unique and minSize constraints.

        book = new ConstrainedBook(title: "Misery", author: "JK")
        assert !book.validate()
        assert "unique" == book.errors["title"]

        assert "minSize" == book.errors["author"]

        // Validation should pass!
        book = new ConstrainedBook(title: "The Shining", author: "Stephen King")
        assert book.validate()
    }
}
class ConstrainedBook {
    Long id
    Long version
    String title
    String author
    static constraints = {
        title(blank: false, unique: true)
        author(blank: false, minSize: 5)
    }
}