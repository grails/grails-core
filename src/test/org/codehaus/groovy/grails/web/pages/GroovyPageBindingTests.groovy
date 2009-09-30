package org.codehaus.groovy.grails.web.pages
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class GroovyPageBindingTests extends GroovyTestCase{

    void testGroovyPageBinding() {
        def binding = new GroovyPageBinding()

        binding.foo = "bar"
        assertEquals "bar", binding.foo
        assertEquals( [foo:'bar'], binding.variables )
        assertEquals binding.getMetaClass(), binding.metaClass
    }

}