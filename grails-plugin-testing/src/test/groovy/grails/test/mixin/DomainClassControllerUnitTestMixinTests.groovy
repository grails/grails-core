package grails.test.mixin

import grails.converters.XML
import grails.persistence.Entity
import org.junit.Test

/**
 * A Junit 4 test that tests a scaffolded controllers logic using the new mixins
 */
@TestFor(BookController)
@Mock([Book, Author])
class DomainClassControllerUnitTestMixinTests {

    @Test
    void testIndex() {
        controller.index()

        assert "/book/list" == response.redirectedUrl
    }

    @Test
    void testConvertToXml() {
        controller.renderXml()

        assert response.xml.title.text() == "The Stand"
    }

    @Test
    void testBinding() {
        def book = new Book(title:"The Stand", pages:"200")

        assert book.pages == 200

        book.properties = [pages:"300"]

        assert book.pages == 300
    }

    @Test
    void testList() {
        def model = controller.list()

        assert model.bookInstanceList.size() == 0
        assert model.bookInstanceTotal == 0

        def book = new Book(title:"")

        assert book.validate() == false

        book.clearErrors()

        book.title = "The Stand"
        book.pages = 1000
        assert book.save() != null

        model = controller.list()

        assert model.bookInstanceList.size() == 1
        assert model.bookInstanceTotal == 1
    }

    @Test
    void testCreate() {
       params.title = "The Stand"
       params.pages = "500"

       def model = controller.create()

       assert model.bookInstance?.title == "The Stand"
       assert model.bookInstance?.pages == 500
    }

    @Test
    void testSave() {
        controller.save()

        assert model.bookInstance != null
        assert view == '/book/create'

        params.title = "The Stand"
        params.pages = "500"

        controller.save()

        assert response.redirectedUrl == '/book/show/1'
        assert flash.message != null
        assert Book.count() == 1
    }

    @Test
    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/book/list'

        def book = new Book(title:"")

        book.title = "The Stand"
        book.pages = 1000
        assert book.save() != null

        params.id = book.id

        def model = controller.show()

        assert model.bookInstance == book
    }

    @Test
    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/book/list'


        def book = new Book(title:"")

        book.title = "The Stand"
        book.pages = 1000
        assert book.save() != null

        params.id = book.id

        def model = controller.edit()

        assert model.bookInstance == book
    }

    @Test
    void testUpdate() {
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/book/list'

        response.reset()

        def book = new Book()
        book.title = "The Stand"
        book.pages = 1000
        assert book.save() != null

        // test invalid parameters in update
        params.id = book.id
        params.title = ""

        controller.update()

        assert view == "/book/edit"
        assert model.bookInstance != null

        book.clearErrors()
        params.title = "The Shining"
        params.pages = "500"

        controller.update()

        assert response.redirectedUrl == "/book/show/$book.id"
        assert flash.message != null
        assert Book.get(book.id).title == "The Shining"
        assert Book.get(book.id).pages == 500
    }

    @Test
    void testDelete() {
        controller.delete()

        assert flash.message != null
        assert response.redirectedUrl == '/book/list'

        response.reset()

        def book = new Book()
        book.title = "The Stand"
        book.pages = 1000
        assert book.save() != null
        assert Book.count() == 1

        params.id = book.id

        controller.delete()

        assert Book.count() == 0
        assert Book.get(book.id) == null
        assert response.redirectedUrl == '/book/list'
    }

    @Test
    void testCriteriaQuery() {
        mockDomain(Book, [[title:"The Stand", pages:"1000"], [title:"The Shining", pages:400], [title:"Along Came a Spider", pages:300]])

        assert Book.count() == 3

        def results = Book.withCriteria {
            like('title', 'The S%')
        }

        assert results.size() == 2
    }
}

@Entity
class Book {
    String title
    Date releaseDate = new Date()
    int pages
    static constraints = {
        title blank:false, nullable:false
    }
    static mapping = {
        title index:true
    }
}

@Entity
class Author {
    String name
}

class BookController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def renderXml = {
        render new Book(title:"The Stand") as XML
    }
    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [bookInstanceList: Book.list(params), bookInstanceTotal: Book.count()]
    }

    def create = {
        def bookInstance = new Book()
        bookInstance.properties = params
        return [bookInstance: bookInstance]
    }

    def save = {
        def bookInstance = new Book(params)
        if (bookInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'book.label', default: 'Book'), bookInstance.id])}"
            redirect(action: "show", id: bookInstance.id)
        }
        else {
            render(view: "create", model: [bookInstance: bookInstance])
        }
    }

    def show = {
        def bookInstance = Book.get(params.id)
        if (!bookInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
            redirect(action: "list")
        }
        else {
            [bookInstance: bookInstance]
        }
    }

    def edit = {
        def bookInstance = Book.get(params.id)
        if (!bookInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [bookInstance: bookInstance]
        }
    }

    def update = {
        def bookInstance = Book.get(params.id)
        if (bookInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (bookInstance.version > version) {

                    bookInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'book.label', default: 'Book')] as Object[], "Another user has updated this Book while you were editing")
                    render(view: "edit", model: [bookInstance: bookInstance])
                    return
                }
            }
            bookInstance.properties = params
            if (!bookInstance.hasErrors() && bookInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'book.label', default: 'Book'), bookInstance.id])}"
                redirect(action: "show", id: bookInstance.id)
            }
            else {
                render(view: "edit", model: [bookInstance: bookInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def bookInstance = Book.get(params.id)
        if (bookInstance) {
            try {
                bookInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'book.label', default: 'Book'), params.id])}"
            redirect(action: "list")
        }
    }
}
