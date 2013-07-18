package org.codehaus.groovy.grails.web.metaclass

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(DemoController)
class CollectionBindDataMethodSpec extends Specification {

    void 'Test bindData with a CollectionDataBindingSource argument using XML'() {
        when:
        request.xml = '''
<people>
    <person>
        <firstName>Peter</firstName>
        <lastName>Gabriel</lastName>
    </person>
    <person>
        <firstName>Tony</firstName>
        <lastName>Banks</lastName>
    </person>
    <person>
        <firstName>Steve</firstName>
        <lastName>Hacket</lastName>
    </person>
</people>
'''
        def model = controller.createPeople()
        def people = model.people
        
        then:
        people instanceof List
        people.size() == 3
        people[0] instanceof Person
        people[0].firstName == 'Peter'
        people[0].lastName == 'Gabriel'
        people[1] instanceof Person
        people[1].firstName == 'Tony'
        people[1].lastName == 'Banks'
        people[2] instanceof Person
        people[2].firstName == 'Steve'
        people[2].lastName == 'Hacket'
    }
    
    void 'Test bindData with a CollectionDataBindingSource argument using JSON'() {
        when:
        request.json = '''
  [{"firstName": "Phil", "lastName" : "Lynott"},
   {"firstName": "Scott", "lastName" : "Gorham"},
   {"firstName": "Brian", "lastName" : "Downey"}]
'''
        def model = controller.createPeople()
        def people = model.people
        
        then:
        people instanceof List
        people.size() == 3
        people[0] instanceof Person
        people[0].firstName == 'Phil'
        people[0].lastName == 'Lynott'
        people[1] instanceof Person
        people[1].firstName == 'Scott'
        people[1].lastName == 'Gorham'
        people[2] instanceof Person
        people[2].firstName == 'Brian'
        people[2].lastName == 'Downey'
    }
}

@Artefact('Controller')
class DemoController {
    def dataBindingSourceRegistry
    def mimeTypeResolver

    def createPeople() {
        // this is a peculiar thing that applications generally wouldn't do but
        // is a way to to test that the resolver is creating a compatible CollectionDataBindingSource
        def mimeType = mimeTypeResolver.resolveRequestMimeType()
        def bindingSource = dataBindingSourceRegistry.createCollectionDataBindingSource mimeType, Person, request
        
        def listOfPeople = []
        
        bindData Person, listOfPeople, bindingSource
        
        [people: listOfPeople]
    }
}

class Person {
    String firstName
    String lastName
}
