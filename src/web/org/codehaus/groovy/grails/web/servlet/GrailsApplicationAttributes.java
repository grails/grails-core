package org.codehaus.groovy.grails.web.servlet;

import org.springframework.validation.Errors;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.web.metaclass.GrailsParameterMap;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import groovy.lang.GroovyObject;

import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An interface defining the names of and methods to retrieve Grails specific request and servlet attributes
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
public interface GrailsApplicationAttributes {

	String GSP_FILE_EXTENSION = ".gsp";
	String PATH_TO_VIEWS = "/WEB-INF/grails-app/views";
    String GSP_TEMPLATE_ENGINE = "org.codehaus.groovy.grails.GSP_TEMPLATE_ENGINE";
    String APPLICATION_CONTEXT = "org.codehaus.groovy.grails.APPLICATION_CONTEXT";
    String FLASH_SCOPE = "org.codehaus.groovy.grails.FLASH_SCOPE";
    String PARAMS_OBJECT = "org.codehaus.groovy.grails.PARAMS_OBJECT";
    String CONTROLLER = "org.codehaus.groovy.grails.CONTROLLER";
    String ERRORS =  "org.codehaus.groovy.grails.ERRORS";
    String TAG_CACHE = "org.codehaus.groovy.grails.TAG_CACHE";
    String ID_PARAM = "id";
    String PARENT_APPLICATION_CONTEXT = "org.codehaus.groovy.grails.PARENT_APPLICATION_CONTEXT";
	String GSP_TO_RENDER = "org.codehaus.groovy.grails.GSP_TO_RENDER";
	String REQUEST_SCOPE_ID = "org.codehaus.groovy.grails.GRAILS_APPLICATION_ATTRIBUTES";
	String PLUGIN_MANAGER = "org.codehaus.groovy.grails.GRAILS_PLUGIN_MANAGER";

    /**
     * @return The application context for servlet
     */
    ApplicationContext getApplicationContext();

    /**
     * @return The controller for the request
     */
    GroovyObject getController(ServletRequest request);

    /**
     *
     * @param request
     * @return The uri of the controller within the request
     */
    String getControllerUri(ServletRequest request);

    /**
     *
     * @param request
     * @return The uri of the application relative to the server root
     */
    String getApplicationUri(ServletRequest request);

    /**
     * Retrieves the servlet context instance
     * @return The servlet context instance
     */
    ServletContext getServletContext();

    /**
     * Retrieves the flash scope instance for the given requeste
     * @param request
     * @return The FlashScope instance
     */
    FlashScope getFlashScope(ServletRequest request);
    
    /**
     * Returns the params object instance for the request
     * 
     * @param request The request
     * @return The params object
     */
    GrailsParameterMap getParamsMap(ServletRequest request);
    /**
     *
     * @param templateName
     * @param request
     * @return The uri of a named template for the current controller
     */
    String getTemplateUri(String templateName, ServletRequest request);
    
	/**
	 * Retrieves the uri of a named view
	 * 
	 * @param viewName The name of the view
	 * @param request The request instance
	 * @return The name of the view
	 */
	String getViewUri(String viewName, HttpServletRequest request);    

    /**
     *
     * @param request
     * @return The uri of the action called within the controller
     */
    String getControllerActionUri(ServletRequest request);

    /**
     *
     * @param request
     * @return The errors instance contained within the request
     */
    Errors getErrors(ServletRequest request);

    /**
     *
     * @return Retrieves the shared GSP template engine
     */
    GroovyPagesTemplateEngine getPagesTemplateEngine();

    /**
     *
     * @return Retrieves the grails application instance
     */
    GrailsApplication getGrailsApplication();

    /**
     * Retrieves a Grails tag library from the request for the named tag
     * @param request the request instance
     * @param response the response instancte
     * @param tagName The name of the tag that contains the tag library
     * 
     * @return An instance of the tag library or null if not found
     */
	GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,String tagName);


}
