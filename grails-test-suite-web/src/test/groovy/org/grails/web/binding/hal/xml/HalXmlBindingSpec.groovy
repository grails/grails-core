package org.grails.web.binding.hal.xml

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.Controller
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class HalXmlBindingSpec extends Specification implements ControllerUnitTest<BindingController>, DataTest {

    Class<?>[] getDomainClassesToMock() {
        [Person, Address]
    }

    void 'Test binding XML body'() {
        when:
        request.method = 'POST'
        request.xml = '''<?xml version="1.0" encoding="UTF-8"?>
<resource href="http://localhost/people/1" hreflang="en">
    <name>Douglas</name>
    <age>42</age>
    <resource rel="homeAddress">
        <state>Missouri</state>
        <city>O'Fallon</city>
    </resource>
    <resource rel="workAddress">
        <state>California</state>
        <city>San Mateo</city>
    </resource>
</resource>'''

        request.contentType = HAL_XML_CONTENT_TYPE
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
