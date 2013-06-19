package org.codehaus.groovy.grails.web.binding.json

import grails.persistence.Entity
import grails.test.mixin.TestFor
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
}

class BindingController {
    def createPerson() {
        def person = new Person()
        person.properties = request
        [person: person]
    }
}

@Entity
class Person {
    String name
    Integer age
}
