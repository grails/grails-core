/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.servlet.HttpHeaders;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.web.mapping.LinkGenerator;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.RedirectEventListener;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.CannotRedirectException;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implements the "redirect" Controller method for action redirection.
 *
 * @author Graeme Rocher
 * @since 0.2
 *
 * Created Oct 27, 2005
 */
public class RedirectDynamicMethod extends AbstractDynamicMethodInvocation {

    public static final String METHOD_SIGNATURE = "redirect";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    public static final String GRAILS_VIEWS_ENABLE_JSESSIONID = "grails.views.enable.jsessionid";
    public static final String GRAILS_REDIRECT_ISSUED = GrailsApplicationAttributes.REDIRECT_ISSUED;

    public static final String ARGUMENT_ERRORS = "errors";

    public static final String ARGUMENT_PERMANENT = "permanent";

    private static final Log LOG = LogFactory.getLog(RedirectDynamicMethod.class);
    private static final String BLANK = "";
    private boolean useJessionId = false;
    private Collection<RedirectEventListener> redirectListeners;
    private LinkGenerator linkGenerator;

    /**
     */
    public RedirectDynamicMethod(Collection<RedirectEventListener> redirectListeners) {
        super(METHOD_PATTERN);
        this.redirectListeners = redirectListeners;
    }

    /**
     * @param applicationContext The ApplicationContext
     * @deprecated Here fore compatibility, will be removed in a future version of Grails
     */
    @Deprecated
    public RedirectDynamicMethod(@SuppressWarnings("unused") ApplicationContext applicationContext) {
        super(METHOD_PATTERN);
    }

    public RedirectDynamicMethod() {
        super(METHOD_PATTERN);
    }

    public void setLinkGenerator(LinkGenerator linkGenerator) {
        this.linkGenerator = linkGenerator;
    }

    public void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        this.redirectListeners = redirectListeners;
    }

    public void setUseJessionId(boolean useJessionId) {
        this.useJessionId = useJessionId;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    @Override
    public Object invoke(Object target, String methodName, Object[] arguments) {
        if (arguments.length == 0) {
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);
        }

        Map argMap = arguments[0] instanceof Map ? (Map)arguments[0] : Collections.EMPTY_MAP;
        if (argMap.isEmpty()) {
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);
        }

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        LinkGenerator requestLinkGenerator = getLinkGenerator(webRequest);

        HttpServletRequest request = webRequest.getCurrentRequest();
        if (request.getAttribute(GRAILS_REDIRECT_ISSUED) != null) {
            throw new CannotRedirectException("Cannot issue a redirect(..) here. A previous call to redirect(..) has already redirected the response.");
        }

        HttpServletResponse response = webRequest.getCurrentResponse();
        if (response.isCommitted()) {
            throw new CannotRedirectException("Cannot issue a redirect(..) here. The response has already been committed either by another redirect or by directly writing to the response.");
        }

        GroovyObject controller = (GroovyObject)target;

        // if there are errors add it to the list of errors
        Errors controllerErrors = (Errors)controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY);
        Errors errors = (Errors)argMap.get(ARGUMENT_ERRORS);
        if (controllerErrors != null) {
            controllerErrors.addAllErrors(errors);
        }
        else {
            controller.setProperty(ControllerDynamicMethods.ERRORS_PROPERTY, errors);
        }

        boolean permanent = DefaultGroovyMethods.asBoolean(argMap.get(ARGUMENT_PERMANENT));

        Object action = argMap.get(GrailsControllerClass.ACTION);
        if (action != null) {
            argMap.put(GrailsControllerClass.ACTION, establishActionName(action,controller));
        }

        // we generate a relative link with no context path so that the absolute can be calculated by combining the serverBaseURL
        // which includes the contextPath
        argMap.put(LinkGenerator.ATTRIBUTE_CONTEXT_PATH, BLANK);
        return redirectResponse(requestLinkGenerator.getServerBaseURL(), requestLinkGenerator.link(argMap), request, response, permanent);
    }

    private LinkGenerator getLinkGenerator(GrailsWebRequest webRequest) {
        if (this.linkGenerator == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                linkGenerator = applicationContext.getBean("grailsLinkGenerator", LinkGenerator.class);
            }
        }

        return linkGenerator;
    }

    /*
     * Redirects the response the the given URI
     */
    private Object redirectResponse(String serverBaseURL, String actualUri, HttpServletRequest request, HttpServletResponse response, boolean permanent) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dynamic method [redirect] forwarding request to ["+actualUri +"]");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing redirect with response ["+response+"]");
        }

        String absoluteURL = serverBaseURL + actualUri;
        String redirectUrl = useJessionId ? response.encodeRedirectURL(absoluteURL) : absoluteURL;
        int status = permanent ? HttpServletResponse.SC_MOVED_PERMANENTLY : HttpServletResponse.SC_MOVED_TEMPORARILY;

        response.setStatus(status);
        response.setHeader(HttpHeaders.LOCATION, redirectUrl);

        if (redirectListeners != null) {
            for (RedirectEventListener redirectEventListener : redirectListeners) {
                redirectEventListener.responseRedirected(redirectUrl);
            }
        }

        request.setAttribute(GRAILS_REDIRECT_ISSUED, actualUri);
        return null;
    }

    /*
     * Figures out the action name from the specified action reference (either a string or closure)
     */
    private String establishActionName(Object actionRef, Object target) {
        String actionName = null;
        if (actionRef instanceof String) {
            actionName = (String)actionRef;
        }
        else if (actionRef instanceof CharSequence) {
            actionName = actionRef.toString();
        }
        else if (actionRef instanceof Closure) {
            actionName = GrailsClassUtils.findPropertyNameForValue(target, actionRef);
        }
        return actionName;
    }
}
