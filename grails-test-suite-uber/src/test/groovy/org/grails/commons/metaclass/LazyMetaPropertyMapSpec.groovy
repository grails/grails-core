package org.grails.commons.metaclass

import grails.beans.util.LazyMetaPropertyMap
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
class LazyMetaPropertyMapSpec extends Specification {

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
