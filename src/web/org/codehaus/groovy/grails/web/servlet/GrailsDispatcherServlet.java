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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsBootstrapClass;
import org.codehaus.groovy.grails.commons.GrailsConfigUtils;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

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

    public GrailsDispatcherServlet() {
        super();
        setDetectAllHandlerMappings(false);
    }

    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) throws BeansException {
    	WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    	WebApplicationContext webContext;
        // construct the SpringConfig for the container managed application
        GrailsApplication application = (GrailsApplication) parent.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
    	
    	if(wac instanceof GrailsWebApplicationContext) {
    		webContext = wac;
    	}
    	else {
            webContext = GrailsConfigUtils.configureWebApplicationContext(getServletContext(), parent);     		
    	}
                // configure scaffolders
        GrailsConfigUtils.configureScaffolders(application, webContext);
        GrailsConfigUtils.executeGrailsBootstraps(application, webContext, getServletContext());

        return webContext;
    }

	public void destroy() {
        WebApplicationContext webContext = getWebApplicationContext();
        GrailsApplication application = (GrailsApplication) webContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

        GrailsBootstrapClass[] bootstraps =  application.getGrailsBootstrapClasses();
        for (int i = 0; i < bootstraps.length; i++) {
            bootstraps[i].callDestroy();
        }
        // call super
        super.destroy();
    }

}
