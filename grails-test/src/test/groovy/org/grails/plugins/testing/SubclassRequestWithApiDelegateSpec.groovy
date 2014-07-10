package org.grails.plugins.testing

import spock.lang.Specification

class SubclassRequestWithApiDelegateSpec extends Specification {

    void "Test that delegate methods are available on request"() {
        when:
            def request = getRequestClass().newInstance()

        then:
            request.format == "html"
    }

    Class getRequestClass() {
        def gcl = new GroovyClassLoader()

        gcl.parseClass '''
import org.grails.plugins.web.api.RequestMimeTypesApi
import grails.artefact.ApiDelegate
import javax.servlet.http.HttpServletRequest

class GrailsMockHttpServletRequest extends org.grails.plugins.testing.GrailsMockHttpServletRequest {
    @ApiDelegate(HttpServletRequest) RequestMimeTypesApi requestMimeTypesApi = new RequestMimeTypesApi()
}
'''
    }
}
