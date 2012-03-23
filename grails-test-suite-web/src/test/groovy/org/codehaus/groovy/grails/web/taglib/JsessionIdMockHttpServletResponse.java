package org.codehaus.groovy.grails.web.taglib;

import org.springframework.mock.web.MockHttpServletResponse;

public class JsessionIdMockHttpServletResponse extends MockHttpServletResponse {
    public String encodeURL(String url) {
        return super.encodeURL(url + ";jsessionid=test");
    }
}
