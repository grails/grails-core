package org.codehaus.groovy.grails.commons.metaclass

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class LazyMetaPropertyMapSpec extends Specification {

	def setup() {
	}

	def cleanup() {
	}

	void "test putAll adds all key and value pairs provided to this map"() {
        given:
            def targetMap = new LazyMetaPropertyMap(new A())
        when:
            targetMap.putAll([someProperty:"a value"])
        then:
            targetMap == [someProperty:"a value"]
	}
}
class A {
    def someProperty = "value"
}
