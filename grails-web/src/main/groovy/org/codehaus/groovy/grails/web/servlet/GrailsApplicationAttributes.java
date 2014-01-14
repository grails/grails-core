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

import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

/**
 * Defines the names of and methods to retrieve Grails specific request and servlet attributes.
 *
 * @author Graeme Rocher
 */
public interface GrailsApplicationAttributes extends ApplicationAttributes {

    String PATH_TO_VIEWS = "/WEB-INF/grails-app/views";
    String GSP_TEMPLATE_ENGINE = "org.codehaus.groovy.grails.GSP_TEMPLATE_ENGINE";
    String ASYNC_STARTED = "org.codehaus.groovy.grails.ASYNC_STARTED";
    String CONTENT_FORMAT = "org.codehaus.groovy.grails.CONTENT_FORMAT";
    String RESPONSE_FORMAT = "org.codehaus.groovy.grails.RESPONSE_FORMAT";
    String RESPONSE_MIME_TYPE = "org.codehaus.groovy.grails.RESPONSE_MIME_TYPE";
    String REQUEST_FORMATS = "org.codehaus.groovy.grails.REQUEST_FORMATS";
    String RESPONSE_FORMATS = "org.codehaus.groovy.grails.RESPONSE_FORMATS";
    String FLASH_SCOPE = "org.codehaus.groovy.grails.FLASH_SCOPE";
    String PARAMS_OBJECT = "org.codehaus.groovy.grails.PARAMS_OBJECT";
    String CONTROLLER = "org.codehaus.groovy.grails.CONTROLLER";
    String PROPERTY_REGISTRY = "org.codehaus.groovy.grails.PROPERTY_REGISTRY";
    String ERRORS =  "org.codehaus.groovy.grails.ERRORS";
    String MODEL_AND_VIEW = "org.codehaus.groovy.grails.MODEL_AND_VIEW";
    String TEMPLATE_MODEL = "org.codehaus.groovy.grails.TEMPLATE_MODEL";
    String OUT = "org.codehaus.groovy.grails.RESPONSE_OUT";
    String TAG_CACHE = "org.codehaus.groovy.grails.TAG_CACHE";
    String ID_PARAM = "id";
    String GSP_TO_RENDER = "org.codehaus.groovy.grails.GSP_TO_RENDER";
    String GSP_CODEC = "org.codehaus.groovy.grails.GSP_CODEC";
    String WEB_REQUEST = "org.codehaus.groovy.grails.WEB_REQUEST";
    String PAGE_SCOPE = "org.codehaus.groovy.grails.PAGE_SCOPE";
    String GSP_TMP_WRITER = "org.codehaus.groovy.grails.GSP_TMP_WRITER";
    String REQUEST_REDIRECTED_ATTRIBUTE = "org.codehaus.groovy.grails.request_redirected";
    String ACTION_NAME_ATTRIBUTE = "org.codehaus.groovy.grails.ACTION_NAME_ATTRIBUTE";
    String CONTROLLER_NAME_ATTRIBUTE = "org.codehaus.groovy.grails.CONTROLLER_NAME_ATTRIBUTE";
    String CONTROLLER_NAMESPACE_ATTRIBUTE = "org.codehaus.groovy.grails.CONTROLLER_NAMESPACE_ATTRIBUTE";
    String GRAILS_CONTROLLER_CLASS = "org.codehaus.groovy.grails.GRAILS_CONTROLLER_CLASS";
    String APP_URI_ATTRIBUTE = "org.codehaus.groovy.grails.APP_URI_ATTRIBUTE";
    String RENDERING_ERROR_ATTRIBUTE = "org.codehaus.groovy.grails.RENDERING_ERROR_ATTRIBUTE";
    String REDIRECT_ISSUED = "org.codehaus.groovy.grails.REDIRECT_ISSUED";
    String GRAILS_CONTROLLER_CLASS_AVAILABLE = "org.codehaus.groovy.grails.GRAILS_CONTROLLER_CLASS_AVAILABLE";

    /**
     * Retrieves the plugin context path for the current request. The plugin context path is the path
     * used by plugins to reference resources such as javascript, CSS and so forth
     *
     * It is established by evaluating the current controller, if the current controller is plugin provided
     * then it will attempt to evaluate the path based on the plugin the controller came from
     *
     * @return The plugin context path
     */
    String getPluginContextPath(HttpServletRequest request);

    /**
     * @return The controller for the request
     */
    GroovyObject getController(ServletRequest request);

    /**
     * @param request
     * @return The uri of the controller within the request
     */
    String getControllerUri(ServletRequest request);

    /**
     * @param request
     * @return The uri of the application relative to the server root
     */
    String getApplicationUri(ServletRequest request);

    String getTemplateURI(GroovyObject controller, String templateName);

    String getNoSuffixViewURI(GroovyObject controller, String viewName);

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
     * @param templateName
     * @param request
     * @return The uri of a named template for the current controller
     */
    String getTemplateUri(CharSequence templateName, ServletRequest request);

    /**
     * Retrieves the uri of a named view
     *
     * @param viewName The name of the view
     * @param request The request instance
     * @return The name of the view
     */
    String getViewUri(String viewName, HttpServletRequest request);

    /**
     * @param request
     * @return The uri of the action called within the controller
     */
    String getControllerActionUri(ServletRequest request);

    /**
     * @param request
     * @return The errors instance contained within the request
     */
    Errors getErrors(ServletRequest request);

    /**
     * @return Retrieves the shared GSP template engine
     */
    GroovyPagesTemplateEngine getPagesTemplateEngine();

    /**
     * Retrieves a Grails tag library from the request for the named tag in
     * the default namespace GroovyPage.DEFAULT_NAMESPACE
     *
     * @param request the request instance
     * @param response the response instancte
     * @param tagName The name of the tag that contains the tag library
     *
     * @return An instance of the tag library or null if not found
     */
    GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response, String tagName);

    /**
     * Retrieves a Grails tag library from the request for the named tag in a
     * given namespace.
     *
     * @param request the request instance
     * @param response the response instancte
     * @param tagName The name of the tag that contains the tag library
     * @param namespace The namespace of the tag
     *
     * @return An instance of the tag library or null if not found
     */
    GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,
            String tagName, String namespace);

    /**
     * Holds the current response write for the request
     * @return The held response writer
     */
    Writer getOut(HttpServletRequest request);

    /**
     * Sets the current write for the request
     * @param currentRequest The request
     * @param out2 The writer
     */
    void setOut(HttpServletRequest currentRequest, Writer out2);

    GroovyPagesUriService getGroovyPagesUriService();

    MessageSource getMessageSource();
}
