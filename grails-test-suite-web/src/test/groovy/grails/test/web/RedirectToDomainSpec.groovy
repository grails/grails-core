package grails.test.web

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class RedirectToDomainSpec extends Specification implements ControllerUnitTest<BookController>, DomainUnitTest<Book> {

    void "Test redirect to domain"() {
        given:"A domain instance"
            def b = new Book().save(flush:true)

        when:"A redirect is issued"
            controller.index()

        then:"The correct link is produced"
            response.redirectUrl == '/book/show/1'
    }
}

@Artefact("Controller")
class BookController {
    def index() {
        def b = Book.get(1)
        redirect b
    }
}
@Entity
class Book {

}
