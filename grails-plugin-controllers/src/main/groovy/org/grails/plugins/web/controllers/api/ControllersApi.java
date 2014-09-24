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

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.DEFAULT_ENCODING;
import grails.core.GrailsControllerClass;
import grails.core.GrailsDomainClassProperty;
import grails.plugins.GrailsPluginManager;
import grails.util.Environment;
import grails.util.GrailsClassUtils;
import grails.util.GrailsNameUtils;
import grails.util.GrailsUtil;
import grails.web.mapping.LinkGenerator;
import grails.web.mapping.ResponseRedirector;
import grails.web.mapping.mvc.RedirectEventListener;
import grails.web.mapping.mvc.exceptions.CannotRedirectException;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.web.api.CommonWebApi;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.validation.Errors;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.request.RequestAttributes;
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

    protected static final String RENDER_METHOD_NAME = "render";
    protected static final String BIND_DATA_METHOD = "bindData";
    private Collection<RedirectEventListener> redirectListeners;
    private LinkGenerator linkGenerator;
    private RequestDataValueProcessor requestDataValueProcessor;
    private boolean useJessionId = false;
    private String gspEncoding = DEFAULT_ENCODING;

    public ControllersApi() {
        this(null);
    }

    public ControllersApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
    }

    public static ApplicationContext getStaticApplicationContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof GrailsWebRequest)) {
            return ContextLoader.getCurrentWebApplicationContext();
        }
        return ((GrailsWebRequest)requestAttributes).getApplicationContext();
    }

    public void setGspEncoding(String gspEncoding) {
       this.gspEncoding = gspEncoding;
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
     * Constructor used by controllers
     *
     * @param instance The instance
     */
    public static void initialize(Object instance) {
        ApplicationContext applicationContext = getStaticApplicationContext();
        if (applicationContext == null) {
            return;
        }

        applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

        if (Environment.getCurrent() == Environment.TEST) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if (webRequest != null) {
                // GRAILS-10929 - If the class name ends with $$..., then it's a proxy and we want to remove that from the name
				String className = instance.getClass().getName().replaceAll("\\$\\$.*$", "");
                webRequest.setControllerName(GrailsNameUtils.getLogicalPropertyName(className, ControllerArtefactHandler.TYPE));
            }
        }
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

    /**
     * Redirects for the given arguments.
     *
     * @param object A domain class
     * @return null
     */
    @SuppressWarnings("unchecked")
    public Object redirect(Object instance,Object object) {
        if(object != null) {

            Class<?> objectClass = object.getClass();
            boolean isDomain = DomainClassArtefactHandler.isDomainClass(objectClass) && object instanceof GroovyObject;
            if(isDomain) {

                Object id = ((GroovyObject)object).getProperty(GrailsDomainClassProperty.IDENTITY);
                if(id != null) {
                    Map args = new HashMap();
                    args.put(LinkGenerator.ATTRIBUTE_RESOURCE, object);
                    args.put(LinkGenerator.ATTRIBUTE_METHOD, HttpMethod.GET.toString());
                    return redirect(instance, args);
                }
            }
        }
        throw new CannotRedirectException("Cannot redirect for object ["+object+"] it is not a domain or has no identifier. Use an explicit redirect instead ");
    }

    // the render method

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
