package org.grails.validation

import grails.validation.ValidationErrors
import org.grails.core.DefaultGrailsDomainClass
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.ObjectError
import spock.lang.Issue
import spock.lang.Specification

/**
 * Created by graemerocher on 08/12/16.
 */
class GrailsDomainClassValidatorSpec extends Specification {

    @Issue('https://github.com/grails/grails-core/issues/10347')
    void "test validator constraint is only invoked once"() {
        when:""
        GrailsDomainClassValidator validator = new GrailsDomainClassValidator()
        validator.setDomainClass( new DefaultGrailsDomainClass(Test))
        validator.setMessageSource(new StaticMessageSource())
        def test = new Test(name: "blah", foo: "two")
        def errors = new ValidationErrors(test)
        validator.validate(test, errors)

        then:
        errors.allErrors.size() == 2
        errors.allErrors.find { ObjectError e -> e.code == 'invalid.name' }
        errors.allErrors.find { ObjectError e -> e.code == 'invalid.foo' }
    }


    static class Test {
        Long id,version
        String name
        String foo
        static transients = ['foo']
        static constraints = {
            name validator: { val, obj, errors ->
                errors.reject("invalid.name", "Invalid Name")
            }
            foo validator: { val, obj, errors ->
                errors.reject("invalid.foo", "Invalid Foo")
            }
        }
    }

}
