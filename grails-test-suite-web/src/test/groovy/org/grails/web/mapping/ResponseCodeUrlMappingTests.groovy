package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import spock.lang.PendingFeature
import spock.lang.Specification

/**
 * @author mike
 */
class ResponseCodeUrlMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    void testParse() {
        expect:
        urlMappingsHolder
    }

    void testMatch() {
        expect:
        !urlMappingsHolder.match("/")
    }

    void testMatchStatusCodeAndException() {
        when:
        def info = urlMappingsHolder.matchStatusCode(500)

        then:
        "error500" == info.actionName

        when:
        info = urlMappingsHolder.matchStatusCode(500, new IllegalArgumentException())

        then:
        "custom" == info.actionName
    }

    void testForwardMapping() {
        when:
        def info = urlMappingsHolder.matchStatusCode(404)

        then:
        info
        "errors" == info.getControllerName()
        "error404" == info.getActionName()
    }

    void testForwardMappingWithNamedArgs() {
        when:
        def info = urlMappingsHolder.matchStatusCode(500)

        then:
        info
        "errors" == info.getControllerName()
        "error500" ==  info.getActionName()
    }

    void testMissingForwardMapping() {
        when:
        def info = urlMappingsHolder.matchStatusCode(501)
        then:
        !info
    }

    void testNoReverseMappingOccures() {
        when:
        def creator = urlMappingsHolder.getReverseMapping("errors", "error404", null)

        then:
        creator.delegate instanceof DefaultUrlCreator
    }

    static class UrlMappings {
        static mappings = {
            "404"{
                controller = "errors"
                action = "error404"
            }

            "500"(controller:"errors", action:"custom", exception:IllegalArgumentException)
            "500"(controller:"errors", action:"error500")
        }
    }
}
