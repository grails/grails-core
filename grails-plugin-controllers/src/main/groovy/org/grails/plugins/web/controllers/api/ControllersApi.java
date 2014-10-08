/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.web.controllers.api;

import grails.core.GrailsControllerClass;
import grails.plugins.GrailsPluginManager;
import grails.util.GrailsClassUtils;
import grails.util.GrailsUtil;
import grails.web.mapping.LinkGenerator;
import grails.web.mapping.ResponseRedirector;
import grails.web.mapping.mvc.RedirectEventListener;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import java.util.Collection;
import java.util.Map;

import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.grails.web.api.CommonWebApi;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
/**
 * API for each controller in a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class ControllersApi extends CommonWebApi {

    private static final long serialVersionUID = 1;

    private Collection<RedirectEventListener> redirectListeners;
    private LinkGenerator linkGenerator;
    private RequestDataValueProcessor requestDataValueProcessor;
    private boolean useJessionId = false;

    public ControllersApi() {
        this(null);
    }

    public ControllersApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
    }

    public void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        this.redirectListeners = redirectListeners;
    }

    public void setUseJessionId(boolean useJessionId) {
        this.useJessionId = useJessionId;
    }

    public void setLinkGenerator(LinkGenerator linkGenerator) {
        this.linkGenerator = linkGenerator;
    }

    /**
     * Redirects for the given arguments.
     *
     * @param argMap The arguments
     * @return null
     */
    public Object redirect(Object target, Map argMap) {

        if (argMap.isEmpty()) {
            throw new MissingMethodException("redirect",target.getClass(),new Object[]{ argMap });
        }

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();

        if(target instanceof GroovyObject) {
            GroovyObject controller = (GroovyObject)target;

            // if there are errors add it to the list of errors
            Errors controllerErrors = (Errors)controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY);
            Errors errors = (Errors)argMap.get(ControllerDynamicMethods.ERRORS_PROPERTY);
            if (controllerErrors != null && errors != null) {
                controllerErrors.addAllErrors(errors);
            }
            else {
                controller.setProperty(ControllerDynamicMethods.ERRORS_PROPERTY, errors);
            }
            Object action = argMap.get(GrailsControllerClass.ACTION);
            if (action != null) {
                argMap.put(GrailsControllerClass.ACTION, establishActionName(action,controller));
            }
            if (!argMap.containsKey(GrailsControllerClass.NAMESPACE_PROPERTY)) {
                // this could be made more efficient if we had a reference to the GrailsControllerClass object, which
                // has the namespace property accessible without needing reflection
                argMap.put(GrailsControllerClass.NAMESPACE_PROPERTY, GrailsClassUtils.getStaticFieldValue(controller.getClass(), GrailsControllerClass.NAMESPACE_PROPERTY));
            }
        }

        ResponseRedirector redirector = new ResponseRedirector(getLinkGenerator(webRequest));
        redirector.setRedirectListeners(redirectListeners);
        redirector.setRequestDataValueProcessor(initRequestDataValueProcessor());
        redirector.setUseJessionId(useJessionId);
        redirector.redirect(webRequest.getRequest(), webRequest.getResponse(), argMap);
        return null;
    }

    private LinkGenerator getLinkGenerator(GrailsWebRequest webRequest) {
        if (linkGenerator == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                linkGenerator = applicationContext.getBean("grailsLinkGenerator", LinkGenerator.class);
            }
        }

        return linkGenerator;
    }

    /*
     * Figures out the action name from the specified action reference (either a string or closure)
     */
    private String establishActionName(Object actionRef, Object target) {
        String actionName = null;
        if (actionRef instanceof String) {
            actionName = (String)actionRef;
        }
        else if (actionRef instanceof CharSequence) {
            actionName = actionRef.toString();
        }
        else if (actionRef instanceof Closure) {
            GrailsUtil.deprecated("Using a closure reference in the 'action' argument of the 'redirect' method is deprecated. Please change to use a String.");
            actionName = GrailsClassUtils.findPropertyNameForValue(target, actionRef);
        }
        return actionName;
    }

    /**
     * getter to obtain RequestDataValueProcessor from
     */
    private RequestDataValueProcessor initRequestDataValueProcessor() {
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        ApplicationContext applicationContext = webRequest.getApplicationContext();
        if (requestDataValueProcessor == null && applicationContext.containsBean("requestDataValueProcessor")) {
            requestDataValueProcessor = applicationContext.getBean("requestDataValueProcessor", RequestDataValueProcessor.class);
        }
        return requestDataValueProcessor;
    }

}
