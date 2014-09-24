package org.grails.web.binding

import grails.persistence.Entity
import grails.test.mixin.Mock
import groovy.transform.NotYetImplemented
import org.junit.Test
import static org.junit.Assert.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Mock(PropertyNotReadableBook)
class BindToPropertyThatIsNotReadableTests {

    @Test
    @NotYetImplemented
    void testBindToPropertyThatIsNotReadable() {
        def b = new PropertyNotReadableBook()

        b.properties = [calculatedField:[1,2,3], title:"blah"]

        assertEquals 6, b.sum()
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
