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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffoldingUtil;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.util.UrlPathHelper;

import java.util.Map;
import java.util.Iterator;

import grails.util.GrailsUtil;

/**
 * <p>Servlet that handles incoming requests for Grails.
 * <p/>
 * <p>This servlet loads the Spring configuration based on the Grails application
 * in the parent application context.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since Jul 2, 2005
 */
public class GrailsDispatcherServlet extends DispatcherServlet {
    private GrailsApplication application;
    private UrlPathHelper urlHelper = new UrlPathHelper();
    private SimpleGrailsController grailsController;
    private HandlerInterceptor[] interceptors;

    public GrailsDispatcherServlet() {
        super();
        setDetectAllHandlerMappings(false);
    }

    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) throws BeansException {
    	WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    	WebApplicationContext webContext;
        // construct the SpringConfig for the container managed application
        this.application = (GrailsApplication) parent.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

    	
        if(wac instanceof GrailsWebApplicationContext) {
    		webContext = wac;
    	}
    	else {
            webContext = GrailsConfigUtils.configureWebApplicationContext(getServletContext(), parent);     		
    	}

        initGrailsController(webContext);

        this.interceptors = establishInterceptors(webContext);
        GrailsConfigUtils.executeGrailsBootstraps(application, webContext, getServletContext());

        return webContext;
    }

    private void initGrailsController(WebApplicationContext webContext) {
        if(webContext.containsBean(SimpleGrailsController.APPLICATION_CONTEXT_ID)) {
            this.grailsController = (SimpleGrailsController)webContext.getBean(SimpleGrailsController.APPLICATION_CONTEXT_ID);
        }
    }

    /**
     * Evalutes the given WebApplicationContext for all HandlerInterceptor and WebRequestInterceptor instances
     *
     * @param webContext The WebApplicationContext
     * @return An array of HandlerInterceptor instances
     */
    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext) {
        HandlerInterceptor[] interceptors;
        String[] interceptorNames = webContext.getBeanNamesForType(HandlerInterceptor.class);
        String[] webRequestInterceptors = webContext.getBeanNamesForType( WebRequestInterceptor.class);
        interceptors = new HandlerInterceptor[interceptorNames.length+webRequestInterceptors.length];

        int j = 0;
        for (int i = 0; i < interceptorNames.length; i++) {
            interceptors[i] = (HandlerInterceptor)webContext.getBean(interceptorNames[i]);
            j = i+1;
        }
        for (int i = 0; i < webRequestInterceptors.length; i++) {
            j = i+j;
            interceptors[j] = new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor)webContext.getBean(webRequestInterceptors[i]));
        }
        return interceptors;
    }

    public void destroy() {
        WebApplicationContext webContext = getWebApplicationContext();
        GrailsApplication application = (GrailsApplication) webContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

        GrailsClass[] bootstraps =  application.getArtefacts(BootstrapArtefactHandler.TYPE);
        for (int i = 0; i < bootstraps.length; i++) {
            ((GrailsBootstrapClass)bootstraps[i]).callDestroy();
        }
        // call super
        super.destroy();
    }


    public void setApplication(GrailsApplication application) {
        this.application = application;
    }

    /* (non-Javadoc)
	 * @see org.springframework.web.servlet.DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		super.doDispatch(request, response);
	}


    /**
     * Overrides the default behaviour to establish the handler from the GrailsApplication instance
     *
     * @param request The request
     * @param cache Whether to cache the Handler in the request
     * @return The HandlerExecutionChain
     *
     * @throws Exception
     */
    protected HandlerExecutionChain getHandler(HttpServletRequest request, boolean cache) throws Exception {
        String uri = urlHelper.getPathWithinApplication(request);
        GrailsControllerClass controllerClass = (GrailsControllerClass) application.getArtefactForFeature(
            ControllerArtefactHandler.TYPE, uri);
        if(controllerClass!=null) {
             HandlerInterceptor[] interceptors;
            // if we're in a development environment we want to re-establish interceptors just in case they
            // have changed at runtime
             if(GrailsUtil.isDevelopmentEnv()) {
                  interceptors = establishInterceptors(getWebApplicationContext());
             }
             else {
                  interceptors = this.interceptors;
             }
             if(grailsController == null) {
                 initGrailsController(getWebApplicationContext());
             }
             return new HandlerExecutionChain(grailsController, interceptors);
        }
        else {
            return super.getHandler(request, cache);
        }
    }


}
