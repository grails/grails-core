package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import grails.util.GrailsWebMockUtil

import org.springframework.core.io.ByteArrayResource
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class OverlappingUrlMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {


    void testEvaluateMappings() {

        when:
        Map params = [id: "contact"]
        def reverse = urlMappingsHolder.getReverseMapping("content", "view", params)

        then:
        "/contact" == reverse.createURL(params, "utf-8")


        when:
        params.dir = "fred"
        reverse = urlMappingsHolder.getReverseMapping("content", "view", params)


        then:
        "/contact/fred" == reverse.createURL(params, "utf-8")
    }

    static class UrlMappings {
        static mappings = {
            "/$id?" {
                controller = "content"
                action = "view"
            }
            "/$id/$dir" {
                controller = "content"
                action = "view"
            }
        }
    }
}
