package org.grails.compiler.injection

import grails.persistence.Entity
import groovy.transform.Generated
import groovy.transform.ToString
import spock.lang.Specification

/**
 * @author James Kleeh
 */
class DefaultDomainClassInjectorSpec extends Specification {

    void "test default toString"() {
        when:
        Test test = new Test()
        test.id = 1

        then:
        test.toString().endsWith("Test : 1")

        and: 'toString is marked as Generated'
        test.class.getMethod('toString').isAnnotationPresent(Generated)
    }

    void "test domain with groovy.transform.ToString"() {
        when:
        TestWithGroovy test = new TestWithGroovy()
        test.id = 1

        then:
        test.toString().endsWith("TestWithGroovy(1)")

        and: 'toString is marked as Generated'
        test.class.getMethod('toString').isAnnotationPresent(Generated)
    }

    @Entity
    class Test {
    }

    @Entity
    @ToString(includes = ["id"])
    class TestWithGroovy {
    }
}
