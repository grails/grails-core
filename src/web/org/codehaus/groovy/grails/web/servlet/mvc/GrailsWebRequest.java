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

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletResponse;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

/**
 * A class the encapsulates a Grails request. An instance of this class is bound to the current thread using
 * Spring's RequestContextHolder which can later be retrieved using:
 * 
 * def webRequest = RequestContextHolder.currentRequestAttributes()
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class GrailsWebRequest extends DispatcherServletWebRequest {

	private GrailsApplicationAttributes attributes;
	private GrailsParameterMap params;
	private GrailsHttpServletResponse response;
	private GrailsHttpSession session;
	private boolean renderView = true;
    private static final String ACTION_NAME = "org.codehaus.groovy.grails.ACTION_NAME";
    private static final String CONTROLLER_NAME = "org.codehaus.groovy.grails.CONTROLLER_NAME";


    public GrailsWebRequest(HttpServletRequest request,  HttpServletResponse response, ServletContext servletContext) {
		super(new GrailsHttpServletRequest(request));
		this.attributes = new DefaultGrailsApplicationAttributes(servletContext);
		this.response = new GrailsHttpServletResponse(response);
		this.params = new GrailsParameterMap(request);
		
	}
	
	
	
	/**
	 * @return the out
	 */
	public Writer getOut() {
		Writer out = attributes.getOut(getCurrentRequest());
		if(out ==null)
			try {
				return getCurrentResponse().getWriter();
			} catch (IOException e) {
				throw new ControllerExecutionException("Error retrieving response writer: " + e.getMessage(), e);
			}
		return out;
	}



	/**
	 * @param out the out to set
	 */
	public void setOut(Writer out) {
		attributes.setOut(getCurrentRequest(),out);
	}



	/**
	 * @return The ServletContext instance
	 */
	public ServletContext getServletContext() {
		return this.attributes.getServletContext();
	}

	/**
	 * @return The FlashScope instance for the current request
	 */
	public FlashScope getFlashScope() {
		return attributes.getFlashScope(getRequest());
	}
	
	/**
	 * @return The currently executing request
	 */
	public GrailsHttpServletRequest getCurrentRequest() {
		return (GrailsHttpServletRequest)getRequest();
	}
	
	public GrailsHttpServletResponse getCurrentResponse() {
		return this.response;
	}
	
	/**
	 * @return The Grails params object
	 */
	public GrailsParameterMap getParams() {
		return this.params;
	}
	
	/**
	 * 
	 * @return The Grails session object
	 */
	public GrailsHttpSession getSession() {
		if(session == null)
			session = new GrailsHttpSession(getCurrentRequest().getSession(true));
		
		return session;
	}
	
	/**
	 * @return The GrailsApplicationAttributes instance
	 */
	public GrailsApplicationAttributes getAttributes() {
		return this.attributes;
	}

	public void setActionName(String actionName) {
		getCurrentRequest().setAttribute(ACTION_NAME, actionName);
	}

	public void setControllerName(String controllerName) {
		getCurrentRequest().setAttribute(CONTROLLER_NAME, controllerName);
	}

	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return (String)getCurrentRequest().getAttribute(ACTION_NAME);
	}

	/**
	 * @return the controllerName
	 */
	public String getControllerName() {
		return (String)getCurrentRequest().getAttribute(CONTROLLER_NAME);
	}

	public void setRenderView(boolean renderView) {
		this.renderView = renderView;
	}

	/**
	 * @return True if the view for this GrailsWebRequest should be rendered
	 */
	public boolean isRenderView() {
		return renderView;
	}	
	
}
