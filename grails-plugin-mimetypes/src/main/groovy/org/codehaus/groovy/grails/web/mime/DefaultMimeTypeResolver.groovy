package org.codehaus.groovy.grails.web.mime

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Graeme Rocher
 */
@CompileStatic
class DefaultMimeTypeResolver implements MimeTypeResolver{

    @Autowired ResponseMimeTypesApi responseMimeTypesApi

    @Override
    MimeType resolveResponseMimeType() {
        final webRequest = GrailsWebRequest.lookup()
        if (webRequest != null) {
            return responseMimeTypesApi.getMimeType(webRequest.currentResponse)
        }
        return null
    }
}
