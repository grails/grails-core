package org.codehaus.groovy.grails.validation

import spock.lang.Specification
import grails.persistence.Entity
import grails.test.mixin.TestFor
import grails.test.mixin.Mock

/**
 */
@TestFor(CascadingPerson)
@Mock(Name)
class CascadingErrorCountSpec extends Specification {

    void "Test that the error count is correct when validating sorted set"() {
        when:"A domain is created with an invalid collection and then validated"
            def person = new CascadingPerson(placeholder:"test")
            person.addToNames(new Name(name:null))
            person.validate()

        then:"The error count is correct"
            person.hasErrors() == true
            person.errors.allErrors.size() == 1


    }
}

@Entity
class CascadingPerson {
    String placeholder

    static hasMany = [ names: Name ]

    SortedSet<Name> names

    static constraints = {
    }
}

@Entity
class Name {
    String name
    static belongsTo = [ person: CascadingPerson ]

    static constraints = {
        name(blank: false, nullable: false)
    }
}