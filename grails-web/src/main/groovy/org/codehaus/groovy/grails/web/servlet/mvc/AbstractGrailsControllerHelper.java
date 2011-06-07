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

import grails.util.GrailsUtil;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.util.Proxy;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.collections.map.CompositeMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.NoViewNameDefinedException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.UnknownControllerException;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Processes Grails controller requests and responses.
 *
 * @author Graeme Rocher
 * @author Stephane Maldini
 * @since 1.4
 */
public abstract class AbstractGrailsControllerHelper implements ApplicationContextAware, ServletContextAware, GrailsApplicationAware {

    protected GrailsApplication application;
    protected ApplicationContext applicationContext;
    protected ServletContext servletContext;
    protected GrailsApplicationAttributes grailsAttributes;

    private static final Log LOG = LogFactory.getLog(AbstractGrailsControllerHelper.class);
    private static final String PROPERTY_CHAIN_MODEL = "chainModel";
    private static final String FORWARD_CALLED = "org.codehaus.groovy.grails.FORWARD_CALLED";


    public ServletContext getServletContext() {
        return servletContext;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerClassByName(java.lang.String)
     */
    public GrailsControllerClass getControllerClassByName(String name) {
        return (GrailsControllerClass) application.getArtefact(
                ControllerArtefactHandler.TYPE, name);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerClassByURI(java.lang.String)
     */
    public GrailsControllerClass getControllerClassByURI(String uri) {
        return (GrailsControllerClass) application.getArtefactForFeature(
                ControllerArtefactHandler.TYPE, uri);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerInstance(org.codehaus.groovy.grails.commons.GrailsControllerClass)
     */
    public GroovyObject getControllerInstance(GrailsControllerClass controllerClass) {
        return (GroovyObject)applicationContext.getBean(controllerClass.getFullName());
    }

    /**
     * If in Proxy's are used in the Groovy context, unproxy (is that a word?) them by setting
     * the adaptee as the value in the map so that they can be used in non-groovy view technologies
     *
     * @param model The model as a map
     */
    private void removeProxiesFromModelObjects(Map<Object, Object> model) {
        for (Map.Entry<Object, Object> entry : model.entrySet()) {
            if (entry.getValue() instanceof Proxy) {
                entry.setValue(((Proxy)entry.getValue()).getAdaptee());
            }
        }
    }

    public ModelAndView handleURI(String uri, GrailsWebRequest request) {
        return handleURI(uri, request, Collections.EMPTY_MAP);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#handleURI(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
     */
    @SuppressWarnings("rawtypes")
    public ModelAndView handleURI(String uri, GrailsWebRequest grailsWebRequest, Map params) {
        Assert.notNull(uri, "Controller URI [" + uri + "] cannot be null!");

        HttpServletRequest request = grailsWebRequest.getCurrentRequest();
        HttpServletResponse response = grailsWebRequest.getCurrentResponse();

        if (uri.endsWith("/")) {
            uri = uri.substring(0,uri.length() - 1);
        }

        // Step 2: lookup the controller in the application.
        GrailsControllerClass controllerClass = getControllerClassByURI(uri);

        if (controllerClass == null) {
            throw new UnknownControllerException("No controller found for URI [" + uri + "]!");
        }

        String actionName = controllerClass.getClosurePropertyName(uri);
        grailsWebRequest.setActionName(actionName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request for controller action ["+actionName+"]");
        }
        // Step 3: load controller from application context.
        GroovyObject controller = getControllerInstance(controllerClass);

        if (!controllerClass.isHttpMethodAllowedForAction(controller, request.getMethod(), actionName)) {
            try {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return null;
            }
            catch (IOException e) {
                throw new ControllerExecutionException("I/O error sending 403 error",e);
            }
        }

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller);

        // Step 4: Set grails attributes in request scope
        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID,grailsAttributes);

        // Step 5: get the view name for this URI.
        String viewName = controllerClass.getViewByURI(uri);

        boolean executeAction = invokeBeforeInterceptor(controller, actionName, controllerClass);
        // if the interceptor returned false don't execute the action
        if (!executeAction) {
            return null;
        }

        ModelAndView mv = executeAction(controller, actionName, viewName, grailsWebRequest, params);

        boolean returnModelAndView = invokeAfterInterceptor(controllerClass, controller, actionName, mv) && !response.isCommitted();
        return returnModelAndView ? mv : null;
    }

    protected abstract Object retrieveAction(GroovyObject controller, @SuppressWarnings("hiding") String actionName,
            HttpServletResponse response);

    /**
     * Invokes the action defined by the webRequest for the given arguments.
     *
     * @param controller The controller instance
     * @param actionName The current action
     * @param viewName The name of the view to delegate to if necessary
     * @param webRequest the current web Request
     * @param params A map of parameters
     * @return A Spring ModelAndView instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected ModelAndView executeAction(GroovyObject controller,
            String actionName,
            String viewName, GrailsWebRequest webRequest, Map params) {
        // Step 5a: Check if there is a before interceptor if there is execute it
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        HttpServletResponse response = webRequest.getCurrentResponse();
        HttpServletRequest request = webRequest.getCurrentRequest();
        try {
            // Step 6: get action from implementation
            Object action = retrieveAction(controller, actionName, response);

                        // Step 7: process the action
            Object returnValue = null;
            try {
                returnValue = handleAction(controller,action,request,response,params);
            }
            catch (Throwable t) {
                String pluginName = GrailsPluginUtils.getPluginName(controller.getClass());
                pluginName = pluginName != null ? "in plugin ["+pluginName+"]" : "";
                throw new ControllerExecutionException("Executing action [" + actionName +
                        "] of controller [" + controller.getClass().getName() + "] " +
                        pluginName + " caused exception: " + t.getMessage(), t);
            }

            // Step 8: determine return value type and handle accordingly
            Map chainModel = initChainModel(request);

            if (response.isCommitted() || request.getAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED) != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Response has been redirected, returning null model and view");
                }
                return null;
            }

            TokenResponseHandler handler = (TokenResponseHandler) request.getAttribute(TokenResponseHandler.KEY);
            if (handler != null && !handler.wasInvoked() && handler.wasInvalidToken()) {
                String uri = (String) request.getAttribute(SynchronizerTokensHolder.TOKEN_URI);
                if (uri == null) {
                    uri = WebUtils.getForwardURI(request);
                }
                try {
                    FlashScope flashScope = webRequest.getFlashScope();
                    flashScope.put("invalidToken", request.getParameter(SynchronizerTokensHolder.TOKEN_KEY));
                    response.sendRedirect(uri);
                    return null;
                }
                catch (IOException e) {
                    throw new ControllerExecutionException("I/O error sending redirect to URI: " + uri,e);
                }
            }
            else if (request.getAttribute(FORWARD_CALLED) == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Action ["+actionName+"] executed with result ["+returnValue+"] and view name ["+viewName+"]");
                }
                ModelAndView mv = handleActionResponse(controller,returnValue, webRequest, chainModel, actionName,  viewName);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Action ["+actionName+"] handled, created Spring model and view ["+mv+"]");
                }
                return mv;
            }
            else {
                return null;
            }
        }
        finally {
            try {
                Thread.currentThread().setContextClassLoader(cl);
            }
            catch (AccessControlException e) {
                // not allowed by container, probably related to WAR deployment on AppEngine. Proceed.
            }
        }
    }

    private boolean invokeBeforeInterceptor(GroovyObject controller, String actionName, GrailsControllerClass controllerClass) {
        boolean executeAction = true;
        if (controllerClass.isInterceptedBefore(controller, actionName)) {
            Closure<?> beforeInterceptor = controllerClass.getBeforeInterceptor(controller);
            if (beforeInterceptor!= null) {
                if (beforeInterceptor.getDelegate() != controller) {
                    beforeInterceptor.setDelegate(controller);
                    beforeInterceptor.setResolveStrategy(Closure.DELEGATE_FIRST);
                }
                Object interceptorResult = beforeInterceptor.call();
                if (interceptorResult instanceof Boolean) {
                    executeAction = ((Boolean)interceptorResult).booleanValue();
                }
            }
        }
        return executeAction;
    }

    @SuppressWarnings("rawtypes")
    private boolean invokeAfterInterceptor(GrailsControllerClass controllerClass,
            GroovyObject controller, String actionName, ModelAndView mv) {
        // Step 9: Check if there is after interceptor
        Object interceptorResult = null;
        if (controllerClass.isInterceptedAfter(controller,actionName)) {
            Closure afterInterceptor = controllerClass.getAfterInterceptor(controller);
            if (afterInterceptor.getDelegate() != controller) {
                afterInterceptor.setDelegate(controller);
                afterInterceptor.setResolveStrategy(Closure.DELEGATE_FIRST);
            }
            Map model = new HashMap();
            if (mv != null) {
                model =    mv.getModel() != null ? mv.getModel() : new HashMap();
            }
            switch(afterInterceptor.getMaximumNumberOfParameters()) {
                case 1:
                    interceptorResult = afterInterceptor.call(new Object[]{ model });
                    break;
                case 2:
                    interceptorResult = afterInterceptor.call(new Object[]{ model, mv });
                    break;
                default:
                    throw new ControllerExecutionException("AfterInterceptor closure must accept one or two parameters");
            }
        }
        return !(interceptorResult != null && interceptorResult instanceof Boolean) ||
            ((Boolean) interceptorResult).booleanValue();
    }

    public GrailsApplicationAttributes getGrailsAttributes() {
        return grailsAttributes;
    }

    public Object handleAction(GroovyObject controller, Object action, HttpServletRequest request,
            HttpServletResponse response) {
        return handleAction(controller,action,request,response,Collections.EMPTY_MAP);
    }

    protected abstract Object invoke(GroovyObject controller, Object action);

    @SuppressWarnings("rawtypes")
    public Object handleAction(GroovyObject controller, Object action, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response, Map params) {
        GrailsParameterMap paramsMap = (GrailsParameterMap)controller.getProperty("params");
        // if there are additional params add them to the params dynamic property
        if (params != null && !params.isEmpty()) {
            paramsMap.putAll(params);
        }
        Object returnValue = action != null ? invoke(controller, action) : null;

        // Step 8: add any errors to the request
        request.setAttribute(GrailsApplicationAttributes.ERRORS, controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY));

        return returnValue;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#handleActionResponse(org.codehaus.groovy.grails.commons.GrailsControllerClass, java.lang.Object, java.lang.String, java.lang.String)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ModelAndView handleActionResponse(GroovyObject controller, Object returnValue, GrailsWebRequest webRequest, Map chainModel, String closurePropertyName, String viewName) {
        boolean viewNameBlank = (viewName == null || viewName.length() == 0);
        // reset the metaclass
        ModelAndView explicitModelAndView = (ModelAndView)controller.getProperty(ControllerDynamicMethods.MODEL_AND_VIEW_PROPERTY);

        if (!webRequest.isRenderView()) {
            return null;
        }

        if (explicitModelAndView != null) {
            return explicitModelAndView;
        }

        if (returnValue == null) {
            if (viewNameBlank) {
                return null;
            }

            Map model;
            if (!chainModel.isEmpty()) {
                model = new CompositeMap(chainModel, new BeanMap(controller));
            }
            else {
                model = new BeanMap(controller);
            }
            return new ModelAndView(viewName, model);
        }

        if (returnValue instanceof Map) {
            // remove any Proxy wrappers and set the adaptee as the value
            Map finalModel = new LinkedHashMap();
            if (!chainModel.isEmpty()) {
                finalModel.putAll(chainModel);
            }
            Map returnModel = (Map)returnValue;
            finalModel.putAll(returnModel);

            removeProxiesFromModelObjects(finalModel);
            return new ModelAndView(viewName, finalModel);
        }

        if (returnValue instanceof ModelAndView) {
            ModelAndView modelAndView = (ModelAndView)returnValue;

            // remove any Proxy wrappers and set the adaptee as the value
            Map modelMap = modelAndView.getModel();
            removeProxiesFromModelObjects(modelMap);

            if (!chainModel.isEmpty()) {
                modelAndView.addAllObjects(chainModel);
            }

            if (modelAndView.getView() == null && modelAndView.getViewName() == null) {
                if (viewNameBlank) {
                    throw new NoViewNameDefinedException("ModelAndView instance returned by and no view name defined by nor for closure on property [" + closurePropertyName + "] in controller [" + controller.getClass() + "]!");
                }

                modelAndView.setViewName(viewName);
            }
            return modelAndView;
        }

        Map model;
        if (!chainModel.isEmpty()) {
            model = new CompositeMap(chainModel, new BeanMap(controller));
        }
        else {
            model = new BeanMap(controller);
        }
        return new ModelAndView(viewName, model);
    }

    @SuppressWarnings("rawtypes")
    private Map initChainModel(HttpServletRequest request) {
        FlashScope fs = grailsAttributes.getFlashScope(request);
        if (fs.containsKey(PROPERTY_CHAIN_MODEL)) {
            Map chainModel = (Map)fs.get(PROPERTY_CHAIN_MODEL);
            if (chainModel == null) {
                chainModel = Collections.EMPTY_MAP;
            }
            return chainModel;
        }
        return Collections.EMPTY_MAP;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        this.grailsAttributes = new DefaultGrailsApplicationAttributes(servletContext);
    }

    public void setGrailsApplication(GrailsApplication application) {
        this.application = application;
    }
}
