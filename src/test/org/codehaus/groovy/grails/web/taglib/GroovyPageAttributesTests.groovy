package org.codehaus.groovy.grails.web.taglib

public class GroovyPageAttributesTests extends GroovyTestCase {

    void testMutatingImpactsWrappedMap() {
        def originalMap = [framework: 'Grails', company: 'SpringSource']
        def wrapper = new GroovyPageAttributes(originalMap)

        // remove an entry from the wrapper
        wrapper.remove('framework')
        assertEquals 1, originalMap.size()
        assertNull originalMap.framework
        assertEquals 'SpringSource', originalMap.company

        // add an entry to the wrapper
        wrapper.lang = 'Groovy'
        assertEquals 2, originalMap.size()
        assertNull originalMap.framework
        assertEquals 'SpringSource', originalMap.company
        assertEquals 'Groovy', originalMap.lang

        // add several entries (via putAll) to the wrapper
        def newMap = [ide: 'STS', target: 'JVM']
        wrapper.putAll(newMap)
        assertEquals 4, originalMap.size()
        assertNull originalMap.framework
        assertEquals 'SpringSource', originalMap.company
        assertEquals 'Groovy', originalMap.lang
        assertEquals 'STS', originalMap.ide        
        assertEquals 'JVM', originalMap.target
    }

}