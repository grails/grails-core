/**
 * @author mike
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource
import org.springframework.mock.web.MockServletContext

class ResponseCodeUrlMappingTests extends AbstractGrailsMappingTests {
    def topLevelMapping = '''
mappings {
    "404"{
        controller = "errors"
        action = "error404"
    }

    "500"(controller:"errors", action:"custom", exception:IllegalArgumentException)
    "500"(controller:"errors", action:"error500")
}
'''
    def UrlMappingsHolder holder


    void setUp() {
        super.setUp()
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }


    void testParse() {
        assertNotNull holder
    }

    void testMatch() {
        assertNull holder.match("/")
    }

    void testMatchStatusCodeAndException() {
        def info = holder.matchStatusCode(500)

        assertEquals "error500", info.actionName

        info = holder.matchStatusCode(500, new IllegalArgumentException())

        assertEquals "custom", info.actionName
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