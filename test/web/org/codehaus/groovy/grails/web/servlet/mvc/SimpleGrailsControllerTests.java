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

import groovy.lang.GroovyClassLoader;
import groovy.lang.MetaClassRegistry;

import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClassCreationHandle;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;
import grails.util.GrailsWebUtil;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public class SimpleGrailsControllerTests extends TestCase {

	public SimpleGrailsControllerTests() {
		super();
	}

	protected GrailsApplication grailsApplication = null;
	protected SimpleGrailsController controller = null;
	private GenericApplicationContext localContext;
	private ConfigurableApplicationContext appCtx;
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		InvokerHelper.getInstance()
					.getMetaRegistry()
					.setMetaClassCreationHandle(new ExpandoMetaClassCreationHandle());
		GroovyClassLoader cl = new GroovyClassLoader();
		
		Class c1 = cl.parseClass("class TestController {\n"+
							" Closure test = {\n"+
								"return [ \"test\" : \"123\" ]\n"+
						     "}\n" +
						"}");	
		
		Class c2 = cl.parseClass("class SimpleController {\n"+
				" Closure test = {\n"+
			     "}\n" +
			"}");
		
		Class c3 = cl.parseClass("class NoViewController {\n"+
				" Closure test = {\n"+
			      "request, response ->\n" +
			      "new grails.util.OpenRicoBuilder(response).ajax { element(id:\"test\") { } };\n" +
			      "return null;\n" +				
			     "}\n" +
			"}");		
		
		Class c4 = cl.parseClass("class RestrictedController {\n"+
				"def allowedMethods=[action1:'POST', action3:['PUT', 'DELETE']]\n" +
				"def action1 = {}\n" +
				"def action2 = {}\n" +
				"def action3 = {}\n" +
		"}");
		
//		this.grailsApplication = new DefaultGrailsApplication(new Class[]{c1,c2,c3},cl);
//		this.controller = new SimpleGrailsController();
//		this.controller.setGrailsApplication(grailsApplication);
		
		Thread.currentThread().setContextClassLoader(cl);
		
		//grailsApplication = new DefaultGrailsApplication(,cl);
		this.localContext = new GenericApplicationContext();
		
		ConstructorArgumentValues args = new ConstructorArgumentValues();
		args.addGenericArgumentValue(new Class[]{c1,c2,c3,c4});
		args.addGenericArgumentValue(cl);
		MutablePropertyValues propValues = new MutablePropertyValues();
		
		BeanDefinition grailsApplicationBean = new RootBeanDefinition(DefaultGrailsApplication.class,args,propValues);		
		localContext.registerBeanDefinition( "grailsApplication", grailsApplicationBean );
		this.localContext.refresh();
		
		this.grailsApplication = (GrailsApplication)localContext.getBean("grailsApplication");
		
		/*BeanDefinition applicationEventMulticaster = new RootBeanDefinition(SimpleApplicationEventMulticaster.class);
		context.registerBeanDefinition( "applicationEventMulticaster ", applicationEventMulticaster);*/
		GrailsRuntimeConfigurator rConfig = new GrailsRuntimeConfigurator(grailsApplication, localContext);
		
		MockServletContext servletContext = new MockServletContext();
		this.appCtx = (ConfigurableApplicationContext)rConfig.configure(servletContext);
		
		this.controller = (SimpleGrailsController)appCtx.getBean("simpleGrailsController");
		
		servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,appCtx);
		controller.setServletContext(servletContext);
		assertNotNull(appCtx);		
		super.setUp();
	}
	
	

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(new MetaClassRegistry.MetaClassCreationHandle());
	}



	private ModelAndView execute(String uri, Properties parameters) throws Exception {
		return execute(uri, parameters, "GET");
	}
	
	private ModelAndView execute(String uri, Properties parameters, String requestMethod) throws Exception {
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest((GrailsWebApplicationContext)this.appCtx);

        MockHttpServletRequest request = (MockHttpServletRequest)((GrailsHttpServletRequest)webRequest.getCurrentRequest()).getRequest();
        request.setRequestURI(uri);
        request.setMethod(requestMethod);
        
		request.setContextPath("/simple");
		
		if (parameters != null) {
			for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();) {
				String paramName = (String)iter.next();
				String paramValue = parameters.getProperty(paramName);
				request.addParameter(paramName, paramValue);
			}
		}
		return controller.handleRequest(request, null);
	}
	
	public void testSimpleControllerSuccess() throws Exception {
		ModelAndView modelAndView = execute("/test/test", null);
		assertNotNull(modelAndView);
	}
	
	public void testAllowedMethods() throws Exception {
		assertResponseStatusCode("/restricted/action1", "GET", HttpServletResponse.SC_FORBIDDEN);
		assertResponseStatusCode("/restricted/action1", "PUT", HttpServletResponse.SC_FORBIDDEN);
		assertResponseStatusCode("/restricted/action1", "POST", HttpServletResponse.SC_OK);
		assertResponseStatusCode("/restricted/action1", "DELETE", HttpServletResponse.SC_FORBIDDEN);

		assertResponseStatusCode("/restricted/action2", "GET", HttpServletResponse.SC_OK);
		assertResponseStatusCode("/restricted/action2", "PUT", HttpServletResponse.SC_OK);
		assertResponseStatusCode("/restricted/action2", "POST", HttpServletResponse.SC_OK);
		assertResponseStatusCode("/restricted/action2", "DELETE", HttpServletResponse.SC_OK);
		
		assertResponseStatusCode("/restricted/action3", "GET", HttpServletResponse.SC_FORBIDDEN);
		assertResponseStatusCode("/restricted/action3", "PUT", HttpServletResponse.SC_OK);
		assertResponseStatusCode("/restricted/action3", "POST", HttpServletResponse.SC_FORBIDDEN);
		assertResponseStatusCode("/restricted/action3", "DELETE", HttpServletResponse.SC_OK);
	}
	
	private void assertResponseStatusCode(String uri, String httpMethod, int expectedStatusCode) throws Exception {
		execute(uri, null, httpMethod);
		GrailsWebRequest gwr = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
		MockHttpServletResponse res = (MockHttpServletResponse) gwr.getCurrentResponse().getDelegate();
		
		assertEquals(expectedStatusCode, res.getStatus());
	}
}
