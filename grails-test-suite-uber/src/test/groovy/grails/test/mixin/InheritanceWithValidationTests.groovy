package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.PendingFeature
import spock.lang.Specification

/**
 * Test for GRAILS-9010
 */
class InheritanceWithValidationTests extends Specification implements DataTest {

    void setupSpec() {
        mockDomains(CustomProperty, StringPropertyValue)
    }

    void testNewStringValue () {
        when:
        CustomProperty property = new CustomProperty()

        AbstractCustomPropertyValue propertyValue = property.newValue('testValue')

        then:
        propertyValue.valid
        propertyValue.validate()
    }
}

class AbstractCustomPropertyValue {
// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)

    boolean valid = false

    static constraints = {
        valid (validator: AbstractCustomPropertyValue.validator)
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
    AbstractCustomPropertyValue newValue(String value) {
        return new StringPropertyValue(value)
    }
}

//@Entity
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
