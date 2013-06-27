package org.codehaus.groovy.grails.web.binding.json

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Ignore
import spock.lang.Specification

@TestFor(BindingController)
class JsonBindingSpec extends Specification {

    void 'Test binding JSON body'() {
        given:
        request.json = '''
            {
    "name": "Douglas", "age": "42"}
'''
    when:
        def model = controller.createPerson()
    then:
        model.person instanceof Person
        model.person.name == 'Douglas'
        model.person.age == 42
    }
    
    @Ignore
    void 'Test binding nested collection elements'() {
        given:
        request.json = '''
            {
    "lastName": "Brown", 
    "familyMembers": [
        {"name": "Jake", "age": "12"}, 
        {"name": "Zack", "age": "15"}
    ]
}
'''
    when:
        def model = controller.createFamily()
    then:
        model.family.lastName == 'Brown'
        
        model.family.familyMembers.size() == 2
        
        model.family.familyMembers[0].name == 'Jake'
        model.family.familyMembers[0].age == 12

        model.family.familyMembers[1].name == 'Zack'
        model.family.familyMembers[1].age == 15
    }
}

@Artefact('Controller')
class BindingController {
    def createPerson(Person person) {
        [person: person]
    }
    
    def createFamily(Family family) {
        [family: family]
    }
}

class Person {
    String name
    Integer age
}

class Family {
    String lastName
    List<Person> familyMembers
}
