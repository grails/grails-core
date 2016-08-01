package org.codehaus.groovy.grails.web.mapping

import grails.util.GrailsWebMockUtil
import org.grails.web.mapping.CachingLinkGenerator
import org.grails.web.servlet.mvc.DefaultRequestStateLookupStrategy
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests for the {@link org.grails.web.mapping.CachingLinkGenerator} class
 */
class CachingLinkGeneratorSpec extends Specification {

    @Shared
    MyCachingLinkGenerator linkGenerator

    @Shared
    GrailsWebRequest request

    void setup() {
        linkGenerator = new MyCachingLinkGenerator("http://grails.org/")
        request = GrailsWebMockUtil.bindMockWebRequest()
        linkGenerator.requestStateLookupStrategy = new DefaultRequestStateLookupStrategy(request)
    }

    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    void "test namespace"() {
        given:
        String key

        when: "not in the request or params"
        key = linkGenerator.makeKey([controller: "foo", action: "bar"])

        then: "its not in the key"
        key == "link[controller:foo, action:bar]"

        when: "its in the params"
        key = linkGenerator.makeKey([controller: "foo", action: "bar", namespace: "foo"])

        then: "its in the key"
        key == "link[controller:foo, action:bar, namespace:foo]"

        when: "its in the request"
        request.setControllerNamespace("fooReq")
        key = linkGenerator.makeKey([controller: "foo", action: "bar"])

        then: "its in the key"
        key == "link[controller:foo, action:bar, namespace:fooReq]"

        when: "its in the request and the params"
        request.setControllerNamespace("fooReq")
        key = linkGenerator.makeKey([controller: "foo", action: "bar", namespace: "fooParam"])

        then: "params wins"
        key == "link[controller:foo, action:bar, namespace:fooParam]"
    }


    class MyCachingLinkGenerator extends CachingLinkGenerator {
        public MyCachingLinkGenerator(String serverBaseURL) {
            super(serverBaseURL)
        }

        String makeKey(Map attrs) {
            super.makeKey(LINK_PREFIX, attrs)
        }
    }
}
