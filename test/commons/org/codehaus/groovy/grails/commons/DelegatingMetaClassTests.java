package org.codehaus.groovy.grails.commons;

import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethods;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicProperty;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

import java.util.regex.Pattern;

public class DelegatingMetaClassTests extends TestCase {


    private GroovyObject groovyObject;

    /* (non-Javadoc)
      * @see junit.framework.TestCase#setUp()
      */
    protected void setUp() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();

        Class groovyClass = gcl.parseClass( "class TestClass {\n" +
                        "def testMethod() {\n" +
                        "}\n" +
                        "}" );

        DynamicMethods methods = new AbstractDynamicMethods(groovyClass) {};
        methods.addDynamicMethodInvocation(new AbstractDynamicMethodInvocation(Pattern.compile("^testDynamic$")) {
            public Object invoke(Object target, Object[] arguments) {
                return "success";
            }

        });
        methods.addDynamicProperty(new AbstractDynamicProperty("testProp") {

            private Object internal;
            public Object get(Object object) {
                return internal;
            }

            public void set(Object object, Object newValue) {
                internal = newValue;
            }

        });
        this.groovyObject = (GroovyObject)groovyClass.newInstance();
    }

    public void testInvokeExistingJavaMethod() {
        assertNotNull(groovyObject.invokeMethod("toString", new Object[0]));
    }

    public void testInvokeExistingGroovyMethod() {
        assertTrue(((Boolean)groovyObject.invokeMethod("is", new Object[]{groovyObject})).booleanValue());
    }
    /*
      * Test method for 'org.codehaus.groovy.grails.commons.metaclass.DelegatingMetaClass.invokeMethod(Object, String, Object[])'
      */
    public void testInvokeMethodObjectStringObjectArray() {
        assertEquals("success",groovyObject.invokeMethod("testDynamic", new Object[0]));
    }

    /*
      * Test method for 'org.codehaus.groovy.grails.commons.metaclass.DelegatingMetaClass.invokeStaticMethod(Object, String, Object[])'
      */
    public void testInvokeStaticMethodObjectStringObjectArray() {
        //TODO
    }


    public void testPropertyAccess() {
        assertNull(groovyObject.getProperty("testProp"));
        groovyObject.setProperty("testProp", "success");
        assertEquals("success",groovyObject.getProperty("testProp"));
    }

}
