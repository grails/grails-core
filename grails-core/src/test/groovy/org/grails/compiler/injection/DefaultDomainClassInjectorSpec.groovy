package org.grails.compiler.injection

import grails.persistence.Entity
import groovy.transform.ToString

/**
 * @author James Kleeh
 */
class DefaultDomainClassInjectorSpec extends GroovyTestCase {

    void "test default toString"() {
        Test test = new Test()
        test.id = 1
        assert test.toString().endsWith("Test : 1")
    }

    void "test domain with groovy.transform.ToString"() {
        TestWithGroovy test = new TestWithGroovy()
        test.id = 1
        assert test.toString().endsWith("TestWithGroovy(1)")
    }


    @Entity
    class Test {
    }

    @Entity
    @ToString(includes = ["id"])
    class TestWithGroovy {
    }



}
