package org.codehaus.groovy.grails.orm.hibernate

import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class GetAllSpec extends GormSpec {

    @Issue('GRAILS-9813')
    void "Test that the getAll method returns the correct results"() {
        given:"Some sample domains"
            new Person(firstName: "Fred", lastName: "Flintstone").save()
            new Person(firstName: "Homer", lastName: "Simpson").save()
            new Person(firstName: "Bart", lastName: "Simpson").save(flush:true)

        when:"The getAll method is used with varargs"
            def people = Person.getAll(1L, 2L)

        then:"The correct results are returned"
            people != null
            people.size() == 2
            people.any { it.firstName == "Fred" }
            people.any { it.firstName == "Homer" }

        when:"The getAll method is used with a list"
            people = Person.getAll([1L, 2L])

        then:"The correct results are returned"
            people != null
            people.size() == 2
            people.any { it.firstName == "Fred" }
            people.any { it.firstName == "Homer" }
    }

    @Override
    List getDomainClasses() {
        [Person, Pet, Face, Nose]
    }
}
