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

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingPropertyException;
import groovy.util.Proxy;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.collections.map.CompositeMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.metaclass.ForwardMethod;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.NoViewNameDefinedException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.UnknownControllerException;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>This is a helper class that does the main job of dealing with Grails web requests
 *
 * @author Graeme Rocher
 * @since 0.1
 * 
 * Created: 12-Jan-2006
 */
public class SimpleGrailsControllerHelper implements GrailsControllerHelper {

    

    private GrailsApplication application;
    private ApplicationContext applicationContext;
    private Map chainModel = Collections.EMPTY_MAP;
    private ServletContext servletContext;
    private GrailsApplicationAttributes grailsAttributes;
	private GrailsWebRequest webRequest;
    
    private static final Log LOG = LogFactory.getLog(SimpleGrailsControllerHelper.class);
    private static final char SLASH = '/';

    private static final String PROPERTY_CHAIN_MODEL = "chainModel";
    private String id;
    private String controllerName;
    private String actionName;
    private String controllerActionURI;

    public SimpleGrailsControllerHelper(GrailsApplication application, ApplicationContext context, ServletContext servletContext) {
        super();
        this.application = application;
        this.applicationContext = context;
        this.servletContext = servletContext;
        this.grailsAttributes = new DefaultGrailsApplicationAttributes(this.servletContext);
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerClassByName(java.lang.String)
      */
    public GrailsControllerClass getControllerClassByName(String name) {
        return (GrailsControllerClass) this.application.getArtefact(
            ControllerArtefactHandler.TYPE, name);
    }


    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerClassByURI(java.lang.String)
      */
    public GrailsControllerClass getControllerClassByURI(String uri) {
        return (GrailsControllerClass) this.application.getArtefactForFeature(
            ControllerArtefactHandler.TYPE, uri);
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#getControllerInstance(org.codehaus.groovy.grails.commons.GrailsControllerClass)
      */
    public GroovyObject getControllerInstance(GrailsControllerClass controllerClass) {
        return (GroovyObject)this.applicationContext.getBean(controllerClass.getFullName());
    }





    /**
     * If in Proxy's are used in the Groovy context, unproxy (is that a word?) them by setting
     * the adaptee as the value in the map so that they can be used in non-groovy view technologies
     *
     * @param model The model as a map
     */
    private void removeProxiesFromModelObjects(Map<Object, Object> model) {
        for (Map.Entry entry : model.entrySet()) {
            if(entry.getValue() instanceof Proxy) {
            	entry.setValue(((Proxy)entry.getValue()).getAdaptee());
            }
        }
    }

    
	public ModelAndView handleURI(String uri, GrailsWebRequest webRequest) {
		return handleURI(uri, webRequest, Collections.EMPTY_MAP);
	}
	
    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#handleURI(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
      */
    public ModelAndView handleURI(String uri, GrailsWebRequest webRequest, Map params) {
        if(uri == null) {
            throw new IllegalArgumentException("Controller URI [" + uri + "] cannot be null!");
        }
        HttpServletRequest request = webRequest.getCurrentRequest();
        HttpServletResponse response = webRequest.getCurrentResponse();

        configureStateForWebRequest(webRequest, request);


        if(uri.endsWith("/")) {
            uri = uri.substring(0,uri.length() - 1);
        }

        // if the id is blank check if its a request parameter

        // Step 2: lookup the controller in the application.
        GrailsControllerClass controllerClass = getControllerClassByURI(uri);

        if (controllerClass == null) {
            throw new UnknownControllerException("No controller found for URI [" + uri + "]!");
        }

        actionName = controllerClass.getClosurePropertyName(uri);
        webRequest.setActionName(actionName);

        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Processing request for controller ["+controllerName+"], action ["+actionName+"], and id ["+id+"]");
        }
        controllerActionURI = SLASH + controllerName + SLASH + actionName + SLASH;
        
        // Step 3: load controller from application context.
        GroovyObject controller = getControllerInstance(controllerClass);


        if(!controllerClass.isHttpMethodAllowedForAction(controller, request.getMethod(), actionName)) {
        	try {
				response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return null;
			} catch (IOException e) {
				throw new ControllerExecutionException("I/O error sending 403 error",e);
			}
        }
        
        request.setAttribute( GrailsApplicationAttributes.CONTROLLER, controller );


        // Step 4: Set grails attributes in request scope
        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID,this.grailsAttributes);

        // Step 5: get the view name for this URI.
        String viewName = controllerClass.getViewByURI(uri);

        boolean executeAction = invokeBeforeInterceptor(controller, controllerClass);
        // if the interceptor returned false don't execute the action
        if(!executeAction)
            return null;

        ModelAndView mv = executeAction(controller, controllerClass, viewName, request, response, params);


        boolean returnModelAndView = invokeAfterInterceptor(controllerClass, controller, mv) && !response.isCommitted();
        return returnModelAndView ? mv : null;
    }




    /**
     * Invokes the action defined by the webRequest for the given arguments
     * 
     * @param controller The controller instance
     * @param controllerClass The GrailsControllerClass that defines the conventions within the controller
     * @param viewName The name of the view to delegate to if necessary
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     * @param params A map of parameters
     * @return A Spring ModelAndView instance
     */
    protected ModelAndView executeAction(GroovyObject controller, GrailsControllerClass controllerClass, String viewName, HttpServletRequest request, HttpServletResponse response, Map params) {
        // Step 5a: Check if there is a before interceptor if there is execute it
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            // Step 6: get closure from closure property
            Closure action;
            try {
                action = (Closure)controller.getProperty(actionName);
            }
            catch(MissingPropertyException mpe) {
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return null;
                } catch (IOException e) {
                        throw new ControllerExecutionException("I/O error sending 404 error",e);
                }
            }
            
            // Step 7: process the action
            Object returnValue = handleAction( controller,action,request,response,params );

            // Step 8: determine return value type and handle accordingly
            initChainModel(controller);
            if(response.isCommitted()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Response has been redirected, returning null model and view");
                }
                return null;
            }
            else {

                TokenResponseHandler handler = (TokenResponseHandler) request.getAttribute(TokenResponseHandler.KEY);
                if(handler != null && !handler.wasInvoked() && handler.wasInvalidToken()) {
                    String uri = (String) request.getAttribute(SynchronizerToken.URI);
                    if(uri == null) {
                        uri = WebUtils.getForwardURI(request);
                    }
                    try {
                        FlashScope flashScope = webRequest.getFlashScope();
                        flashScope.put("invalidToken", request.getParameter(SynchronizerToken.KEY));
                        response.sendRedirect(uri);
                        return null;
                    }
                    catch (IOException e) {
                        throw new ControllerExecutionException("I/O error sending redirect to URI: " + uri,e);
                    }
                }
                else if(request.getAttribute(ForwardMethod.CALLED) == null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Action ["+actionName+"] executed with result ["+returnValue+"] and view name ["+viewName+"]");
                    }
                    ModelAndView mv = handleActionResponse(controller,returnValue,actionName,viewName);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Action ["+actionName+"] handled, created Spring model and view ["+mv+"]");
                    }
                    return mv;
                }
                else {
                    return null;
                }
            }

        } finally {
            try {
                Thread.currentThread().setContextClassLoader(cl);
            }
            catch (java.security.AccessControlException e) {
                // not allowed by container, probably related to WAR deployment on AppEngine. Proceed.
            }
        }
    }

    private boolean invokeBeforeInterceptor(GroovyObject controller, GrailsControllerClass controllerClass) {
        boolean executeAction = true;
        if(controllerClass.isInterceptedBefore(controller,actionName)) {
            Closure beforeInterceptor = controllerClass.getBeforeInterceptor(controller);
            if(beforeInterceptor!= null) {
                if(beforeInterceptor.getDelegate() != controller) {
                    beforeInterceptor.setDelegate(controller);
                    beforeInterceptor.setResolveStrategy(Closure.DELEGATE_FIRST);                    
                }
                Object interceptorResult = beforeInterceptor.call();
                if(interceptorResult instanceof Boolean) {
                    executeAction = ((Boolean)interceptorResult).booleanValue();
                }
            }
        }
        return executeAction;
    }


    private void configureStateForWebRequest(GrailsWebRequest webRequest, HttpServletRequest request) {
        this.webRequest = webRequest;
        this.actionName = webRequest.getActionName();
        this.controllerName = webRequest.getControllerName();
        this.id = webRequest.getId();

        if(StringUtils.isBlank(id) && request.getParameter(GrailsWebRequest.ID_PARAMETER) != null) {
            id = request.getParameter(GrailsWebRequest.ID_PARAMETER);
        }
    }

    private boolean invokeAfterInterceptor(GrailsControllerClass controllerClass, GroovyObject controller, ModelAndView mv) {
        // Step 9: Check if there is after interceptor
        Object interceptorResult = null;
        if(controllerClass.isInterceptedAfter(controller,actionName)) {
            Closure afterInterceptor = controllerClass.getAfterInterceptor(controller);
            if(afterInterceptor.getDelegate() != controller) {
                afterInterceptor.setDelegate(controller);
                afterInterceptor.setResolveStrategy(Closure.DELEGATE_FIRST);
            }
            Map model = new HashMap();
            if(mv != null) {
				model =	mv.getModel() != null ? mv.getModel() : new HashMap();
            }
            switch(afterInterceptor.getMaximumNumberOfParameters()){
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
        return !(interceptorResult != null && interceptorResult instanceof Boolean) || ((Boolean) interceptorResult).booleanValue();
    }


    public GrailsApplicationAttributes getGrailsAttributes() {
        return this.grailsAttributes;
    }

    public Object handleAction(GroovyObject controller,Closure action, HttpServletRequest request, HttpServletResponse response) {
        return handleAction(controller,action,request,response,Collections.EMPTY_MAP);
    }

    public Object handleAction(GroovyObject controller,Closure action, HttpServletRequest request, HttpServletResponse response, Map params) {
        GrailsParameterMap paramsMap = (GrailsParameterMap)controller.getProperty("params"); 
        // if there are additional params add them to the params dynamic property
        if(params != null && !params.isEmpty()) {
            paramsMap.putAll( params );
        }
        Object returnValue = action.call();

        // Step 8: add any errors to the request
        request.setAttribute( GrailsApplicationAttributes.ERRORS, controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY) );

        return returnValue;
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper#handleActionResponse(org.codehaus.groovy.grails.commons.GrailsControllerClass, java.lang.Object, java.lang.String, java.lang.String)
      */
    public ModelAndView handleActionResponse( GroovyObject controller,Object returnValue,String closurePropertyName, String viewName) {
        boolean viewNameBlank = (viewName == null || viewName.length() == 0);
        // reset the metaclass
        ModelAndView explicityModelAndView = (ModelAndView)controller.getProperty(ControllerDynamicMethods.MODEL_AND_VIEW_PROPERTY);

        if(!webRequest.isRenderView()) {
            return null;
        }
        else if(explicityModelAndView != null) {
            return explicityModelAndView;
        }
        else if (returnValue == null) {
            if (viewNameBlank) {
                return null;
            } else {
                Map model;
                if(!this.chainModel.isEmpty()) {
                    model = new CompositeMap(this.chainModel, new BeanMap(controller));
                }
                else {
                    model = new BeanMap(controller);
                }

                return new ModelAndView(viewName, model);
            }
        } else if (returnValue instanceof Map) {
            // remove any Proxy wrappers and set the adaptee as the value
            Map returnModel = (Map)returnValue;
            removeProxiesFromModelObjects(returnModel);
            if(!this.chainModel.isEmpty()) {
                returnModel.putAll(this.chainModel);
            }
            return new ModelAndView(viewName, returnModel);

        } else if (returnValue instanceof ModelAndView) {
            ModelAndView modelAndView = (ModelAndView)returnValue;

            // remove any Proxy wrappers and set the adaptee as the value
            Map modelMap = modelAndView.getModel();
            removeProxiesFromModelObjects(modelMap);

            if(!this.chainModel.isEmpty()) {
                modelAndView.addAllObjects(this.chainModel);
            }

            if (modelAndView.getView() == null && modelAndView.getViewName() == null) {
                if (viewNameBlank) {
                    throw new NoViewNameDefinedException("ModelAndView instance returned by and no view name defined by nor for closure on property [" + closurePropertyName + "] in controller [" + controller.getClass() + "]!");
                } else {
                    modelAndView.setViewName(viewName);
                }
            }
            return modelAndView;
        }
        else {
            Map model;
            if(!this.chainModel.isEmpty()) {
                model = new CompositeMap(this.chainModel, new BeanMap(controller));
            }
            else {
                model = new BeanMap(controller);
            }
            return new ModelAndView(viewName, model);
        }
    }

	private void initChainModel(GroovyObject controller) {
		FlashScope fs = this.grailsAttributes.getFlashScope((HttpServletRequest)controller.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY));
        if(fs.containsKey(PROPERTY_CHAIN_MODEL)) {
            this.chainModel = (Map)fs.get(PROPERTY_CHAIN_MODEL);
            if(this.chainModel == null)
                this.chainModel = Collections.EMPTY_MAP;
        }
	}

}
