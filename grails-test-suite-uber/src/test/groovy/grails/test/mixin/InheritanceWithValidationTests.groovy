package grails.test.mixin

import grails.persistence.Entity

import org.junit.Test

/**
 * Test for GRAILS-9010
 */
@Mock([AbstractCustomPropertyValue,CustomProperty,StringPropertyValue])
class InheritanceWithValidationTests {

    @Test
    void testNewStringValue () {
        CustomProperty property = new CustomProperty ()

        AbstractCustomPropertyValue propertyValue = property.newValue ("testValue")
        assertValid (propertyValue)
    }

    private void assertValid (def propertyValue) {
        assert propertyValue.valid

        def result = propertyValue.validate () // fails here with: java.lang.IllegalArgumentException:
        // Argument [org.example.StringPropertyValue : null] is not an instance of
        // [class org.example.AbstractCustomPropertyValue] which this validator is configured for
        assert result == true
    }
}

@Entity
class AbstractCustomPropertyValue {

    boolean valid = false

    static constraints = {
        valid (validator: validator)
    }

    static transients = ['valid']

    protected static validator = { value, instance ->
        if (!instance.valid) {
            return 'invalid.value.for.type'
        }

        return null // returning null means valid
    }
}

@Entity
class CustomProperty {
    AbstractCustomPropertyValue newValue (String value) {
        return new StringPropertyValue (value)
    }
}

@Entity
class StringPropertyValue extends AbstractCustomPropertyValue {

    String stringValue

    static constraints = {
        stringValue (nullable: true)
    }

    StringPropertyValue (String value) {
        this.stringValue = value
        this.valid = true
    }
}
