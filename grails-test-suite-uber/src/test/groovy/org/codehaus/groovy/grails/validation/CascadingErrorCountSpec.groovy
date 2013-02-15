package org.codehaus.groovy.grails.validation

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 */
@TestFor(CascadingPerson)
@Mock([CascadingPerson, Name])
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

    SortedSet<Name> names
    static hasMany = [ names: Name ]
}

@Entity
class Name implements Comparable<Name> {
    String name
    static belongsTo = [ person: CascadingPerson ]

    static constraints = {
        name(blank: false, nullable: false)
    }

    int compareTo(Name other) { other.name <=> name }
}
