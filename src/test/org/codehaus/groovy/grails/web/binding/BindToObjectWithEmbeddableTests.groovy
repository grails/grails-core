package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class BindToObjectWithEmbeddableTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Person {

    static embedded = ['address']

    String name
    int age
    Address address = new Address()

}

class Address {
    String street
    String street2
    String city
    String state
    String zip

    static constraints = {
        street2(nullable:true)
    }
}

class PersonController {

    def save = {
        def p = new Person(params)
        [person:p]
    }
}

''')
    }


    void testBindToObjectWithEmbedded() {
        def controller = ga.getControllerClass("PersonController").newInstance()

        controller.params.name = "Joe"
        controller.params.age= "45"
        controller.params.'address.city' = 'Brighton'

        def model = controller.save()

        assertEquals "Joe", model.person.name
        assertEquals 45, model.person.age
        assertEquals "Brighton", model.person.address.city

    }

}