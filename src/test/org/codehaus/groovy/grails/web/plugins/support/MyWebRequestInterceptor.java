package org.codehaus.groovy.grails.web.plugins.support;

import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

public class MyWebRequestInterceptor implements WebRequestInterceptor {

    public void afterCompletion(WebRequest request, Exception ex) {
        // do nothing
    }

    public void postHandle(WebRequest request, ModelMap model) {
        // do nothing
    }

    public void preHandle(WebRequest request) throws Exception {
        // do nothing
    }
}
