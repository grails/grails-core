package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.orm.hibernate.GormSpec
import grails.persistence.Entity

/**
 * Tests binding from one domain to the next
 */
class DataBindingFromDomainToDomainSpec extends GormSpec{

    void "Test data binding from source entity properties"() {
        given:"An existing instance"
            def source = new Person(firstName: "Homer", lastName: "Simpson", phoneNumber: "3083908043").save(flush:true)

        when:"The source properties are accessed"
            def sourceProps = source.properties

        then:"Only bindable properties are contained within the map"
            sourceProps.size() == 3
            sourceProps.containsKey "firstName"
            sourceProps.containsKey "lastName"
            sourceProps.containsKey "phoneNumber"

        when:"Binding to a target"
            def target = new Person()
            target.properties = source.properties

        then:"The binding works correctly"
            target.firstName == 'Homer'
            target.lastName == "Simpson"
            target.phoneNumber == "3083908043"
    }

    @Override
    List getDomainClasses() {
        [Person]
    }
}

@Entity
class Person {
    String firstName
    String lastName
    String phoneNumber
}
