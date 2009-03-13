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
package org.codehaus.groovy.grails.web.servlet.mvc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Base class for Grails controllers.
 *
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public class SimpleGrailsController implements Controller, ServletContextAware {

    public static final String APPLICATION_CONTEXT_ID = "simpleGrailsController";
    
    private UrlPathHelper urlPathHelper = new GrailsUrlPathHelper();
    private GrailsApplication application = null;
    private ServletContext servletContext;

    private static final Log LOG = LogFactory.getLog(SimpleGrailsController.class);


    public SimpleGrailsController() {
        super();        
    }


    public void setGrailsApplication(GrailsApplication application) {
        this.application = application;
    }

    /**
     * <p>This method wraps regular request and response objects into Grails request and response objects.
     *
     * <p>It can handle maps as model types next to ModelAndView instances.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return the model
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        // Step 1: determine the correct URI of the request.
        String uri = this.urlPathHelper.getPathWithinApplication(request);
        if(LOG.isDebugEnabled()) {
            LOG.debug("[SimpleGrailsController] Processing request for uri ["+uri+"]");
        }

        
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();

        if(!(ra instanceof GrailsWebRequest)) {
            throw new IllegalStateException("Bound RequestContext is not an instance of GrailsWebRequest");
        }
        GrailsWebRequest webRequest = (GrailsWebRequest)ra;


        ApplicationContext context = webRequest.getAttributes().getApplicationContext();
        SimpleGrailsControllerHelper helper = new SimpleGrailsControllerHelper(this.application,context,this.servletContext);
        ModelAndView mv = helper.handleURI(uri,webRequest);

        if(LOG.isDebugEnabled()) {
            if(mv != null) {
                LOG.debug("[SimpleGrailsController] Forwarding model and view ["+mv+"] with class ["+(mv.getView() != null ? mv.getView().getClass().getName() : mv.getViewName())+"]");    
            }
        }
        return mv;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }
}
