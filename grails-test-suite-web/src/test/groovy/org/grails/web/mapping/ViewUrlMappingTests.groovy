package org.grails.web.mapping

import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

import org.junit.Test
import static org.junit.Assert.*
import org.springframework.core.io.ByteArrayResource

/**
 * @author mike
 */
@TestMixin(ControllerUnitTestMixin)
class ViewUrlMappingTests  {

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
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(servletContext)
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    @Test
    void testParse() {
        assertNotNull holder
    }

    @Test
    void testMatch() {
        UrlMappingInfo info = holder.match("/book/joyce/ullisses")

        assertNotNull info
        assertEquals "book.gsp", info.getViewName()
    }

    @Test
    void testMatch2() {
        UrlMappingInfo info = holder.match("/book2/foo")

        assertNotNull info
        assertEquals "book.gsp", info.getViewName()
    }

    @Test
    void testMatchToControllerAndView() {
        UrlMappingInfo info = holder.match("/book3")

        assertNotNull info
        assertEquals "list", info.viewName
        assertEquals "book", info.controllerName
    }
}
