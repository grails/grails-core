/**
 * @author mike
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

class ResponseCodeUrlMappingTests extends AbstractGrailsControllerTests {
    def topLevelMapping = '''
mappings {
    "404"{
        controller = "errors"
        action = "error404"
    }
    "500"(controller:"errors", action:"error500")
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
        assertNull holder.match("/")
    }

    void testForwardMapping() {
        def info = holder.matchStatusCode(404)
        assertNotNull info
        assertEquals("errors", info.getControllerName());
        assertEquals("error404", info.getActionName());
    }

    void testForwardMappingWithNamedArgs() {
        def info = holder.matchStatusCode(500)
        assertNotNull info
        assertEquals("errors", info.getControllerName());
        assertEquals("error500", info.getActionName());
    }

    void testMissingForwardMapping() {
        def info = holder.matchStatusCode(501)
        assertNull info
    }

    void testNoReverseMappingOccures() {
        def creator = holder.getReverseMapping("errors", "error404", null)

        assertTrue ("Creator is of wrong type: " + creator.class, creator instanceof DefaultUrlCreator)
    }
}