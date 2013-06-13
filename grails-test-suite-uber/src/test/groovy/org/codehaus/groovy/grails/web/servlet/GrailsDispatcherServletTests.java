/* Copyright 2004-2005 Graeme Rocher
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

import grails.web.CamelCaseUrlConverter;
import grails.web.HyphenatedUrlConverter;
import grails.web.UrlConverter;
import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Tests for handler mapping in the Grails dispatcher servlet.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsDispatcherServletTests extends TestCase {

    @SuppressWarnings("serial")
    public void testHandlerMapping() throws Exception {
        final MockApplicationContext appCtx = new MockApplicationContext();

        try {
            System.setProperty("grails.env", "development");
            appCtx.registerMockBean("localeInterceptor", new LocaleChangeInterceptor());
            appCtx.registerMockBean("grailsUrlConverter", new HyphenatedUrlConverter());

            SimpleGrailsController controller = new SimpleGrailsController();
            appCtx.registerMockBean("mainSimpleController", controller);
            GrailsControllerHandlerMapping handlerMapping = new GrailsControllerHandlerMapping();

            appCtx.registerMockBean("controllerHandlerMappings", handlerMapping);

            GrailsDispatcherServlet dispatcherServlet = new GrailsDispatcherServlet() {
                @Override
                protected WebApplicationContext initWebApplicationContext() {
                    initStrategies(appCtx);
                    return appCtx;
                }
            };

            GroovyClassLoader cl = new GrailsAwareClassLoader();
            cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController {" +
                    "def action = {} " +
                    "}");
            final DefaultGrailsApplication grailsApplication = new DefaultGrailsApplication(cl.getLoadedClasses(), cl);
            MockApplicationContext mainContext = new MockApplicationContext();
            mainContext.registerMockBean(UrlConverter.BEAN_NAME, new CamelCaseUrlConverter());
            grailsApplication.setMainContext(mainContext);
            grailsApplication.initialise();

            handlerMapping.setGrailsApplication(grailsApplication);
            handlerMapping.setApplicationContext(appCtx);
            grailsApplication.setMainContext(appCtx);
            dispatcherServlet.setApplication(grailsApplication);
            dispatcherServlet.init(new MockServletConfig(new MockServletContext()));
            GrailsControllerClass controllerClass = (GrailsControllerClass) grailsApplication.getArtefact(ControllerArtefactHandler.TYPE, "TestController");
            controllerClass.initialize();

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/action");
            HandlerExecutionChain executionChain = dispatcherServlet.getHandler(request);

            assertNotNull(executionChain);
            assertEquals(1, executionChain.getInterceptors().length);
            for (int i = 0; i < executionChain.getInterceptors().length; i++) {
                assertNotNull(executionChain.getInterceptors()[i]);
            }

            request = new MockHttpServletRequest("GET", "/test/action/1");
            executionChain = dispatcherServlet.getHandler(request);

            assertNotNull(executionChain);

            request = new MockHttpServletRequest("GET", "/test/action/1/param/value");
            executionChain = dispatcherServlet.getHandler(request);

            assertNotNull(executionChain);

            request = new MockHttpServletRequest("GET", "/context/rubbish/action");

            executionChain = dispatcherServlet.getHandler(request);
            assertNull(executionChain);
        } finally {
            System.setProperty("grails.env", "");
        }
    }

    public void testGetPathWithinApplication() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/grails/book/show/1.dispatch");
        request.setServletPath("/grails");

        GrailsUrlPathHelper pathHelper = new GrailsUrlPathHelper();
        assertEquals("/book/show/1",pathHelper.getPathWithinApplication(request));
    }
}
