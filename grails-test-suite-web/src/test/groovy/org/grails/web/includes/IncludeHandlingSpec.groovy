package org.grails.web.includes

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin

import org.springframework.web.util.WebUtils

import spock.lang.Specification

/**
 * Tests the behavior of the include tag
 */
@TestMixin(GroovyPageUnitTestMixin)
class IncludeHandlingSpec extends Specification {

    void "Test the appropriate request headers are set and URI of a page included"() {
        given:"A template that includes a view"
            views['/foo/_bar.gsp'] = 'Include = <g:include view="/foo/include.gsp" model="[foo:\'bar\']"/>'

        when:"The template is rendered"
            request.foo = "dontchange"
            def content = render(template:"/foo/bar")

        then:"The include status is valid"
            request.foo == "dontchange"
            content == "Include = "
            response.includedUrls
            response.includedUrls[0] == '/foo/include.gsp'
            request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE) == null
            request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE) == null
            request.getAttribute(WebUtils.INCLUDE_PATH_INFO_ATTRIBUTE) == null
            request.getAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE) == null
            request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE) == null
    }
}
