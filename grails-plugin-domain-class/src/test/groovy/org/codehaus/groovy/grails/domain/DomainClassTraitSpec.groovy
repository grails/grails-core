package org.codehaus.groovy.grails.domain

import grails.core.DefaultGrailsApplication
import grails.persistence.Entity
import grails.util.Holders
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author James Kleeh
 */
class DomainClassTraitSpec extends Specification {

    @Issue("GRAILS-9245")
    void "test getConstrainedProperties"() {
        given:
        def application = new DefaultGrailsApplication([Person] as Class[], getClass().classLoader)
        application.initialise()
        Holders.grailsApplication = application

        expect:
        !Person.constrainedProperties.name.blank
    }

    @Entity
    class Person {
        String name

        static constraints = {
            name blank: false
        }
    }
}

