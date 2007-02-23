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

import java.beans.IntrospectionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import junit.framework.TestCase;
/**
 * Abstract test harness for testing controllers
 *
 * @author Graeme Rocher
 *
 */
public abstract class AbstractControllerTests extends TestCase {
    /**
     * A GroovyClassLoader instance
     */
    public GroovyClassLoader gcl = new GroovyClassLoader();
    /**
     * The GrailsApplication instance created during setup
     */
    public GrailsApplication ga;
	protected StaticMessageSource messageSource;

    protected final void setUp() throws Exception {
        super.setUp();

        onSetUp();

        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
    }
    
    protected GroovyObject getMockController(String name) throws IntrospectionException {
    	
    	GrailsControllerClass controllerClass = (GrailsControllerClass) ga.getArtefact(
            ControllerArtefactHandler.TYPE, name);
    	if(controllerClass == null)
    		throw new IllegalArgumentException("Controller not found for name " + name);
    	
    	GroovyObject mockController = (GroovyObject)controllerClass.newInstance();
        MockServletContext servletContext = new MockServletContext();
        MockApplicationContext appContext = new MockApplicationContext();
        appContext.registerMockBean("grailsApplication", ga);
        this.messageSource = new StaticMessageSource();
        
        appContext.registerMockBean("messageSource", this.messageSource);

        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,appContext);

        GrailsControllerHelper helper = new SimpleGrailsControllerHelper(ga,appContext,servletContext);
        HttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
        

        HttpServletResponse response = new MockHttpServletResponse();
        new ControllerDynamicMethods(mockController,helper,request,response);
    	
        mockController.setProperty("controllerUri", "/"+controllerClass.getLogicalPropertyName());
        return mockController;
    }

	protected abstract void onSetUp();

}
