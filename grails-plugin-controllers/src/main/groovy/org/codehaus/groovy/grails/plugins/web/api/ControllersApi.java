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
package org.codehaus.groovy.grails.plugins.web.api;

import grails.util.CollectionUtils;
import grails.util.Environment;
import grails.util.GrailsNameUtils;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.Predicate;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.compiler.web.ControllerActionTransformer;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.binding.DataBindingUtils;
import org.codehaus.groovy.grails.web.controllers.ControllerExceptionHandlerMetaData;
import org.codehaus.groovy.grails.web.mapping.LinkGenerator;
import org.codehaus.groovy.grails.web.metaclass.ChainMethod;
import org.codehaus.groovy.grails.web.metaclass.ForwardMethod;
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.RenderDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.WithFormMethod;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.RedirectEventListener;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.CannotRedirectException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.grails.databinding.CollectionDataBindingSource;
import org.grails.databinding.DataBindingSource;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;

/**
 * API for each controller in a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class ControllersApi extends CommonWebApi {

    private static final String INCLUDE_MAP_KEY = "include";
    private static final String EXCLUDE_MAP_KEY = "exclude";

    private static final long serialVersionUID = 1;

    protected static final String RENDER_METHOD_NAME = "render";
    protected static final String BIND_DATA_METHOD = "bindData";
    protected static final String SLASH = "/";
    protected transient RedirectDynamicMethod redirect;
    protected transient RenderDynamicMethod render;
    protected transient WithFormMethod withFormMethod;
    protected transient ForwardMethod forwardMethod;

    public ControllersApi() {
        this(null);
    }

    public ControllersApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
        redirect = new RedirectDynamicMethod();
        render = new RenderDynamicMethod();
        withFormMethod = new WithFormMethod();
        forwardMethod = new ForwardMethod();
    }

    public static ApplicationContext getStaticApplicationContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof GrailsWebRequest)) {
            return null;
        }

        return ((GrailsWebRequest)requestAttributes).getApplicationContext();
    }

    public void setGspEncoding(String gspEncoding) {
        render.setGspEncoding(gspEncoding);
    }

    public void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        redirect.setRedirectListeners(redirectListeners);
    }

    public void setUseJessionId(boolean useJessionId) {
        redirect.setUseJessionId(useJessionId);
    }

    public void setLinkGenerator(LinkGenerator linkGenerator) {
        redirect.setLinkGenerator(linkGenerator);
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
                webRequest.setControllerName(GrailsNameUtils.getLogicalPropertyName(
                        instance.getClass().getName(), ControllerArtefactHandler.TYPE));
            }
        }
    }

    /**
     * Returns the URI of the currently executing action
     *
     * @return The action URI
     */
    public String getActionUri(Object instance) {
        return SLASH + getControllerName(instance) + SLASH + getActionName(instance);
    }

    /**
     * Returns the URI of the currently executing controller
     * @return The controller URI
     */
    public String getControllerUri(Object instance) {
        return SLASH + getControllerName(instance);
    }

    /**
     * Obtains a URI of a template by name
     *
     * @param name The name of the template
     * @return The template URI
     */
    public String getTemplateUri(Object instance, String name) {
        return getGrailsAttributes(instance).getTemplateUri(name, getRequest(instance));
    }

    /**
     * Obtains a URI of a view by name
     *
     * @param name The name of the view
     * @return The template URI
     */
    public String getViewUri(Object instance, String name) {
        return getGrailsAttributes(instance).getViewUri(name, getRequest(instance));
    }

    /**
     * Sets the errors instance of the current controller
     *
     * @param errors The error instance
     */
    public void setErrors(Object instance, Errors errors) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0);
    }

    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    public Errors getErrors(Object instance) {
        return (Errors)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0);
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    public void setModelAndView(Object instance, ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0);
    }

    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    public ModelAndView getModelAndView(Object instance) {
        return (ModelAndView)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
    }

    /**
     * Obtains the chain model which is used to chain request attributes from one request to the next via flash scope
     * @return The chainModel
     */
    public Map getChainModel(Object instance) {
        return (Map)getFlash(instance).get("chainModel");
    }

    /**
     * Return true if there are an errors
     * @return true if there are errors
     */
    public boolean hasErrors(Object instance) {
        final Errors errors = getErrors(instance);
        return errors != null && errors.hasErrors();
    }

    /**
     * Redirects for the given arguments.
     *
     * @param args The arguments
     * @return null
     */
    public Object redirect(Object instance,Map args) {
        return redirect.invoke(instance, "redirect", new Object[]{ args });
    }

    /**
     * Redirects for the given arguments.
     *
     * @param object A domain class
     * @return null
     */
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

    /**
     * Invokes the chain method for the given arguments
     *
     * @param instance The instance
     * @param args The arguments
     * @return Result of the redirect call
     */
    public Object chain(Object instance, Map args) {
        return ChainMethod.invoke(instance, args);
    }

    // the render method
    public Object render(Object instance, Object o) {
        return invokeRender(instance, DefaultGroovyMethods.inspect(o));
    }

    public Object render(Object instance, String txt) {
        return invokeRender(instance, txt);
    }

    public Object render(Object instance, Map args) {
        return invokeRender(instance, args);
    }

    public Object render(Object instance, Closure c) {
        return invokeRender(instance, c);
    }

    public Object render(Object instance, Map args, Closure c) {
        return invokeRender(instance, args, c);
    }

    protected Object invokeRender(Object instance, Object... args) {
        return render.invoke(instance, RENDER_METHOD_NAME, args);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, final List excludes) {
        return bindData(instance, target, bindingSource, CollectionUtils.newMap(EXCLUDE_MAP_KEY, excludes), null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, final List excludes, String filter) {
        return bindData(instance, target, bindingSource, CollectionUtils.newMap(EXCLUDE_MAP_KEY, excludes), filter);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, Map includeExclude) {
        return bindData(instance, target, bindingSource, includeExclude, null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, String filter) {
        return bindData(instance, target, bindingSource, Collections.EMPTY_MAP, filter);
    }

    public Object bindData(Object instance, Object target, Object bindingSource) {
        return bindData(instance, target, bindingSource, Collections.EMPTY_MAP, null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, Map includeExclude, String filter) {
        List include = convertToListIfString(includeExclude.get(INCLUDE_MAP_KEY));
        List exclude = convertToListIfString(includeExclude.get(EXCLUDE_MAP_KEY));
        DataBindingUtils.bindObjectToInstance(target, bindingSource, include, exclude, filter);
        return target;
    }

    public <T> void bindData(Object instance, Class<T> targetType, Collection<T> collectionToPopulate, ServletRequest request) throws Exception {
        DataBindingUtils.bindToCollection(targetType, collectionToPopulate, request);
    }

    public <T> void bindData(Object instance, Class<T> targetType, Collection<T> collectionToPopulate, CollectionDataBindingSource collectionBindingSource) throws Exception {
        DataBindingUtils.bindToCollection(targetType, collectionToPopulate, collectionBindingSource);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List convertToListIfString(Object o) {
        if (o instanceof String) {
            List list = new ArrayList();
            list.add(o);
            o = list;
        }
        return (List) o;
    }

    /**
     * Sets a response header for the given name and value
     *
     * @param instance The instance
     * @param headerName The header name
     * @param headerValue The header value
     */
    public void header(Object instance, String headerName, Object headerValue) {
        if (headerValue == null) {
            return;
        }

        final HttpServletResponse response = getResponse(instance);
        if (response != null) {
            response.setHeader(headerName, headerValue.toString());
        }
    }

    /**
     * Used the synchronizer token pattern to avoid duplicate form submissions
     *
     * @param instance The instance
     * @param callable The closure to execute
     * @return The result of the closure execution
     */
    public Object withForm(Object instance, Closure callable) {
        return withFormMethod.withForm(getWebRequest(instance), callable);
    }

    /**
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param instance The instance
     * @param params The parameters
     * @return The forwarded URL
     */
    public String forward(Object instance, Map params) {
        return forwardMethod.forward(getRequest(instance), getResponse(instance), params);
    }

    /**
     * Initializes a command object.
     *
     * If type is a domain class and the request body or parameters include an id, the id is used to retrieve
     * the command object instance from the database, otherwise the no-arg constructor on type is invoke.  If
     * an attempt is made to retrieve the command object instance from the database and no corresponding
     * record is found, null is returned.
     *
     * The command object is then subjected to data binding and dependency injection before being returned.
     *
     *
     * @param controllerInstance The controller instance
     * @param type The type of the command object
     * @return the initialized command object or null if the command object is a domain class, the body or
     * parameters included an id and no corresponding record was found in the database.
     */
    public Object initializeCommandObject(final Object controllerInstance, final Class type) throws Exception {
        final HttpServletRequest request = getRequest(controllerInstance);
        final DataBindingSource dataBindingSource = DataBindingUtils.createDataBindingSource(getGrailsApplication(controllerInstance), type, request);
        final DataBindingSource commandObjectBindingSource = WebMetaUtils.getCommandObjectBindingSource(type, dataBindingSource);
        final Object commandObjectInstance;
        Object entityIdentifierValue = null;
        if(DomainClassArtefactHandler.isDomainClass(type)) {
            entityIdentifierValue = commandObjectBindingSource.getIdentifierValue();
            if(entityIdentifierValue == null) {
                final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
                entityIdentifierValue = webRequest != null ? webRequest.getParams().getIdentifier() : null;
            }
        }
        if(entityIdentifierValue != null) {
            commandObjectInstance = InvokerHelper.invokeStaticMethod(type, "get", entityIdentifierValue);
        } else {
            commandObjectInstance = type.newInstance();
        }

        if(commandObjectInstance != null) {
            final boolean shouldDoDataBinding;

            if(entityIdentifierValue != null) {
                final HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());
                switch(requestMethod) {
                    case PATCH:
                    case POST:
                    case PUT:
                        shouldDoDataBinding = true;
                        break;
                    default:
                        shouldDoDataBinding = false;
                }
            } else {
                shouldDoDataBinding = true;
            }

            if(shouldDoDataBinding) {
                bindData(controllerInstance, commandObjectInstance, commandObjectBindingSource, Collections.EMPTY_MAP, null);
            }

            final ApplicationContext applicationContext = getApplicationContext(controllerInstance);
            final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
            autowireCapableBeanFactory.autowireBeanProperties(commandObjectInstance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
        }
        return commandObjectInstance;
    }
    
    @SuppressWarnings("unchecked")
    public Method getExceptionHandlerMethodFor(final Object controllerInstance, final Class<? extends Exception> exceptionType) throws Exception {
        if(!Exception.class.isAssignableFrom(exceptionType)) {
            throw new IllegalArgumentException("exceptionType [" + exceptionType.getName() + "] argument must be Exception or a subclass of Exception");
        }
        Method handlerMethod = null;
        final List<ControllerExceptionHandlerMetaData> exceptionHandlerMetaDataInstances = (List<ControllerExceptionHandlerMetaData>) GrailsClassUtils.getStaticFieldValue(controllerInstance.getClass(), ControllerActionTransformer.EXCEPTION_HANDLER_META_DATA_FIELD_NAME);
        if(exceptionHandlerMetaDataInstances != null && exceptionHandlerMetaDataInstances.size() > 0) {
            
            // find all of the handler methods which could accept this exception type
            final List<ControllerExceptionHandlerMetaData> matches = (List<ControllerExceptionHandlerMetaData>) org.apache.commons.collections.CollectionUtils.select(exceptionHandlerMetaDataInstances, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    ControllerExceptionHandlerMetaData md = (ControllerExceptionHandlerMetaData) object;
                    return md.getExceptionType().isAssignableFrom(exceptionType);
                }
            });
            
            
            if(matches.size() > 0) {
                ControllerExceptionHandlerMetaData theOne = matches.get(0);
                
                // if there are more than 1, find the one that is farthest down the inheritance hierarchy
                for(int i = 1; i < matches.size(); i++) {
                    final ControllerExceptionHandlerMetaData nextMatch = matches.get(i);
                    if(theOne.getExceptionType().isAssignableFrom(nextMatch.getExceptionType())) {
                        theOne = nextMatch;
                    }
                }
                handlerMethod = controllerInstance.getClass().getMethod(theOne.getMethodName(), theOne.getExceptionType());
            }
        }
        
        return handlerMethod;
    }
}
