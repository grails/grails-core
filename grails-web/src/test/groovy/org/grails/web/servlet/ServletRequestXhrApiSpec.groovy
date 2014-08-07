package org.grails.web.servlet

import org.springframework.mock.web.MockHttpServletRequest

import spock.lang.Specification

class ServletRequestXhrApiSpec extends Specification{

    void "Test identification of XHR requests"() {
        when:"a regular request is used"
            def request = new MockHttpServletRequest()
        then:"it isn't an XHR request"
            request.isXhr() == false

        when:"A non XHR request is sent with the X-Requested-With header"
            request.addHeader("X-Requested-With","com.android.browser")

        then:"It is not an XHR request"
            request.isXhr() == false

        when:"A request is sent with a X-Requested-With value of XMLHttpRequest"
            request = new MockHttpServletRequest()
            request.addHeader("X-Requested-With", "XMLHttpRequest")

        then:"It is an XHR request"
            request.isXhr() == true
    }
}
