package org.grails.web.mapping

import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

import org.junit.Test
import org.springframework.core.io.ByteArrayResource

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestMixin(ControllerUnitTestMixin)
class UrlMappingWithCustomValidatorTests {

    def topLevelMapping = '''
mappings {
    "/help/$path**"(controller : "wiki", action : "show", id : "1") {
        constraints {
            path(validator : { val, obj -> ! val.startsWith("js") })
        }
    }
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
    void testMatchWithCustomValidator() {
        def info = holder.match("/help/foo.html")
        assert info

        info = holder.match("/help/js/foo.js")
        assert !info
    }
}
