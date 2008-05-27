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

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class the encapsulates a Grails request. An instance of this class is bound to the current thread using
 * Spring's RequestContextHolder which can later be retrieved using:
 * 
 * def webRequest = RequestContextHolder.currentRequestAttributes()
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class GrailsWebRequest extends DispatcherServletWebRequest implements ParameterInitializationCallback {

	private GrailsApplicationAttributes attributes;
	private GrailsParameterMap params;
	private HttpServletResponse response;
	private GrailsHttpSession session;
	private boolean renderView = true;
    public static final String ID_PARAMETER = "id";
    private List parameterCreationListeners = new ArrayList();


    public GrailsWebRequest(HttpServletRequest request,  HttpServletResponse response, ServletContext servletContext) {
		super(request);
		this.attributes = new DefaultGrailsApplicationAttributes(servletContext);
		this.response = response;

		
	}

    /**
     * Overriden to return the GrailsParameterMap instance
     *
     * @return An instance of GrailsParameterMap
     */
    public Map getParameterMap() {
        if(this.params == null) {
            this.params = new GrailsParameterMap(getCurrentRequest());
            fireParametersCreated();            
        }
        return this.params;
    }

    /**
	 * @return the out
	 */
	public Writer getOut() {
		Writer out = attributes.getOut(getCurrentRequest());
		if(out ==null)
			try {
				return getCurrentResponse().getWriter();
			} catch (IOException e) {
				throw new ControllerExecutionException("Error retrieving response writer: " + e.getMessage(), e);
			}
		return out;
	}



	/**
	 * @param out the out to set
	 */
	public void setOut(Writer out) {
		attributes.setOut(getCurrentRequest(),out);
	}



	/**
	 * @return The ServletContext instance
	 */
	public ServletContext getServletContext() {
		return this.attributes.getServletContext();
	}

	/**
	 * @return The FlashScope instance for the current request
	 */
	public FlashScope getFlashScope() {
		return attributes.getFlashScope(getRequest());
	}
	
	/**
	 * @return The currently executing request
	 */
	public HttpServletRequest getCurrentRequest() {
		return getRequest();
	}
	
	public HttpServletResponse getCurrentResponse() {
		return this.response;
	}
	
	/**
	 * @return The Grails params object
	 */
	public GrailsParameterMap getParams() {
        if(this.params == null) {
            this.params = new GrailsParameterMap(getCurrentRequest());
            fireParametersCreated();
        }
        return this.params;
	}

    private void fireParametersCreated() {
        for (Iterator i = parameterCreationListeners.iterator(); i.hasNext();) {
            ParameterCreationListener parameterCreationListener = (ParameterCreationListener) i.next();
            parameterCreationListener.paramsCreated(this.params);
        }
    }

    /**
	 * 
	 * @return The Grails session object
	 */
	public GrailsHttpSession getSession() {
		if(session == null)
			session = new GrailsHttpSession(getCurrentRequest());
		
		return session;
	}
	
	/**
	 * @return The GrailsApplicationAttributes instance
	 */
	public GrailsApplicationAttributes getAttributes() {
		return this.attributes;
	}

	public void setActionName(String actionName) {
		getCurrentRequest().setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, actionName);
	}

	public void setControllerName(String controllerName) {
		getCurrentRequest().setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controllerName);
	}

	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return (String)getCurrentRequest().getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE);
	}

	/**
	 * @return the controllerName
	 */
	public String getControllerName() {
		return (String)getCurrentRequest().getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
	}

	public void setRenderView(boolean renderView) {
		this.renderView = renderView;
	}

	/**
	 * @return True if the view for this GrailsWebRequest should be rendered
	 */
	public boolean isRenderView() {
		return renderView;
	}

    public String getId() {
        Object id = getParams().get(ID_PARAMETER);
        return id != null ? id.toString() : null;
    }

    /**
     * Returns true if the current executing request is a flow request
     *
     * @return True if it is a flow request
     */
    public boolean isFlowRequest() {
        GrailsApplication application = getAttributes().getGrailsApplication();
        GrailsControllerClass controllerClass = (GrailsControllerClass)application.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE,getControllerName());

        String actionName = getActionName();
        if(actionName == null) actionName = controllerClass.getDefaultAction();

        if(actionName == null) return false;

        if(controllerClass != null && controllerClass.isFlowAction(actionName)) return true;
        return false;
    }

    public void addParameterListener(ParameterCreationListener creationListener) {
        this.parameterCreationListeners.add(creationListener);
    }

    /**
     * Obtains the ApplicationContext object
     *
     * @return The ApplicationContext
     */
    public ApplicationContext getApplicationContext() {
        return getAttributes().getApplicationContext();
    }
}
