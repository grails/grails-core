package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class BindXmlWithAssociationTests extends Specification implements ControllerUnitTest<PersonController>, DataTest {

    Class[] getDomainClassesToMock() {
        [TargetPerson, Book]
    }

    void testBindXmlWithAssociatedId() {
        when:
        Book b = new Book(title: "The Stand", pages: 1000).save(flush:true)
        request.method = 'POST'
        request.xml = """
<person><name>xyz</name><book id='${b.id}'></book></person>
""".toString()

        controller.save()

        then:
        response.text == 'saved'
    }

    void testBindXmlWithAssociatedIdAndProperties() {
        when:
        request.method = 'POST'
        request.xml = '''
<person><name>xyz</name><book id='1'><title>Blah</title><pages>300</pages></book></person>
'''

        controller.save()
        TargetPerson person = request.person

        then:
        response.text == 'saved'
        person != null
        person.name == 'xyz'
        person.book != null
        person.book.id == 1
        person.book.title == 'Blah'
        person.book.pages == 300
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
