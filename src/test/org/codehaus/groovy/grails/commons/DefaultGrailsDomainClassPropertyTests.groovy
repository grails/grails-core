package org.codehaus.groovy.grails.commons;

import groovy.util.GroovyTestCase;


public class DefaultGrailsDomainClassPropertyTests extends GroovyTestCase {
    GrailsDomainClass parentClass
    GrailsDomainClass childClass

    GrailsDomainClassProperty prop1Parent
    GrailsDomainClassProperty prop1Child
    GrailsDomainClassProperty prop2Child

    void setUp() throws Exception {
    	GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();
        parentClass = new DefaultGrailsDomainClass(ParentClass.class);
        childClass = new DefaultGrailsDomainClass(ChildClass.class);

        prop1Parent = parentClass.getPropertyByName("prop1");
        prop1Child = childClass.getPropertyByName("prop1");
        prop2Child = childClass.getPropertyByName("prop2");
    }

    void testSamePropEquals() {
        assertTrue(prop1Child.equals(prop1Child));
    }

    void testSameInParentEqualsPropInChikd() {
        assertTrue(prop1Parent.equals(prop1Child));
        assertTrue(prop1Child.equals(prop1Parent));
    }

    void testDifferentPropNotEquals() {
        assertFalse(prop1Child.equals(prop2Child));
        assertFalse(prop2Child.equals(prop1Child));
    }

    void testDifferentPropInParentNotEqualChild() {
        assertFalse(prop2Child.equals(prop1Parent));
        assertFalse(prop1Parent.equals(prop2Child));
    }

    void testNullAndNonPropsNotEqual() {
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
