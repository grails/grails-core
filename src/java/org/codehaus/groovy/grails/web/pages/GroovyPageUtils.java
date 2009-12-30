/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.GroovyObject;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Methods to establish template names, paths and so on.
 *
 * Replaced by GroovyPagesUriService / GroovyPagesUriSupport
 *
 * @author Graeme Rocher
 * @since 1.1.1
 * @deprecated
 * @see DefaultGroovyPagesUriService
 * @see GroovyPagesUriSupport
 * 
 *        <p/>
 *        Created: May 1, 2009
 */
public class GroovyPageUtils {
    public static final String PATH_TO_VIEWS = GroovyPagesUriSupport.PATH_TO_VIEWS;

    private static GroovyPagesUriSupport getInstance() {
    	try {
    		GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        	return (GroovyPagesUriSupport)webRequest.getAttributes().getGroovyPagesUriService();
    	} catch (IllegalStateException e) {
    		// returning non cached version, just for backwards compatibility
    		return new GroovyPagesUriSupport();
    	}
    }
    
    /**
     * Obtains a template URI for the given controller instance and template name
     * @param controller The controller instance
     * @param templateName The template name
     * @return The template URI
     * @deprecated
     */
    public static String getTemplateURI(GroovyObject controller, String templateName) {
    	return getInstance().getTemplateURI(controller, templateName);
    }

    /**
    * Obtains a view URI of the given controller and view name
    * @param controller The name of the controller
    * @param viewName The name of the view
    * @return The view URI
    * @deprecated
    */
    public static String getViewURI(GroovyObject controller, String viewName) {
    	return getInstance().getViewURI(controller, viewName);
    }


    /**
    * Obtains a view URI of the given controller and view name without the suffix
    * @param controller The name of the controller
    * @param viewName The name of the view
    * @return The view URI
    * @deprecated
    */
    public static String getNoSuffixViewURI(GroovyObject controller, String viewName) {
    	return getInstance().getNoSuffixViewURI(controller, viewName);
    }

    /**
     * Obtains the URI to a template using the controller name and template name
     * @param controllerName The controller name
     * @param templateName The template name
     * @return The template URI
     * @deprecated
     */
    public static String getTemplateURI(String controllerName, String templateName) {
    	return getInstance().getTemplateURI(controllerName, templateName);
    }
    
    /**
     * Obtains a view URI of the given controller name and view name
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     * @deprecated
     */
    public static String getViewURI(String controllerName, String viewName) {
    	return getInstance().getViewURI(controllerName, viewName);
    }

    /**
     * Obtains a view URI of the given controller name and view name without the suffix
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     * @deprecated
     */
    public static String getNoSuffixViewURI(String controllerName, String viewName) {
        return getInstance().getNoSuffixViewURI(controllerName, viewName);
    }

    /**
     * Obtains a view URI when deployed within the /WEB-INF/grails-app/views context
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     * @deprecated
     */
    public static String getDeployedViewURI(String controllerName, String viewName) {
    	return getInstance().getDeployedViewURI(controllerName, viewName);
    }
}
