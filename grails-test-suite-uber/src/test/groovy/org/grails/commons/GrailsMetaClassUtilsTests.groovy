package org.grails.commons

import grails.util.GrailsMetaClassUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanUtils

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

/**
 * Tests for the GrailsMetaClassUtils class.
 *
 * @author Graeme Rocher
 */
class GrailsMetaClassUtilsTests {

    @Test
    void testGetMetaRegistry() {
        assertNotNull(GrailsMetaClassUtils.getRegistry())
    }

    @Test
    void testCopyExpandoMetaClass() {
        def metaClass = new ExpandoMetaClass(Dummy, true)

        // add property
        metaClass.getFoo = {-> "bar" }
        // add instance method
        metaClass.foo = { String txt -> "bar:$txt".toString() }
        // add static method
        metaClass.'static'.bar = {-> "foo" }
        // add constructor
        metaClass.constructor = { String txt ->
            def obj = BeanUtils.instantiateClass(Dummy)
            obj.name = txt
            obj
        }

        metaClass.initialize()

        def d = new Dummy("foo")
        assertEquals "foo", d.name
        assertEquals "bar", d.foo
        assertEquals "bar", d.getFoo()
        assertEquals "bar:1", d.foo("1")
        assertEquals "foo", Dummy.bar()

        GrailsMetaClassUtils.copyExpandoMetaClass(Dummy, Dummy2, false)

        d = new Dummy2("foo")
        assertEquals "foo", d.name
        assertEquals "bar", d.foo
        assertEquals "bar", d.getFoo()
        assertEquals "bar:1", d.foo("1")
        assertEquals "foo", Dummy.bar()
    }
}

class Dummy {
    String name
}

class Dummy2 {
    String name
}
