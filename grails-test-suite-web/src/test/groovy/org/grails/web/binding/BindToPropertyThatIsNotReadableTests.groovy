package org.grails.web.binding

import grails.gorm.annotation.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindToPropertyThatIsNotReadableTests extends Specification implements DomainUnitTest<PropertyNotReadableBook> {

    @Issue("https://github.com/grails/grails-core/issues/11188")
    @Ignore
    void testBindToPropertyThatIsNotReadable() {
        when:
        def b = new PropertyNotReadableBook()

        b.properties = [calculatedField:[1,2,3], title:"blah"]

        then:
        6 == b.sum()
    }
}

@Entity
class PropertyNotReadableBook {

    String title

    private List calculateField

    static transients = ['calculatedField']

    void setCalculatedField(List value) {
        this.calculateField = value
    }

    int sum() { calculateField.sum() }
}
