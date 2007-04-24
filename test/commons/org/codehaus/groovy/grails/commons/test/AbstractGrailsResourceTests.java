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
package org.codehaus.groovy.grails.commons.test;

import java.beans.IntrospectionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsControllerHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsControllerHelper;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import junit.framework.TestCase;
/**
 * <p>Abstract test harness that takes some classes and bootstraps the whole Grails environment.
 * <p>
 * This class can be extended by an child that wants to bootstrap the whole Grails environment 
 * and perform integration type tests against a real data source, and mocked servlet container.
 * 
 * <p>
 * You can modify the resourcePattern the test case uses to load Grails classes using the onSetUp method
 * 
 * <pre>
 * void onSetUp() {
 *    resourcePattern = "my/path/grails-app/**.groovy"
 * }
 * </pre>
 * 
 * The above code loads all of the groovy files in the specified path into a Grails environment
 * 
 *
 * @author Graeme Rocher
 *
 */
public abstract class AbstractGrailsResourceTests extends TestCase {
    /**
     * A GroovyClassLoader instance
     */
    public GroovyClassLoader gcl = new GroovyClassLoader();
    /**
     * The GrailsApplication instance created during setup
     */
    public GrailsApplication ga;
	protected StaticMessageSource messageSource;
	protected MockApplicationContext parentContext;
	protected WebApplicationContext applicationContext;
	protected MockServletContext servletContext;
	protected SessionFactory sessionFactory;
	protected Session session;
	protected String resourcePattern = "**/grails-app/*/*.groovy";
	private PathMatchingResourcePatternResolver resolver;
	private Resource[] resources;
	
	

    public void setResourcePattern(String resourcePattern) {
		this.resourcePattern = resourcePattern;
	}



	protected final void setUp() throws Exception {
        super.setUp();

        onSetUp();
        resolver = new PathMatchingResourcePatternResolver();
        resources = resolver.getResources(resourcePattern);
        ga = new DefaultGrailsApplication(resources);
        parentContext = new MockApplicationContext();
        parentContext.registerMockBean("grailsApplication", ga);
		GrailsRuntimeConfigurator configurator = new GrailsRuntimeConfigurator(ga, parentContext);
		servletContext = new MockServletContext();
		applicationContext = configurator.configure(servletContext);
		
		if(this.applicationContext.containsBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN)) {
	        this.sessionFactory = (SessionFactory)this.applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN);
	        GrailsHibernateUtil.configureDynamicMethods(applicationContext,ga);

	        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
	            this.session = this.sessionFactory.openSession();
	            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(session));
	        }			
		}		
    }
    

    
    protected GroovyObject getMockController(String name) throws IntrospectionException {
    	
    	GrailsControllerClass controllerClass = (GrailsControllerClass) ga.getArtefact(
            ControllerArtefactHandler.TYPE, name);
    	if(controllerClass == null)
    		throw new IllegalArgumentException("Controller not found for name " + name);
    	
    	GroovyObject mockController = (GroovyObject)controllerClass.newInstance();        
		this.messageSource = new StaticMessageSource();
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,parentContext);
        
        GrailsControllerHelper helper = new SimpleGrailsControllerHelper(ga,parentContext,servletContext);
        HttpServletRequest request = createMockRequest();
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
        
        
        HttpServletResponse response = new MockHttpServletResponse();
    	
        mockController.setProperty("controllerUri", "/"+controllerClass.getLogicalPropertyName());
        return mockController;
    }



	/**
	 * @return
	 */
	protected HttpServletRequest createMockRequest() {
		HttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		return request;
	}

	protected HttpServletRequest createMockRequest(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setRequestURI(uri);
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		return request;
	}
	
	protected abstract void onSetUp();
	
    protected final void tearDown() throws Exception {
        super.tearDown();
		if(TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
		    SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(this.sessionFactory);
		    org.hibernate.Session s = holder.getSession();
		    s.flush();
		    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
		    SessionFactoryUtils.releaseSession(s, this.sessionFactory);
		}
        onTearDown();

        gcl = null;
        ga = null;
        messageSource = null;
        parentContext = null;
        applicationContext = null;
        servletContext = null;
        sessionFactory = null;
        session = null;
        resourcePattern = null;
        resolver = null;
        resources = null;
    }
    
    /**
     * Called directly before destruction of the TestCase in the junit.framework.TestCase#tearDown() method
     */
    protected void onTearDown() throws Exception {
    	
    }



	public Resource[] getResources() {
		return resources;
	}
    
}
