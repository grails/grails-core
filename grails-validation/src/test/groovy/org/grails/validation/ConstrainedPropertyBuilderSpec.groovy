package org.grails.validation

import grails.validation.ConstrainedProperty
import spock.lang.Issue
import spock.lang.Specification

class ConstrainedPropertyBuilderSpec extends Specification {

    @Issue('GRAILS-12010')
    void 'test methods with names matching constrained property names'() {
        given:
        def builder = new ConstrainedPropertyBuilder(Widget)
        def constraintsClosure = Widget.constraints
        constraintsClosure.delegate = builder
        constraintsClosure()

        when:
        def constrainedProperties = builder.getConstrainedProperties()

        then: 'the last property should be constrained'
        constrainedProperties['last'] instanceof ConstrainedProperty

        and: 'the propertyName of the ConstrainedProperty should be "last"'
        constrainedProperties['last'].propertyName == 'last'

        and: 'the propertyType of the ConstrainedProperty should be that of the property, not the return type of the static method'
        constrainedProperties['last'].propertyType == String

        and: 'the first property should be constrained'
        constrainedProperties['first'] instanceof ConstrainedProperty

        and: 'the propertyName of the ConstrainedProperty should be "first"'
        constrainedProperties['first'].propertyName == 'first'

        and: 'the propertyType of the ConstrainedProperty should be that of the property, not the return type of the method'
        constrainedProperties['first'].propertyType == String
    }
}

class Widget {
    String last
    String first

    static Integer last() {}

    Integer first() {}

    static constraints = {
        last blank: false
        first blank: false
    }
}