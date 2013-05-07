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
package org.codehaus.groovy.grails.web.servlet;

import grails.util.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.UrlPathHelper;

/**
 * Matches Grails' SimpleController class.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class GrailsControllerHandlerMapping extends AbstractHandlerMapping implements GrailsApplicationAware {

    public static final String MAIN_CONTROLLER_BEAN = "mainSimpleController";
    private GrailsApplication grailsApplication;
    private UrlPathHelper urlHelper = new GrailsUrlPathHelper();

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

        String uri = urlHelper.getPathWithinApplication(request);
        if (logger.isDebugEnabled()) {
            logger.debug("Looking up Grails controller for URI ["+uri+"]");
        }
        GrailsControllerClass controllerClass;
        Object controllerAttribute = null;
        GrailsWebRequest webRequest = (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
        if (webRequest != null) {
            controllerAttribute = webRequest.getAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS, WebRequest.SCOPE_REQUEST);
            Boolean canUse = (Boolean)webRequest.getAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, WebRequest.SCOPE_REQUEST);
            if(canUse == null || !canUse) {
                controllerAttribute = null;
            }
        }

        if (controllerAttribute instanceof GrailsControllerClass) {
            controllerClass = (GrailsControllerClass) controllerAttribute;
        } else {
            controllerClass = (GrailsControllerClass) grailsApplication.getArtefactForFeature(
                    ControllerArtefactHandler.TYPE, uri);
        }

        return getHandlerForControllerClass(controllerClass, request);
    }

    /**
     * Obtains the handler for the given controller class.
     *
     * @param controllerClass The controller class
     * @param request The HttpServletRequest
     * @return The handler
     */
    protected Object getHandlerForControllerClass(GrailsControllerClass controllerClass, HttpServletRequest request) {
        if (controllerClass != null) {
            try {
                return getWebApplicationContext().getBean(MAIN_CONTROLLER_BEAN, Controller.class);
            }
            catch (NoSuchBeanDefinitionException e) {
                // ignore
            }
        }
        return null;
    }

    @Override
    protected final HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
        if (handler instanceof HandlerExecutionChain) {
            HandlerExecutionChain chain = (HandlerExecutionChain) handler;
            chain.addInterceptors(lookupInterceptors(getWebApplicationContext()));
            return chain;
        }

        return new HandlerExecutionChain(handler, lookupInterceptors(getWebApplicationContext()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void extendInterceptors(List interceptors) {
        setInterceptors(establishInterceptors(getWebApplicationContext(), interceptors));
    }

    protected HandlerInterceptor[] lookupInterceptors(WebApplicationContext applicationContext) {
        if (Environment.getCurrent() == Environment.DEVELOPMENT) {
            return establishInterceptors(applicationContext);
        }

        return getAdaptedInterceptors();
    }

    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext) {
        return establishInterceptors(webContext, Collections.emptyList());
    }

    /**
     * Evalutes the given WebApplicationContext for all HandlerInterceptor and WebRequestInterceptor instances
     *
     * @param webContext The WebApplicationContext
     * @return An array of HandlerInterceptor instances
     */
    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext, List<?> previousInterceptors) {
        String[] interceptorNames = webContext.getBeanNamesForType(HandlerInterceptor.class);
        String[] webRequestInterceptors = webContext.getBeanNamesForType(WebRequestInterceptor.class);
        List<HandlerInterceptor> interceptors = new ArrayList<HandlerInterceptor>();

        // Merge the handler and web request interceptors into a single array. Note that we start with
        // the web request interceptors to ensure that the OpenSessionInViewInterceptor (which is a
        // web request interceptor) is invoked before the user-defined filters (which are attached to
        // a handler interceptor). This should ensure that the Hibernate session is in the proper
        // state if and when users access the database within their filters.
        for (String webRequestInterceptor : webRequestInterceptors) {
            WebRequestInterceptor interceptor = webContext.getBean(webRequestInterceptor, WebRequestInterceptor.class);
            if (!previousInterceptors.contains(interceptor)) {
                interceptors.add(new WebRequestHandlerInterceptorAdapter(interceptor));
            }
        }
        for (String interceptorName : interceptorNames) {
            HandlerInterceptor interceptor = webContext.getBean(interceptorName, HandlerInterceptor.class);
            if (!previousInterceptors.contains(interceptor)) {
                interceptors.add(interceptor);
            }
        }
        return interceptors.toArray(new HandlerInterceptor[interceptors.size()]);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
