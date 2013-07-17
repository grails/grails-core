package org.codehaus.groovy.grails.web.metaclass

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.grails.databinding.CollectionDataBindingSource
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource

import spock.lang.Specification

@TestFor(DemoController)
class CollectionBindDataMethodSpec extends Specification {

    void 'Test bindData with a CollectionDataBindingSource argument'() {
        when:
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
}

@Artefact('Controller')
class DemoController {
    
    def createPeople() {
        def bindingSource = new MockCollectionDataBindingSource()
        bindingSource.listOfMaps = []
        bindingSource.listOfMaps << [firstName: 'Peter', lastName: 'Gabriel']
        bindingSource.listOfMaps << [firstName: 'Tony', lastName: 'Banks']
        bindingSource.listOfMaps << [firstName: 'Steve', lastName: 'Hacket']
        def listOfPeople = []
        
        bindData Person, listOfPeople, bindingSource
        
        [people: listOfPeople]
    }
}

class Person {
    String firstName
    String lastName
}

class MockCollectionDataBindingSource implements CollectionDataBindingSource {
    
    def listOfMaps

    @Override
    public List<DataBindingSource> getDataBindingSources() {
        listOfMaps?.collect { new SimpleMapDataBindingSource(it) }
    }
}
