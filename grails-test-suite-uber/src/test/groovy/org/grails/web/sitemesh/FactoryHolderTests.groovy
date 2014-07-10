package org.grails.web.sitemesh

import com.opensymphony.module.sitemesh.DecoratorMapper
import com.opensymphony.module.sitemesh.Factory
import com.opensymphony.module.sitemesh.PageParser

class FactoryHolderTests extends GroovyTestCase {

    void testGetFactoryReturnsFactoryForNonNullFactory() {
        def factory = new DummyFactory()
        FactoryHolder.setFactory(factory)
        assertSame factory, FactoryHolder.getFactory()
    }

    void testGetFactoryForNullFactory() {
        FactoryHolder.setFactory(null)
        assertNull FactoryHolder.getFactory()
    }

    void testSetFactory() {
        def factory = new DummyFactory()
        FactoryHolder.setFactory(factory)
        assertSame factory, FactoryHolder.factory
    }

    // Silly test, but a necessary evil in order to get Cobertura to give us 100% coverage
    void testPrivateConstructor() {
        assertNotNull new FactoryHolder()
    }
}

/** A bare minimum implementation needed to test the factory above. */
class DummyFactory extends Factory {
    boolean isPathExcluded(String path) { false }
    boolean shouldParsePage(String contentType) { false }
    DecoratorMapper getDecoratorMapper() { null }
    void refresh() {}
    PageParser getPageParser(String contentType) { null }
}
