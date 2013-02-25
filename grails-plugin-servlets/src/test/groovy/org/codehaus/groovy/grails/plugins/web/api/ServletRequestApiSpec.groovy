package org.codehaus.groovy.grails.plugins.web.api

import org.springframework.mock.web.MockHttpServletRequest

import spock.lang.Specification

class ServletRequestApiSpec extends Specification{

    void "Test identification of XHR requests"() {
        given:"An instance of the servlet API extension"
            def api = new ServletRequestApi()

        when:"a regular request is used"
            def request = new MockHttpServletRequest()
        then:"it isn't an XHR request"
            api.isXhr(request) == false

        when:"A non XHR request is sent with the X-Requested-With header"
            request.addHeader("X-Requested-With","com.android.browser")

        then:"It is not an XHR request"
            api.isXhr(request) == false

        when:"A request is sent with a X-Requested-With value of XMLHttpRequest"
            request = new MockHttpServletRequest()
            request.addHeader("X-Requested-With", "XMLHttpRequest")

        then:"It is an XHR request"
            api.isXhr(request) == true

        when:"A custom XHR request identifier is used"
            api.xhrRequestIdentifier = { r -> r.getHeader('X-Requested-With') == 'Ext.basex'  }
            request = new MockHttpServletRequest()
            request.addHeader("X-Requested-With", "Ext.basex")

        then:"It is an XHR request"
            api.isXhr(request) == true
    }
}
