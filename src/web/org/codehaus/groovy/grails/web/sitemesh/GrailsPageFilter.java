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
package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.filter.PageFilter;

/**
 * Extends the default page filter to overide the apply decorator behaviour
 * if the page is a GSP
 *  
 * @author Graeme Rocher
 * @since Apr 19, 2006
 */
public class GrailsPageFilter extends PageFilter {

	private static final Log LOG = LogFactory.getLog( GrailsPageFilter.class );
	
	/* (non-Javadoc)
	 * @see com.opensymphony.module.sitemesh.filter.PageFilter#applyDecorator(com.opensymphony.module.sitemesh.Page, com.opensymphony.module.sitemesh.Decorator, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void applyDecorator(Page page, Decorator decorator, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if(decorator.getURIPath().endsWith(".gsp")) {
    		request.setAttribute(PAGE, page);
            ServletContext context = filterConfig.getServletContext();
            // see if the URI path (webapp) is set
            if (decorator.getURIPath() != null) {
                // in a security conscious environment, the servlet container
                // may return null for a given URL
                if (context.getContext(decorator.getURIPath()) != null) {
                    context = context.getContext(decorator.getURIPath());
                }
            }    		
                      
            RequestDispatcher rd = request.getRequestDispatcher(decorator.getURIPath());
            if(!response.isCommitted()) {
                if(LOG.isDebugEnabled()) {
                	LOG.debug("Rendering layout using forward: " + decorator.getURIPath());
                }            	            	
            	rd.forward(request, response);
            } 
            else {
                if(LOG.isDebugEnabled()) {
                	LOG.debug("Rendering layout using include: " + decorator.getURIPath());
                }
                request.setAttribute(GrailsApplicationAttributes.GSP_TO_RENDER,decorator.getURIPath());
                rd.include(request,response);
                request.removeAttribute(GrailsApplicationAttributes.GSP_TO_RENDER);
            }
            
            // set the headers specified as decorator init params
            while (decorator.getInitParameterNames().hasNext()) {
                String initParam = (String) decorator.getInitParameterNames().next();
                if (initParam.startsWith("header.")) {
                    response.setHeader(initParam.substring(initParam.indexOf('.')), decorator.getInitParameter(initParam));
                }
            }
            request.removeAttribute(PAGE);        		        		
    	}
    	else {
    		
    		super.applyDecorator(page, decorator, request, response);
    		
    	}
	}

}
