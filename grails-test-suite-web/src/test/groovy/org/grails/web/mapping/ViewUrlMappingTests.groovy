package org.grails.web.mapping

import grails.testing.web.GrailsWebUnitTest
import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.UrlMappingsHolder
import org.junit.Test
import spock.lang.Ignore
import spock.lang.Specification
import static org.junit.Assert.*
import org.springframework.core.io.ByteArrayResource

/**
 * @author mike
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class ViewUrlMappingTests extends Specification implements GrailsWebUnitTest {

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

    void setup() {
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(applicationContext)
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    void testParse() {
        expect:
        holder != null
    }

    void testMatch() {
        when:
        UrlMappingInfo info = holder.match("/book/joyce/ullisses")

        then:
        "book.gsp" == info.getViewName()
    }

    void testMatch2() {
        when:
        UrlMappingInfo info = holder.match("/book2/foo")

        then:
        "book.gsp" == info.getViewName()
    }

    void testMatchToControllerAndView() {
        when:
        UrlMappingInfo info = holder.match("/book3")

        then:
        "list" == info.viewName
        "book" == info.controllerName
    }
}
