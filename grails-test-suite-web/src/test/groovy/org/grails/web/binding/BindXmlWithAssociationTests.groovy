package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test

class BindXmlWithAssociationTests
        implements ControllerUnitTest<PersonController>, DataTest {

    Class[] getDomainClassesToMock() {
        [TargetPerson, Book]
    }

    @Test
    void testBindXmlWithAssociatedId() {
        request.method = 'POST'
        request.xml = '''
<person><name>xyz</name><book id='1'></book></person>
'''

        controller.save()

        assert response.text == 'saved'
    }

    @Test
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
