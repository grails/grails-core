/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.api;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * API shared by controllers, tag libraries and any other web artifact
 * 
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public class CommonWebApi {
	private GrailsPluginManager pluginManager;

	public CommonWebApi(GrailsPluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	/**
	 * Obtains the Grails parameter map
	 * 
	 * @return The GrailsParameterMap instance
	 * 
	 */
	public GrailsParameterMap getParams(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getParams();
	}

	/**
	 * Obtains the Grails FlashScope instance
	 * 
	 * @return The FlashScope instance
	 */
	public FlashScope getFlash(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getFlashScope();    	
	}

	/**
	 * Obtains the HttpSession instance
	 * 
	 * @return The HttpSession instance
	 */
	public HttpSession getSession(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getSession();    	    	
	}

	/**
	 * Obtains the HttpServletRequest instance
	 * 
	 * @return The HttpServletRequest instance
	 */
	public HttpServletRequest getRequest(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getCurrentRequest();	    	
		
	}

	/**
	 * Obtains the ServletContext instance
	 * 
	 * @return The ServletContext instance
	 */
	public ServletContext getServletContext(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getServletContext();    	    	
		
	}

	/**
	 * Obtains the HttpServletResponse instance
	 * 
	 * @return The HttpServletResponse instance
	 */
	public HttpServletResponse getResponse(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getCurrentResponse();    	    	    	
	}

	/**
	 * Obtains the GrailsApplicationAttributes instance
	 * 
	 * @return The GrailsApplicationAttributes instance
	 */
	public GrailsApplicationAttributes getGrailsAttributes(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getAttributes();    	    	    	
		
	}

	/**
	 * Obtains the GrailsApplication instance
	 * @return The GrailsApplication instance
	 */
	public GrailsApplication getGrailsApplication(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getAttributes().getGrailsApplication();    	    	    	    	
	}
	
	/**
	 * Obtains the ApplicationContext instance
	 * @return The ApplicationContext instance
	 */
	public ApplicationContext getApplicationContext(Object instance) {
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getApplicationContext();    	    	    	    	
	}	

	/**
	 * Obtains the currently executing action name
	 * @return The action name
	 */
	public String getActionName(Object instance) { 
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getActionName();    	    	    	    	
	}

	/**
	 * Obtains the currently executing controller name
	 * @return The controller name
	 */
	public String getControllerName(Object instance) { 
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		return webRequest.getControllerName();    	    	    	    	
	}

	/**
	 * Obtains the currently executing web request
	 * 
	 * @return The GrailsWebRequest instance
	 */
	public GrailsWebRequest getWebRequest(Object instance) {
		return (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
	}

	/**
	 * Obtains the pluginContextPath
	 * 
	 * @param delegate The object the method is being invoked on
	 * @return The plugin context path
	 */
	public String getPluginContextPath(Object delegate) {
	    final String pluginPath = pluginManager.getPluginPathForInstance(delegate);
		return pluginPath !=null ? pluginPath : "";
	}

}
