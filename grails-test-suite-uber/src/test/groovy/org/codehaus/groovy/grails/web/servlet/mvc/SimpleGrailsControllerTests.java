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

import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
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
        GrailsAwareClassLoader cl = new GrailsAwareClassLoader();
        Class<?> testControllerClass = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController {\n"+
                            " def test = {\n"+
                                "return [ \"test\" : \"123\" ]\n"+
                             "}\n" +
                        "}");

        Class<?> simpleControllerClass = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class SimpleController {\n"+
                " def test = {\n"+
                 "}\n" +
            "}");

        Class<?> restrictedControllerClass = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class RestrictedController {\n"+
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
        args.addGenericArgumentValue(new Class[]{testControllerClass,simpleControllerClass,restrictedControllerClass});
        args.addGenericArgumentValue(cl);
        MutablePropertyValues propValues = new MutablePropertyValues();

        BeanDefinition grailsApplicationBean = new RootBeanDefinition(DefaultGrailsApplication.class,args,propValues);
        localContext.registerBeanDefinition("grailsApplication", grailsApplicationBean);

        localContext.refresh();

        grailsApplication = (GrailsApplication)localContext.getBean("grailsApplication");
        GrailsRuntimeConfigurator rConfig = new GrailsRuntimeConfigurator(grailsApplication, localContext);

        MockServletContext servletContext = new MockServletContext();
        appCtx = (ConfigurableApplicationContext)rConfig.configure(servletContext);

        controller = (SimpleGrailsController)appCtx.getBean("mainSimpleController");

        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,appCtx);
        assertNotNull(appCtx);
        GrailsClass[] controllerArtefacts = grailsApplication.getArtefacts(ControllerArtefactHandler.TYPE);
        for (GrailsClass grailsClass : controllerArtefacts) {
            ((GrailsControllerClass)grailsClass).initialize();
        }
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
