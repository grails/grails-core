package org.codehaus.groovy.grails.web.util

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
