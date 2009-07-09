/**
 * @author mike
 */
package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class ViewUrlMappingTests extends AbstractGrailsControllerTests {
    def topLevelMapping = '''
mappings {
  "/book/$author/$title" {
    view="book.gsp"
  }
  "/book2/foo"(view:"book.gsp")
  "/book3"(controller:"book", view:"list")
}
'''
    def UrlMappingsHolder holder


    void setUp() {
        super.setUp()
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    void testParse() {
        assertNotNull holder
    }

    void testMatch() {
        UrlMappingInfo info = holder.match("/book/joyce/ullisses")

        assertNotNull info
        assertEquals "book.gsp", info.getViewName()
    }

    void testMatch2() {
        UrlMappingInfo info = holder.match("/book2/foo")

        assertNotNull info
        assertEquals "book.gsp", info.getViewName()
    }

    void testMatchToControllerAndView() {
        UrlMappingInfo info = holder.match("/book3")

        assertNotNull info
        assertEquals "list", info.viewName
        assertEquals "book", info.controllerName
    }
}