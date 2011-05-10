package org.codehaus.groovy.grails.plugins.testing

import spock.lang.Specification

 /**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/05/2011
 * Time: 17:02
 * To change this template use File | Settings | File Templates.
 */
class SubclassRequestWithApiDelegateSpec extends Specification{

    void "Test that delegate methods are available on request"() {
        when:
            def request = getRequestClass().newInstance()

        then:
            request.format == "html"
    }

    Class getRequestClass() {
        def gcl = new GroovyClassLoader()

        gcl.parseClass '''
import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.plugins.web.api.RequestMimeTypesApi
import grails.artefact.ApiDelegate
import javax.servlet.http.HttpServletRequest

class GrailsMockHttpServletRequest extends MockHttpServletRequest  {
    @ApiDelegate(HttpServletRequest) RequestMimeTypesApi requestMimeTypesApi = new RequestMimeTypesApi()
}
'''
    }
}
