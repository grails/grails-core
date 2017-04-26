package org.grails.web.binding.hal.json

import grails.persistence.Entity
import grails.test.mixin.TestFor
import grails.web.Controller
import org.grails.core.support.MappingContextBuilder
import spock.lang.Specification

@TestFor(BindingController)
class HalJsonBindingSpec extends Specification {

    void setupSpec() {
        new MappingContextBuilder(grailsApplication).build(Person, Address)
    }

    void 'Test binding JSON body'() {
        when:
        request.method = 'POST'
        request.json = '''
            {
    "name": "Douglas",
    "age": "42",
    "_embedded" : {
        "homeAddress" : { "state": "Missouri", "city": "O'Fallon"},
        "workAddress" : { "state": "California", "city": "San Mateo"}
    }
}
'''
        request.contentType = HAL_JSON_CONTENT_TYPE
        def model = controller.createPerson()
    then:
        model.person instanceof Person
        model.person.name == 'Douglas'
        model.person.age == 42
        model.person.homeAddress.city == "O'Fallon"
        model.person.homeAddress.state == 'Missouri'
        model.person.workAddress.city == 'San Mateo'
        model.person.workAddress.state == 'California'
    }
}

@Controller
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
    Address homeAddress
    Address workAddress
}

@Entity
class Address {
    String city
    String state
}
