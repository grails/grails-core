package org.codehaus.groovy.grails.commons;

import org.codehaus.groovy.grails.commons.metaclass.*

import org.springframework.beans.BeanUtils

/**
 * Tests for the GrailsMetaClassUtils class

 * @author Graeme Rocher
 **/
public class GrailsMetaClassUtilsTests extends GroovyTestCase {


    void testGetMetaRegistry() {
        assertNotNull(GrailsMetaClassUtils.getRegistry())
    }

    void testCopyExpandoMetaClass() {
        def metaClass = new ExpandoMetaClass(Dummy.class, true)

        // add property
        metaClass.getFoo = {-> "bar" }
        // add instance method
        metaClass.foo = { String txt -> "bar:$txt" }
        // add static method
        metaClass.'static'.bar = {-> "foo" }
        // add constructor
        metaClass.constructor = { String txt ->
            def obj = BeanUtils.instantiateClass(Dummy.class)
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

       GrailsMetaClassUtils.copyExpandoMetaClass(Dummy.class, Dummy2.class, false)

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