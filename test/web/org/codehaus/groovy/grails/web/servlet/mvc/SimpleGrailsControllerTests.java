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
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.ApplicationHolder;
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
import org.springframework.validation.Errors;
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
        cl.parseClass("class MyCommandObject {\n" +
                "String firstName\n" +
                "String lastName\n" +
                "static constraints = {\n" +
                "  firstName(maxSize:10)\n" +
                "  lastName(maxSize:10)" +
                "}\n" +
                "}");
        cl.parseClass("class AnotherCommandObject {\n" +
                "Integer age\n" +
                "static constraints = {\n" +
                "  age(max:99)\n" +
                "}\n" +
                "}");
        cl.parseClass("class UnconstrainedCommandObject {\n" +
                "String firstName\n" +
                "}");
		Class testControllerClass = cl.parseClass("class TestController {\n"+
							" Closure test = {\n"+
								"return [ \"test\" : \"123\" ]\n"+
						     "}\n" +
                             "def singlecommandobject = { MyCommandObject mco ->\n" +
                             "[theFirstName:mco.firstName, theLastName:mco.lastName, validationErrors:mco.errors]\n" +
                             "}\n" +
                             "def multiplecommandobjects = {MyCommandObject mco, AnotherCommandObject aco ->\n" +
                             "[theFirstName:mco.firstName, theLastName:mco.lastName, theAge:aco.age, acoErrors:aco.errors, mcoErrors:mco.errors]\n" +
                             "}\n" +
                             "def unconstrainedcommandobject = { UnconstrainedCommandObject uco ->" +
                             "[theFirstName:uco.firstName, validationErrors:uco.errors]\n" +
                             "}\n" +
						"}");

		Class simpleControllerClass = cl.parseClass("class SimpleController {\n"+
				" Closure test = {\n"+
			     "}\n" +
			"}");

		Class noViewControllerClass = cl.parseClass("class NoViewController {\n"+
				" Closure test = {\n"+
			      "request, response ->\n" +
			      "new grails.util.OpenRicoBuilder(response).ajax { element(id:\"test\") { } };\n" +
			      "return null;\n" +
			     "}\n" +
			"}");

		Class restrictedControllerClass = cl.parseClass("class RestrictedController {\n"+
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
		args.addGenericArgumentValue(new Class[]{testControllerClass,simpleControllerClass,noViewControllerClass,restrictedControllerClass});
		args.addGenericArgumentValue(cl);
		MutablePropertyValues propValues = new MutablePropertyValues();

		BeanDefinition grailsApplicationBean = new RootBeanDefinition(DefaultGrailsApplication.class,args,propValues);
		localContext.registerBeanDefinition( "grailsApplication", grailsApplicationBean );
		this.localContext.refresh();

		this.grailsApplication = (GrailsApplication)localContext.getBean("grailsApplication");
        ApplicationHolder.setApplication(grailsApplication);
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

        grailsApplication = null;
	    controller = null;
	    localContext = null;
	    appCtx = null;
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

    public void testUnconstrainedCommandObject() throws Exception {
        ModelAndView modelAndView = execute("/test/unconstrainedcommandobject/1/firstName/James", null);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "James", model.get("theFirstName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of errors", 0, validationErrors.getErrorCount());
    }

    public void testSingleCommandObjectValidationSuccess() throws Exception {
        ModelAndView modelAndView = execute("/test/singlecommandobject/1/firstName/James/lastName/Gosling", null);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "James", model.get("theFirstName"));
        assertEquals("wrong lastName", "Gosling", model.get("theLastName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of errors", 0, validationErrors.getErrorCount());
    }

    public void testMultipleCommandObjectValidationSuccess() throws Exception {
        ModelAndView modelAndView = execute("/test/multiplecommandobjects/1/firstName/James/age/30/lastName/Gosling", null);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "James", model.get("theFirstName"));
        assertEquals("wrong lastName", "Gosling", model.get("theLastName"));
        assertEquals("wrong age", new Integer(30), model.get("theAge"));
        Errors mcoErrors = (Errors) model.get("mcoErrors");
        assertEquals("wrong number of mco errors", 0, mcoErrors.getErrorCount());
        Errors acoErrors = (Errors) model.get("acoErrors");
        assertEquals("wrong number of aco errors", 0, acoErrors.getErrorCount());
    }

    public void testSingleCommandObjectValidationFailure() throws Exception {
        ModelAndView modelAndView = execute("/test/singlecommandobject/1/firstName/ThisFirstNameIsTooLong/lastName/ThisLastNameIsTooLong", null);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "ThisFirstNameIsTooLong", model.get("theFirstName"));
        assertEquals("wrong lastName", "ThisLastNameIsTooLong", model.get("theLastName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of mcoErrors", 2, validationErrors.getErrorCount());
    }

    public void testMultipleCommandObjectValidationFailure() throws Exception {
        ModelAndView modelAndView = execute("/test/multiplecommandobjects/1/firstName/ThisFirstNameIsTooLong/age/300/lastName/Gosling", null);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "ThisFirstNameIsTooLong", model.get("theFirstName"));
        assertEquals("wrong lastName", "Gosling", model.get("theLastName"));
        assertEquals("wrong age", new Integer(300), model.get("theAge"));
        Errors mcoErrors = (Errors) model.get("mcoErrors");
        assertEquals("wrong number of mco errors", 1, mcoErrors.getErrorCount());
        Errors acoErrors = (Errors) model.get("acoErrors");
        assertEquals("wrong number of aco errors", 1, acoErrors.getErrorCount());

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
