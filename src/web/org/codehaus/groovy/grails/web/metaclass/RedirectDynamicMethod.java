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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletResponse;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Implements the "redirect" Controller method for action redirection
 * 
 * @author Graeme Rocher
 * @since 0.2
 * 
 * Created Oct 27, 2005
 */
public class RedirectDynamicMethod extends AbstractDynamicMethodInvocation {

	private static final String SCAFFOLDER = "Scaffolder";
    public static final String METHOD_SIGNATURE = "redirect";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    public static final String ARGUMENT_URI = "uri";
    public static final String ARGUMENT_URL = "url";
    public static final String ARGUMENT_CONTROLLER = "controller";
    public static final String ARGUMENT_ACTION = "action";
    public static final String ARGUMENT_ID = "id";
    public static final String ARGUMENT_PARAMS = "params";
    public static final String ARGUMENT_ERRORS = "errors";

    private static final Log LOG = LogFactory.getLog(RedirectDynamicMethod.class);

    public RedirectDynamicMethod() {
        super(METHOD_PATTERN);
    }

    public Object invoke(Object target, Object[] arguments) {
        if(arguments.length == 0)
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);

        Object actionRef = null;
        String controllerName = null;
        Object id = null;
        Object uri = null;
        String url = null;
        Map params;
        Errors errors;
        GroovyObject controller = (GroovyObject)target;

        if(arguments[0] instanceof Map) {
            Map argMap = (Map)arguments[0];
            if(argMap.containsKey(ARGUMENT_URI)) {
                 uri = argMap.get(ARGUMENT_URI);
            }
            else if(argMap.containsKey(ARGUMENT_URL)) {
                url = argMap.get(ARGUMENT_URL).toString();
            }
            else {
                actionRef = argMap.get(ARGUMENT_ACTION);
                controllerName = (String)argMap.get(ARGUMENT_CONTROLLER);
                id =  argMap.get(ARGUMENT_ID);
            }
            params = (Map)argMap.get(ARGUMENT_PARAMS);
            errors = (Errors)argMap.get(ARGUMENT_ERRORS);
        }
        else {
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);
        }
        // if there are errors add it to the list of errors
        Errors controllerErrors = (Errors)controller.getProperty( ControllerDynamicMethods.ERRORS_PROPERTY );
        if(controllerErrors != null) {
            controllerErrors.addAllErrors(errors);
        }
        else {
            controller.setProperty( ControllerDynamicMethods.ERRORS_PROPERTY, errors);
        }

        String actualUri;
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        
        GrailsApplicationAttributes attrs = webRequest.getAttributes();
        HttpServletRequest request = webRequest.getCurrentRequest();
        GrailsHttpServletResponse response = webRequest.getCurrentResponse();
        
        if(uri != null) {
            actualUri = attrs.getApplicationUri(request) + uri.toString();
        }
        else if(url != null) {
            actualUri = url;
        }
        else {
            String actionName = null;
            if(actionRef instanceof String) {
               actionName = (String)actionRef;
            }
            else if(actionRef instanceof Closure) {
                Closure c = (Closure)actionRef;
                PropertyDescriptor prop = GrailsClassUtils.getPropertyDescriptorForValue(target,c);
                if(prop != null) {
                    actionName = prop.getName();
                }
                else {
                    GrailsScaffolder scaffolder = getScaffolderForController(target.getClass().getName(), webRequest);
                    if(scaffolder != null) {
                            actionName = scaffolder.getActionName(c);
                    }
                }
            }
            else {
                actionName = ""; // default action
            }
            if(actionName != null) {
                StringBuffer actualUriBuf = new StringBuffer(attrs.getApplicationUri(request));
                if(actionName.indexOf('/') > -1) {
                      actualUriBuf.append(actionName);
                }
                else {
                    if(controllerName != null) {
                        actualUriBuf.append('/')
                                 .append(controllerName);
                    }
                    else {
                        actualUriBuf.append(attrs.getControllerUri(request));
                    }
                }
                actualUriBuf.append('/')
                         .append(actionName);
                if(id != null) {
                    actualUriBuf.append('/')
                             .append(id);
                }
                if(params != null) {
                    actualUriBuf.append('?');
                    for (Iterator i = params.keySet().iterator(); i.hasNext();) {
                        Object name = i.next();
                        Object value = params.get(name);
                        if (value==null)
                            value = "";
                        try {
                            actualUriBuf.append(URLEncoder.encode(name.toString(),request.getCharacterEncoding()))
                                     .append('=')
                                     .append(URLEncoder.encode(value.toString(),request.getCharacterEncoding()));
                        } catch (UnsupportedEncodingException ex) {
                            throw new ControllerExecutionException("Error redirecting request for url ["+name+":"+value +"]: " + ex.getMessage(),ex);
                        }
                        if(i.hasNext())
                            actualUriBuf.append('&');
                    }
                }
                actualUri = actualUriBuf.toString();
            }
            else {
                throw new ControllerExecutionException("Action not found in redirect for name ["+actionName+"]");
            }
        }


        if(LOG.isDebugEnabled()) {
            LOG.debug("Dynamic method [redirect] forwarding request to ["+actualUri +"]");
        }

        try {
            response.sendRedirect(response.encodeRedirectURL(actualUri));
        } catch (IOException e) {
            throw new ControllerExecutionException("Error redirecting request for url ["+actualUri +"]: " + e.getMessage(),e);
        }
        return null;
    }
    
    public GrailsScaffolder getScaffolderForController(String controllerName, GrailsWebRequest webRequest) {
    	GrailsApplicationAttributes attributes = webRequest.getAttributes();
		GrailsControllerClass controllerClass = attributes.getGrailsApplication().getController(controllerName);    	
        return (GrailsScaffolder)attributes
        							.getApplicationContext()
        							.getBean(controllerClass.getFullName() + SCAFFOLDER );
    }    

}
