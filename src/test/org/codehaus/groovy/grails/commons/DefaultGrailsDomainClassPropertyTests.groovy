package org.codehaus.groovy.grails.commons;

import junit.framework.TestCase;

import java.beans.IntrospectionException;

public class DefaultGrailsDomainClassPropertyTests extends GroovyTestCase {

    GrailsDomainClass parentClass = new DefaultGrailsDomainClass(ParentClass.class);
    GrailsDomainClass childClass = new DefaultGrailsDomainClass(ChildClass.class);

    GrailsDomainClassProperty prop1Parent = parentClass.getPropertyByName("prop1");
    GrailsDomainClassProperty prop1Child = childClass.getPropertyByName("prop1");
    GrailsDomainClassProperty prop2Child = childClass.getPropertyByName("prop2");

    void testSamePropEquals() throws IntrospectionException {
        assertTrue(prop1Child.equals(prop1Child));
    }

    void testSameInParentEqualsPropInChikd() throws IntrospectionException {
        assertTrue(prop1Parent.equals(prop1Child));
        assertTrue(prop1Child.equals(prop1Parent));
    }

    void testDifferentPropNotEquals() throws IntrospectionException {
        assertFalse(prop1Child.equals(prop2Child));
        assertFalse(prop2Child.equals(prop1Child));
    }

    void testDifferentPropInParentNotEqualChild() throws IntrospectionException {
        assertFalse(prop2Child.equals(prop1Parent));
        assertFalse(prop1Parent.equals(prop2Child));
    }

    void testNullAndNonPropsNotEqual() throws IntrospectionException {
        assertFalse(prop1Child.equals(null));
        assertFalse(prop1Child.equals("Not a property"));

    }
}

class ParentClass {
    Integer id;
    String prop1;
    Integer prop2;
    Integer version;
    ChildClass prop3;

    public ParentClass() {
    }

    public ParentClass(Integer id, String prop1, Integer prop2, ChildClass prop3, Integer version) {
        this.id = id;
        this.prop1 = prop1;
        this.prop2 = prop2;
        this.prop3 = prop3;
        this.version = version;
    }


}


class ChildClass extends ParentClass {

}
