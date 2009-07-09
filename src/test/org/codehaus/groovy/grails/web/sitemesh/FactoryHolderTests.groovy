package org.codehaus.groovy.grails.web.sitemesh

import com.opensymphony.module.sitemesh.Factory

class FactoryHolderTests extends GroovyTestCase {

  void testGetFactoryReturnsFactoryForNonNullFactory() {
    def factory = new DummyFactory()
    FactoryHolder.setFactory(factory)   
    assertSame factory, FactoryHolder.getFactory()
  }

  void testGetFactoryThrowsExceptionForNullFactory() {
    FactoryHolder.setFactory(null)       
    shouldFail(IllegalStateException) {
      FactoryHolder.getFactory()
    }
  }
  
  void testSetFactory() {
    def factory = new DummyFactory()
    FactoryHolder.setFactory(factory)       
    assertSame factory, FactoryHolder.@factory
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
  com.opensymphony.module.sitemesh.DecoratorMapper getDecoratorMapper() { null }
  void refresh() {}
  com.opensymphony.module.sitemesh.PageParser getPageParser(String contentType) { null }
}
