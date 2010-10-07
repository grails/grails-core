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

import grails.util.GrailsWebUtil;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
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

/**
 * @author Steven Devijver
 */
public class SimpleGrailsControllerTests extends TestCase {

    protected GrailsApplication grailsApplication;
    protected SimpleGrailsController controller;
    private GenericApplicationContext localContext;
    private ConfigurableApplicationContext appCtx;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();
        GroovyClassLoader cl = new GroovyClassLoader();
        cl.parseClass("class MyCommandObject {\n" +
                "String firstName\n" +
                "String lastName\n" +
                "static constraints = {\n" +
                "  firstName(maxSize:10)\n" +
                "  lastName(maxSize:10)" +
                "}\n" +
                "}");
        cl.parseClass("class DateStructCommandObject {\n" +
                "Date birthday\n" +
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
        Class<?> testControllerClass = cl.parseClass("class TestController {\n"+
                            " Closure test = {\n"+
                                "return [ \"test\" : \"123\" ]\n"+
                             "}\n" +
                             "def codatestruct = { DateStructCommandObject dsco ->\n" +
                             "[theDate:dsco.birthday, validationErrors:dsco.errors]" +
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

        Class<?> simpleControllerClass = cl.parseClass("class SimpleController {\n"+
                " Closure test = {\n"+
                 "}\n" +
            "}");

        Class<?> noViewControllerClass = cl.parseClass("class NoViewController {\n"+
                " Closure test = {\n"+
                  "request, response ->\n" +
                  "new grails.util.OpenRicoBuilder(response).ajax { element(id:\"test\") { } };\n" +
                  "return null;\n" +
                 "}\n" +
            "}");

        Class<?> restrictedControllerClass = cl.parseClass("class RestrictedController {\n"+
                "static allowedMethods=[action1:'POST', action3:['PUT', 'DELETE'], action4: 'pOsT', action5: ['pUt', 'DeLeTe']]\n" +
                "def action1 = {}\n" +
                "def action2 = {}\n" +
                "def action3 = {}\n" +
                "def action4 = {}\n" +
                "def action5 = {}\n" +
        "}");

        Thread.currentThread().setContextClassLoader(cl);

        localContext = new GenericApplicationContext();

        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addGenericArgumentValue(new Class[]{testControllerClass,simpleControllerClass,noViewControllerClass,restrictedControllerClass});
        args.addGenericArgumentValue(cl);
        MutablePropertyValues propValues = new MutablePropertyValues();

        BeanDefinition grailsApplicationBean = new RootBeanDefinition(DefaultGrailsApplication.class,args,propValues);
        localContext.registerBeanDefinition( "grailsApplication", grailsApplicationBean );

        localContext.refresh();

        grailsApplication = (GrailsApplication)localContext.getBean("grailsApplication");
        ApplicationHolder.setApplication(grailsApplication);
        GrailsRuntimeConfigurator rConfig = new GrailsRuntimeConfigurator(grailsApplication, localContext);

        MockServletContext servletContext = new MockServletContext();
        appCtx = (ConfigurableApplicationContext)rConfig.configure(servletContext);

        controller = (SimpleGrailsController)appCtx.getBean("mainSimpleController");

        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,appCtx);
        controller.setServletContext(servletContext);
        assertNotNull(appCtx);
        super.setUp();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        ExpandoMetaClass.disableGlobally();
    }

    private ModelAndView execute(String uri,String controllerName, String actionName, Properties parameters) throws Exception {
        return execute(uri,controllerName, actionName, parameters, "GET");
    }

    @SuppressWarnings("rawtypes")
    private ModelAndView execute(String uri,String controllerName, String actionName, Properties parameters, String requestMethod) throws Exception {
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest((GrailsWebApplicationContext)this.appCtx);
        webRequest.setControllerName(controllerName);
        webRequest.setActionName(actionName);
        MockHttpServletRequest request = (MockHttpServletRequest)webRequest.getCurrentRequest();
        if (uri.indexOf('?') > -1) {
            request.setRequestURI(uri.substring(0,uri.indexOf('?')));
        }
        else {
            request.setRequestURI(uri);
        }
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
        ModelAndView modelAndView = execute("/test/test","test","test", null);
        assertNotNull(modelAndView);
    }

    @SuppressWarnings("rawtypes")
    public void testCommandObjectDateStruct() throws Exception {
        Properties props = new Properties();
        props.put("birthday", "struct");
        props.put("birthday_day", "03");
        props.put("birthday_month", "05");
        props.put("birthday_year", "1973");

        ModelAndView modelAndView = execute("/test/codatestruct","test", "codatestruct", props);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of errors", 0, validationErrors.getErrorCount());
        Date birthDate = (Date) model.get("theDate");
        assertNotNull("null birthday", birthDate);
        Calendar expectedCalendar = Calendar.getInstance();
        expectedCalendar.clear();
        expectedCalendar.set(Calendar.DAY_OF_MONTH, 3);
        expectedCalendar.set(Calendar.MONTH, Calendar.MAY);
        expectedCalendar.set(Calendar.YEAR, 1973);
        Date expectedDate = expectedCalendar.getTime();
        assertEquals("wrong date", expectedDate, birthDate);
    }

    @SuppressWarnings("rawtypes")
    public void testUnconstrainedCommandObject() throws Exception {
        Properties props = new Properties();
        props.put("firstName", "James");
        ModelAndView modelAndView = execute("/test/unconstrainedcommandobject?id=1&firstName=James","test","unconstrainedcommandobject", props);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "James", model.get("theFirstName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of errors", 0, validationErrors.getErrorCount());
    }

    @SuppressWarnings("rawtypes")
    public void testSingleCommandObjectValidationSuccess() throws Exception {
        Properties props = new Properties();
        props.put("firstName", "James");
        props.put("lastName", "Gosling");

        ModelAndView modelAndView = execute("/test/singlecommandobject","test","singlecommandobject", props);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "James", model.get("theFirstName"));
        assertEquals("wrong lastName", "Gosling", model.get("theLastName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of errors", 0, validationErrors.getErrorCount());
    }

    @SuppressWarnings("rawtypes")
    public void testMultipleCommandObjectValidationSuccess() throws Exception {

        Properties props = new Properties();
        props.put("firstName", "James");
        props.put("lastName", "Gosling");
        props.put("age", "30");

        ModelAndView modelAndView = execute("/test/multiplecommandobjects","test","multiplecommandobjects", props);
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

    @SuppressWarnings("rawtypes")
    public void testSingleCommandObjectValidationFailure() throws Exception {
        Properties props = new Properties();
        props.put("firstName", "ThisFirstNameIsTooLong");
        props.put("lastName", "ThisLastNameIsTooLong");

        ModelAndView modelAndView = execute("/test/singlecommandobject","test", "singlecommandobject", props);
        assertNotNull("null modelAndView", modelAndView);
        Map model = modelAndView.getModelMap();
        assertEquals("wrong firstName", "ThisFirstNameIsTooLong", model.get("theFirstName"));
        assertEquals("wrong lastName", "ThisLastNameIsTooLong", model.get("theLastName"));
        Errors validationErrors = (Errors) model.get("validationErrors");
        assertEquals("wrong number of mcoErrors", 2, validationErrors.getErrorCount());
    }

    @SuppressWarnings("rawtypes")
    public void testMultipleCommandObjectValidationFailure() throws Exception {
        Properties props = new Properties();
        props.put("firstName", "ThisFirstNameIsTooLong");
        props.put("lastName", "Gosling");
        props.put("age", "300");

        ModelAndView modelAndView = execute("/test/multiplecommandobjects", "test","multiplecommandobjects", props);
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
        GrailsClass controllerClass = grailsApplication.getArtefact(ControllerArtefactHandler.TYPE,"RestrictedController");
        assertNotNull(controllerClass);
        assertResponseStatusCode("restricted","action1","/restricted/action1", "GET", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action1","/restricted/action1", "PUT", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action1","/restricted/action1", "POST", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action1","/restricted/action1", "DELETE", HttpServletResponse.SC_METHOD_NOT_ALLOWED);

        assertResponseStatusCode("restricted","action2","/restricted/action2", "GET", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action2","/restricted/action2", "PUT", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action2","/restricted/action2", "POST", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action2","/restricted/action2", "DELETE", HttpServletResponse.SC_OK);

        assertResponseStatusCode("restricted","action3","/restricted/action3", "GET", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action3","/restricted/action3", "PUT", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action3","/restricted/action3", "POST", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action3","/restricted/action3", "DELETE", HttpServletResponse.SC_OK);

        assertResponseStatusCode("restricted","action4","/restricted/action4", "GET", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action4","/restricted/action4", "PUT", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action4","/restricted/action4", "POST", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action4","/restricted/action4", "DELETE", HttpServletResponse.SC_METHOD_NOT_ALLOWED);

        assertResponseStatusCode("restricted","action5","/restricted/action5", "GET", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action5","/restricted/action5", "PUT", HttpServletResponse.SC_OK);
        assertResponseStatusCode("restricted","action5","/restricted/action5", "POST", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertResponseStatusCode("restricted","action5","/restricted/action5", "DELETE", HttpServletResponse.SC_OK);
    }

    private void assertResponseStatusCode(String controllerName, String actionName,String uri, String httpMethod, int expectedStatusCode) throws Exception {
        execute(uri,controllerName, actionName, null, httpMethod);
        GrailsWebRequest gwr = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        MockHttpServletResponse res = (MockHttpServletResponse) gwr.getCurrentResponse();

        assertEquals(expectedStatusCode, res.getStatus());
    }
}
