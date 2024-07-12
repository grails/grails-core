package org.grails.web.mapping

import grails.testing.web.GrailsWebUnitTest
import grails.web.mapping.UrlMappingsHolder
import org.junit.Test
import org.springframework.core.io.ByteArrayResource
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class UrlMappingWithCustomValidatorTests extends Specification implements GrailsWebUnitTest {

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

    void setup() {
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(applicationContext)
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }


    void testMatchWithCustomValidator() {
        when:
        def info = holder.match("/help/foo.html")

        then:
        info

        when:
        info = holder.match("/help/js/foo.js")

        then:
        !info
    }
}
