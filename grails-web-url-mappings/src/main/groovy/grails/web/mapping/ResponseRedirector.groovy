/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.web.mapping

import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import grails.web.http.HttpHeaders
import org.grails.web.servlet.mvc.GrailsWebRequest
import grails.web.mapping.mvc.RedirectEventListener
import grails.web.mapping.mvc.exceptions.CannotRedirectException
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.util.Assert
import org.springframework.web.servlet.support.RequestDataValueProcessor

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Encapsulates the logic for issuing a redirect based on a Map of arguments
 *
 * @since 2.4
 * @author Graeme Rocher
 */
@CompileStatic
@Commons
class ResponseRedirector {

    public static final String ARGUMENT_PERMANENT = "permanent"
    public static final String ARGUMENT_ABSOLUTE = "absolute"
    public static final String GRAILS_REDIRECT_ISSUED = GrailsApplicationAttributes.REDIRECT_ISSUED
    private static final String BLANK = "";

    LinkGenerator linkGenerator
    Collection<RedirectEventListener> redirectListeners = []
    RequestDataValueProcessor requestDataValueProcessor
    boolean useJessionId = false

    ResponseRedirector(LinkGenerator linkGenerator) {
        Assert.notNull linkGenerator, "Argument [linkGenerator] cannot be null"
        this.linkGenerator = linkGenerator
    }

    void redirect(Map arguments = Collections.emptyMap()) {
        def webRequest = GrailsWebRequest.lookup()
        HttpServletRequest request = webRequest.currentRequest
        HttpServletResponse response = webRequest.getCurrentResponse()

        redirect(request, response, arguments)
    }

    void redirect(HttpServletRequest request, HttpServletResponse response, Map arguments) {
        if (request.getAttribute(GRAILS_REDIRECT_ISSUED)) {
            throw new CannotRedirectException("Cannot issue a redirect(..) here. A previous call to redirect(..) has already redirected the response.")
        }

        if (response.committed) {
            throw new CannotRedirectException("Cannot issue a redirect(..) here. The response has already been committed either by another redirect or by directly writing to the response.");
        }

        boolean permanent

        def permanentArgument = arguments.get(ARGUMENT_PERMANENT)
        if(permanentArgument instanceof String) {
            permanent = Boolean.valueOf(permanentArgument)
        } else {
            permanent = Boolean.TRUE == permanentArgument
        }

        // we generate a relative link with no context path so that the absolute can be calculated by combining the serverBaseURL
        // which includes the contextPath
        arguments.put LinkGenerator.ATTRIBUTE_CONTEXT_PATH, BLANK

        boolean absolute
        def absoluteArgument = arguments.get(ARGUMENT_ABSOLUTE)
        if (absoluteArgument instanceof String) {
            absolute = Boolean.valueOf(absoluteArgument)
        } else {
            absolute = (absoluteArgument == null) ? true : (Boolean.TRUE == absoluteArgument)
        }

        // When redirecting from UrlMappings the original request params are on webRequest.originalParams
        // instead of arguments.params so we merge them
        def webRequest = GrailsWebRequest.lookup(request)
        if (webRequest.originalParams) {
            Map existingParams = (Map)arguments.get(LinkGenerator.ATTRIBUTE_PARAMS) ?: [:]
            arguments.put(LinkGenerator.ATTRIBUTE_PARAMS, existingParams + webRequest.originalParams)
        }

        redirectResponse(linkGenerator.getServerBaseURL(), linkGenerator.link(arguments), request, response, permanent, absolute)
    }

    /*
     * Redirects the response the the given URI
     */
    private void redirectResponse(String serverBaseURL, String actualUri, HttpServletRequest request, HttpServletResponse response, boolean permanent, boolean absolute) {
        if(log.isDebugEnabled()) {
            log.debug "Method [redirect] forwarding request to [$actualUri]"
            log.debug "Executing redirect with response [$response]"
        }

        String processedActualUri = processedUrl(actualUri, request);

        String redirectURI
        if (absolute) {
            redirectURI = processedActualUri.contains("://") ? processedActualUri : serverBaseURL + processedActualUri
        } else {
            redirectURI = processedActualUri
        }

        String redirectUrl = useJessionId ? response.encodeRedirectURL(redirectURI) : redirectURI
        int status = permanent ? HttpServletResponse.SC_MOVED_PERMANENTLY : HttpServletResponse.SC_MOVED_TEMPORARILY

        response.status = status
        response.setHeader HttpHeaders.LOCATION, redirectUrl

        if (redirectListeners) {
            for (redirectEventListener in redirectListeners) {
                redirectEventListener.responseRedirected redirectUrl
            }
        }

        request.setAttribute GRAILS_REDIRECT_ISSUED, processedActualUri
    }


    private String processedUrl(String link, HttpServletRequest request) {
        if (requestDataValueProcessor) {
            link = requestDataValueProcessor.processUrl(request, link)
        }
        return link
    }
}
