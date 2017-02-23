package org.grails.core

import grails.core.DefaultGrailsApplication
import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.core.support.MappingContextBuilder

class DefaultGrailsDomainClassPropertyTests extends GroovyTestCase {
    GrailsDomainClass parentClass
    GrailsDomainClass childClass

    GrailsDomainClassProperty prop1Parent
    GrailsDomainClassProperty prop1Child
    GrailsDomainClassProperty prop2Child

    void setUp() {
        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()
        def ga = new DefaultGrailsApplication(ParentClass, ChildClass)
        ga.initialise()
        new MappingContextBuilder(ga).build(ParentClass, ChildClass)

        parentClass = (GrailsDomainClass)ga.getArtefact(DomainClassArtefactHandler.TYPE, ParentClass.name)
        childClass = (GrailsDomainClass)ga.getArtefact(DomainClassArtefactHandler.TYPE, ChildClass.name)

        prop1Parent = parentClass.getPropertyByName("prop1")
        prop1Child = childClass.getPropertyByName("prop1")
        prop2Child = childClass.getPropertyByName("prop2")
    }

    void testSamePropEquals() {
        assertTrue(prop1Child.equals(prop1Child))
    }

    void testSameInParentEqualsPropInChild() {
        assertTrue(prop1Parent.equals(prop1Child))
        assertTrue(prop1Child.equals(prop1Parent))
    }

    void testDifferentPropNotEquals() {
        assertFalse(prop1Child.equals(prop2Child))
        assertFalse(prop2Child.equals(prop1Child))
    }

    void testDifferentPropInParentNotEqualChild() {
        assertFalse(prop2Child.equals(prop1Parent))
        assertFalse(prop1Parent.equals(prop2Child))
    }

    void testNullAndNonPropsNotEqual() {
        assertFalse(prop1Child.equals(null))
        assertFalse(prop1Child.equals("Not a property"))
    }
}

class ParentClass {
    Integer id
    String prop1
    Integer prop2
    Integer version
    ChildClass prop3

    ParentClass() {}

    ParentClass(Integer id, String prop1, Integer prop2, ChildClass prop3, Integer version) {
        this.id = id
        this.prop1 = prop1
        this.prop2 = prop2
        this.prop3 = prop3
        this.version = version
    }
}

class ChildClass extends ParentClass {}
