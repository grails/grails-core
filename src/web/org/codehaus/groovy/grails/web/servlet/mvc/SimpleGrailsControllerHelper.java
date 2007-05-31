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
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffolder;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.metaclass.ChainDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.NoClosurePropertyForURIException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.NoViewNameDefinedException;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.UnknownControllerException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class for handling controller requests
 *
 * @author Graeme Rocher
 * @since 0.1
 * 
 * Created: 12-Jan-2006
 */
public class SimpleGrailsControllerHelper implements GrailsControllerHelper {

    private static final String SCAFFOLDER = "Scaffolder";
    

    private GrailsApplication application;
    private ApplicationContext applicationContext;
    private Map chainModel = Collections.EMPTY_MAP;
    private ServletContext servletContext;
    private GrailsApplicationAttributes grailsAttributes;
    private Pattern uriPattern = Pattern.compile("/(\\w+)/?(\\w*)/?([^/]*)/?(.*)");


	private GrailsWebRequest webRequest;
    
    private static final Log LOG = LogFactory.getLog(SimpleGrailsControllerHelper.class);
    private static final String DISPATCH_ACTION_PARAMETER = "_action_";
    private static final String ID_PARAMETER = "id";


	private String id;


	private String controllerName;


	private String actionName;


	private Map extraParams;

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
    private void removeProxiesFromModelObjects(Map model) {

        for (Iterator keyIter = model.keySet().iterator(); keyIter.hasNext();) {
            Object current = keyIter.next();
            Object modelObject = model.get(current);
            if(modelObject instanceof Proxy) {
                model.put( current, ((Proxy)modelObject).getAdaptee() );
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
        if(uri == null)
            throw new IllegalArgumentException("Controller URI [" + uri + "] cannot be null!");
        
        this.webRequest = webRequest;

        uri = configureStateForUri(uri);
       
        HttpServletRequest request = webRequest.getCurrentRequest();
        HttpServletResponse response = webRequest.getCurrentResponse();

        // if the action name is blank check its included as dispatch parameter
        if(StringUtils.isBlank(actionName))
        	uri = checkDispatchAction(request, uri);

        if(uri.endsWith("/"))
            uri = uri.substring(0,uri.length() - 1);

        // if the id is blank check if its a request parameter
        if(StringUtils.isBlank(id) && request.getParameter(ID_PARAMETER) != null) {
            id = request.getParameter(ID_PARAMETER);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Processing request for controller ["+controllerName+"], action ["+actionName+"], and id ["+id+"]");
        }
        if(LOG.isTraceEnabled()) {
            LOG.trace("Extra params from uri ["+extraParams+"] ");
        }
        // Step 2: lookup the controller in the application.
        GrailsControllerClass controllerClass = getControllerClassByURI(uri);

        if (controllerClass == null) {
            throw new UnknownControllerException("No controller found for URI [" + uri + "]!");
        }

        // parse the uri in its individual tokens
        controllerName = WordUtils.uncapitalize(controllerClass.getName());

        // Step 3: load controller from application context.
        GroovyObject controller = getControllerInstance(controllerClass);

        if(!controllerClass.isHttpMethodAllowedForAction(controller, request.getMethod(), actionName)) {
        	try {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return null;
			} catch (IOException e) {
				throw new ControllerExecutionException("I/O error sending 403 error",e);
			}
        }
        
        request.setAttribute( GrailsApplicationAttributes.CONTROLLER, controller );


        // Step 3: if scaffolding retrieve scaffolder
        GrailsScaffolder scaffolder = null;
        if(controllerClass.isScaffolding())  {
            scaffolder = (GrailsScaffolder)applicationContext.getBean( controllerClass.getFullName() + SCAFFOLDER );
            if(scaffolder == null)
                throw new IllegalStateException("Scaffolding set to true for controller ["+controllerClass.getFullName()+"] but no scaffolder available!");
        }

        // Step 4: get closure property name for URI.
        if(StringUtils.isBlank(actionName))
            actionName = controllerClass.getClosurePropertyName(uri);

        if (StringUtils.isBlank(actionName)) {
            // Step 4a: Check if scaffolding
            if( controllerClass.isScaffolding() && !scaffolder.supportsAction(actionName))
                throw new NoClosurePropertyForURIException("Could not find closure property for URI [" + uri + "] for controller [" + controllerClass.getFullName() + "]!");
        }

        // Step 4a: Set the action and controller name of the web request
        webRequest.setActionName(actionName);
        webRequest.setControllerName(controllerName);
        
        // populate additional params from url
        Map controllerParams = webRequest.getParameterMap();
        
        if(!StringUtils.isBlank(id)) {
            controllerParams.put(GrailsApplicationAttributes.ID_PARAM, id);
        }
        if(!extraParams.isEmpty()) {
            for (Iterator i = extraParams.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                controllerParams.put(name,extraParams.get(name));
            }
        }

        // set the flash scope instance to its next state and set on controller
        FlashScope fs = this.grailsAttributes.getFlashScope(request);
        fs.next();

        // Step 4b: Set grails attributes in request scope
        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID,this.grailsAttributes);

        // Step 5: get the view name for this URI.
        String viewName = controllerClass.getViewByURI(uri);

        // Step 5a: Check if there is a before interceptor if there is execute it
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(application.getClassLoader());
            boolean executeAction = true;
            if(controllerClass.isInterceptedBefore(controller,actionName)) {
                Closure beforeInterceptor = controllerClass.getBeforeInterceptor(controller);
                if(beforeInterceptor!= null) {
                    Object interceptorResult = beforeInterceptor.call();
                    if(interceptorResult instanceof Boolean) {
                        executeAction = ((Boolean)interceptorResult).booleanValue();
                    }
                }
            }
            // if the interceptor returned false don't execute the action
            if(!executeAction)
        	return null;

            // Step 6: get closure from closure property
            Closure action;
            try {
                action = (Closure)controller.getProperty(actionName);
                // Step 7: process the action
                Object returnValue = handleAction( controller,action,request,response,params );


                // Step 8: determine return value type and handle accordingly
                initChainModel(controller);
                if(response.isCommitted()) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Response has been redirected, returning null model and view");
                    }
                    invokeAfterInterceptor(controllerClass, controller, null);
                    return null;
                }
                else {

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Action ["+actionName+"] executed with result ["+returnValue+"] and view name ["+viewName+"]");
                    }
                    ModelAndView mv = handleActionResponse(controller,returnValue,actionName,viewName);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Action ["+actionName+"] handled, created Spring model and view ["+mv+"]");
                    }
                    invokeAfterInterceptor(controllerClass, controller, mv);
                    return mv;
                }

            }
            catch(MissingPropertyException mpe) {
                if(controllerClass.isScaffolding())
                    throw new IllegalStateException("Scaffolder supports action ["+actionName +"] for controller ["+controllerClass.getFullName()+"] but getAction returned null!");
                else {
                    try {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return null;
                    } catch (IOException e) {
                            throw new ControllerExecutionException("I/O error sending 404 error",e);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

    }

    private void invokeAfterInterceptor(GrailsControllerClass controllerClass, GroovyObject controller, ModelAndView mv) {
        // Step 9: Check if there is after interceptor
        if(controllerClass.isInterceptedAfter(controller,actionName)) {
            Closure afterInterceptor = controllerClass.getAfterInterceptor(controller);
            Map model = Collections.EMPTY_MAP;
 			if(mv != null) {
				model =	mv.getModel() != null ? mv.getModel() : Collections.EMPTY_MAP;
			}
            afterInterceptor.call(new Object[]{ model });
        }
    }

    private String configureStateForUri(String uri) {
		// step 1: process the uri
        if (uri.indexOf("?") > -1) {
            uri = uri.substring(0, uri.indexOf("?"));
        }
        if(uri.indexOf('\\') > -1) {
            uri = uri.replaceAll("\\\\", "/");
        }
        if(!uri.startsWith("/"))
            uri = '/' + uri;
        if(uri.endsWith("/"))
            uri = uri.substring(0,uri.length() - 1);

        id = null;
		controllerName = null;
		actionName = null;
		extraParams = Collections.EMPTY_MAP;
		Matcher m = uriPattern.matcher(uri);
        if(m.find()) {
            controllerName = m.group(1);
            actionName =  m.group(2);
            uri = '/' + controllerName + '/' + actionName;
            id = m.group(3);
            String extraParamsString = m.group(4);
            if(!StringUtils.isBlank(extraParamsString)) {
                extraParams = new HashMap();
                if(extraParamsString.indexOf('/') > -1) {
                    String[] tokens = extraParamsString.split("/");

                    for (int i = 0; i < tokens.length; i++) {
                        String token = tokens[i];
                        if(i == 0 || ((i % 2) == 0)) {
                            if((i + 1) < tokens.length) {
                                extraParams.put(token, tokens[i + 1]);
                            }
                        }
                    }
                }
                else {
                   extraParams.put(extraParamsString, null); 
                }


            }
        }
		return uri;
	}
	
	private String checkDispatchAction(HttpServletRequest request, String uri) {
    	for(Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = (String)e.nextElement();
            if(name.startsWith(DISPATCH_ACTION_PARAMETER)) {
            	// remove .x suffix in case of submit image
            	name = StringUtils.removeEnd(name, ".x");
            	actionName = GrailsClassUtils.getPropertyNameRepresentation(name.substring((DISPATCH_ACTION_PARAMETER).length()));
            	uri = '/' + controllerName + '/' + actionName;
            	break;
            }
        }
        return uri;
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
        // Step 7: determine argument count and execute.
        Class[] paramTypes = action.getParameterTypes();
        List commandObjects = new ArrayList();
        if(paramTypes != null) {
            for(int j = 0; j < paramTypes.length; j++) {
                Class paramType = paramTypes[j];
                if(GroovyObject.class.isAssignableFrom(paramType)) {
                    try {
                        GroovyObject commandObject = (GroovyObject) paramType.newInstance();
                        GrailsDataBinder binder = GrailsDataBinder.createBinder(commandObject, commandObject.getClass().getName());
                        binder.bind(new MutablePropertyValues(paramsMap));

                        Errors errors = new BindException(commandObject, paramType.getName());
                        Collection constrainedProperties = ((Map)commandObject.getProperty("constraints")).values();
                        for (Iterator i = constrainedProperties.iterator(); i.hasNext();) {
                            ConstrainedProperty constrainedProperty = (ConstrainedProperty)i.next();
                            constrainedProperty.setMessageSource( applicationContext );
                            constrainedProperty.validate(commandObject, commandObject.getProperty( constrainedProperty.getPropertyName() ),errors);
                        }
                        commandObject.setProperty("errors", errors);
                        if(errors.hasErrors()) {
                            LOG.warn("Command Object " + paramType.getName() + " Failed Validation");
                        }
                        commandObjects.add(commandObject);
                    } catch (Exception e) {
                        throw new ControllerExecutionException("Error occurred creating command object.", e);
                    }
                }
            }
        }
        Object returnValue = action.call(commandObjects.toArray());

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
        if(fs.containsKey(ChainDynamicMethod.PROPERTY_CHAIN_MODEL)) {
            this.chainModel = (Map)fs.get(ChainDynamicMethod.PROPERTY_CHAIN_MODEL);
            if(this.chainModel == null)
                this.chainModel = Collections.EMPTY_MAP;
        }
	}


}
