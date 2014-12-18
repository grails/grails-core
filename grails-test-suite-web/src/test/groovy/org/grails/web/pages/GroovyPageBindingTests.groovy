package org.grails.web.pages

import org.grails.gsp.GroovyPageBinding

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class GroovyPageBindingTests extends GroovyTestCase {

    void testGroovyPageBinding() {
        def binding = new GroovyPageBinding()

        binding.foo = "bar"
        assertEquals "bar", binding.foo
        assertEquals([foo:'bar'], binding.variables)
        assertEquals binding.getMetaClass(), binding.metaClass
    }

    void testVariables() {
        def parentBinding = new GroovyPageBinding()
        parentBinding.a = 1
        parentBinding.b = 2
        def binding = new GroovyPageBinding(parentBinding)
        binding.c = 3
        binding.d = 4
        def shouldbe=[a:1,b:2,c:3,d:4]
        assertEquals(shouldbe, binding.getVariables())
        def copied=[:]
        for (e in binding.getVariables().entrySet()) {
            copied.put(e.key, e.value)
        }
        assertEquals(shouldbe, copied)
    }
}
