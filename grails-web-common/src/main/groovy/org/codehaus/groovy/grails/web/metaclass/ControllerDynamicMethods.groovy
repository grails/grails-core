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
package org.codehaus.groovy.grails.web.metaclass

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Holds constants that refer to the names of dynamic methods and properties within controllers
 *
 * @author Graeme Rocher
 * @since Oct 24, 2005
 */
class ControllerDynamicMethods{

    public static final String REQUEST_PROPERTY = "request";
    public static final String SERVLET_CONTEXT = "servletContext";
    public static final String FLASH_SCOPE_PROPERTY = "flash";
    public static final String GRAILS_ATTRIBUTES = "grailsAttributes";
    public static final String GRAILS_APPLICATION = "grailsApplication";
    public static final String RESPONSE_PROPERTY = "response";
    public static final String RENDER_VIEW_PROPERTY = "renderView";
    public static final String ERRORS_PROPERTY = "errors";
    public static final String HAS_ERRORS_METHOD = "hasErrors";
    public static final String MODEL_AND_VIEW_PROPERTY = "modelAndView";
    public static final String ACTION_URI_PROPERTY = "actionUri";
    public static final String CONTROLLER_URI_PROPERTY = "controllerUri";
    public static final String ACTION_NAME_PROPERTY = "actionName";
    public static final String CONTROLLER_NAME_PROPERTY = "controllerName";
    public static final String GET_VIEW_URI = "getViewUri";
    public static final String GET_TEMPLATE_URI = "getTemplateUri";

    /**
     * This creates the difference dynamic methods and properties on the controllers. Most methods
     * are implemented by looking up the current request from the RequestContextHolder (RCH)
     */
    static void registerCommonWebProperties(MetaClass mc, GrailsApplication application) {
        def paramsObject = {-> RequestContextHolder.currentRequestAttributes().params }
        def flashObject = {-> RequestContextHolder.currentRequestAttributes().flashScope }
        def sessionObject = {-> RequestContextHolder.currentRequestAttributes().session }
        def requestObject = {-> RequestContextHolder.currentRequestAttributes().currentRequest }
        def responseObject = {-> RequestContextHolder.currentRequestAttributes().currentResponse }
        def servletContextObject = {-> RequestContextHolder.currentRequestAttributes().servletContext }
        def grailsAttrsObject = {-> RequestContextHolder.currentRequestAttributes().attributes }

        // the params object
        mc.getParams = paramsObject
        // the flash object
        mc.getFlash = flashObject
        // the session object
        mc.getSession = sessionObject
        // the request object
        mc.getRequest = requestObject
        // the servlet context
        mc.getServletContext = servletContextObject
        // the response object
        mc.getResponse = responseObject
        // The GrailsApplicationAttributes object
        mc.getGrailsAttributes = grailsAttrsObject
        // The GrailsApplication object
        mc.getGrailsApplication = {-> RequestContextHolder.currentRequestAttributes().attributes.grailsApplication }

        mc.getActionName = {-> RequestContextHolder.currentRequestAttributes().actionName }
        mc.getControllerName = {-> RequestContextHolder.currentRequestAttributes().controllerName }
        mc.getWebRequest = {-> RequestContextHolder.currentRequestAttributes() }
    }
}
