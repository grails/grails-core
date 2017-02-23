package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

@TestFor(PersonController)
@Mock([TargetPerson, Book])
class BindXmlWithAssociationTests {

    void testBindXmlWithAssociatedId() {
        Book b = new Book(title: "The Stand", pages: 1000).save(flush:true)
        request.method = 'POST'
        request.xml = """
<person><name>xyz</name><book id='${b.id}'></book></person>
""".toString()

        controller.save()

        assert response.text == 'saved'
    }

    void testBindXmlWithAssociatedIdAndProperties() {
        request.method = 'POST'
        request.xml = '''
<person><name>xyz</name><book id='1'><title>Blah</title><pages>300</pages></book></person>
'''

        controller.save()

        assert response.text == 'saved'

        TargetPerson person = request.person

        assert person != null
        assert person.name == 'xyz'
        assert person.book != null
        assert person.book.id == 1
        assert person.book.title == 'Blah'
        assert person.book.pages == 300
    }
}

@Artefact('Controller')
class PersonController {

    def save = {
        def person = new TargetPerson()
        person.properties = request

        // uncomment next line to avoid error
        //person.merge()
        person.save(failOnError:true)

        request.person = person
        render 'saved'
    }
}

@Entity
class TargetPerson {
    String name
    Book book
}

@Entity
class Book {

    String title
    int pages

    static constraints = {
        id bindable: true
    }
}
