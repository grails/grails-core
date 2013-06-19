package org.codehaus.groovy.grails.web.binding.xml

import grails.persistence.Entity
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(BindingController)
class XmlBindingSpec extends Specification {
    
    void 'Test binding XML body'() {
        when:
        request.xml = '''
<person>
    <name>Douglas</name>
    <age>42</age>
    <homeAddress>
        <state>Missouri</state>
        <city>O'Fallon</city>
    </homeAddress>
    <workAddress>
        <state>California</state>
        <city>San Mateo</city>
    </workAddress>
</person>
'''
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
