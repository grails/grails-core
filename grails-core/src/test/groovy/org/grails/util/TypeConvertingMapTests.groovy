package org.grails.util

import grails.util.TypeConvertingMap

import groovy.transform.CompileStatic

/**
 * @author Luke Daley
 */
class TypeConvertingMapTests extends GroovyTestCase {

    void testEqualsImpl() {
        assert toTypeConverting([:]) == toTypeConverting([:])
        assert toTypeConverting(a: 1) == toTypeConverting(a: 1)
        assert toTypeConverting(a: 1, b: 2) == toTypeConverting(a: 1, b: 2)
        assert toTypeConverting(a: 1, b: 2) == toTypeConverting(b: 2, a: 1)

        assert toTypeConverting(a: 1, b: 2) != toTypeConverting(a: 1, b: "2")
        assert toTypeConverting(a: 1) != toTypeConverting(a: 1, b: 2)
        assert toTypeConverting(a: 1, b: 2) == toTypeConverting(b: 2, "a": 1)
    }
    
    @CompileStatic
    void testEqualsWithNullsCompileStatic() {
        assert toTypeConverting(a: null, b: 2).equals(toTypeConverting(b: 2, a: null))
        assert !toTypeConverting(a: null, b: 2).equals(toTypeConverting(b: 2, a: 1))
        assert !toTypeConverting(a: 1, b: 2).equals(toTypeConverting(b: 2, a: null))
    }
    
    void testEqualsWithNullsCompileDynamic() {
        assert toTypeConverting(a: null, b: 2).equals(toTypeConverting(b: 2, a: null))
        assert !toTypeConverting(a: null, b: 2).equals(toTypeConverting(b: 2, a: 1))
        assert !toTypeConverting(a: 1, b: 2).equals(toTypeConverting(b: 2, a: null))
    }

    void testHashCode() {
        assert toTypeConverting(a: 1, b: 2).hashCode() == toTypeConverting(a: 1, b: 2).hashCode()
        assert toTypeConverting([:]).hashCode() == toTypeConverting([:]).hashCode()
        assert toTypeConverting(a: 1, b: 2).hashCode() == toTypeConverting(b: 2, a: 1).hashCode()

        assert toTypeConverting(a: 1, b: 2).hashCode() != [b: 2, a: 1].hashCode()
        assert toTypeConverting(a: 1, b: 2).hashCode() != ["b": 2, a: 1].hashCode()
    }

    protected toTypeConverting(map) {
        new TypeConvertingMap(map)
    }
}
