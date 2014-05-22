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
package org.codehaus.groovy.grails.commons;

import grails.web.CamelCaseUrlConverter;
import grails.web.HyphenatedUrlConverter;
import grails.web.UrlConverter;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.support.MockApplicationContext;

/**
 * @author Steven Devijver
 */
public class DefaultGrailsControllerClass2Tests extends TestCase {

    public DefaultGrailsControllerClass2Tests() {
        super();
    }

    public DefaultGrailsControllerClass2Tests(String name) {
        super(name);
    }

    public void testDefaultGrailsControllerClassURIs() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        Class<?> clazz = cl.parseClass("class OverviewController { }");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();

        assertEquals(2, grailsClass.getURIs().length);
    }

    public void testDefaultGrailsControllerViewNames() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { def action = { return null }; } ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("Test", grailsClass.getName());
        assertEquals("TestController", grailsClass.getFullName());
        assertEquals("/test/action", grailsClass.getViewByURI("/test/action"));
        assertEquals("action",grailsClass.getMethodActionName("/test"));
        assertEquals("action",grailsClass.getMethodActionName("/test/action"));
        assertEquals(4, grailsClass.getURIs().length);
        assertTrue(grailsClass.mapsToURI("/test"));
        assertTrue(grailsClass.mapsToURI("/test/action"));
        assertTrue(grailsClass.mapsToURI("/test/action/**"));
    }

    public void testDefaultGrailsControllerViewNamesForHyphenatedUrls() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { def someAction = { return null }; } ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass, new HyphenatedUrlConverter());
        grailsClass.initialize();
        assertEquals("Test", grailsClass.getName());
        assertEquals("TestController", grailsClass.getFullName());
        assertEquals("/test/someAction", grailsClass.getViewByURI("/test/some-action"));
    }

    public void testDefaultGrailsControllerDefaultActionViewNamesForHyphenatedUrls() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static defaultAction = 'someAction'; def index() {}; def someAction = {}; } ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass, new HyphenatedUrlConverter());
        grailsClass.initialize();
        assertEquals("MyTest", grailsClass.getName());
        assertEquals("MyTestController", grailsClass.getFullName());
        assertEquals("/myTest/someAction", grailsClass.getViewByName("someAction"));
        assertEquals("/myTest/someAction", grailsClass.getViewByURI("/my-test/some-action"));
        assertEquals("/myTest/someAction", grailsClass.getViewByURI("/my-test/"));
        assertEquals("/myTest/index", grailsClass.getViewByURI("/my-test/index"));
    }

    public void testDefaultActionOnScaffoldedControllers() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static scaffold = true; def test() {}} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("index", grailsClass.getDefaultAction());

        clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static scaffold = true; def test =  {}} ");
        grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("index", grailsClass.getDefaultAction());

        clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static scaffold = true; def one() {}; def two(){};} ");
        grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("index", grailsClass.getDefaultAction());

        clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static scaffold = true; def one = {}; def two ={};} ");
        grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("index", grailsClass.getDefaultAction());

        clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MyTestController { static scaffold = true; def one(){}; def two ={};} ");
        grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("index", grailsClass.getDefaultAction());
    }

    public void testMappingToControllerBeginningWith2UpperCaseLetters() {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class MYdemoController { def action = { return null }; } ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        assignGrailsApplication(cl, grailsClass);
        grailsClass.initialize();
        assertEquals("MYdemo", grailsClass.getName());
        assertEquals("MYdemoController", grailsClass.getFullName());
        assertTrue(grailsClass.mapsToURI("/MYdemo"));
        assertTrue(grailsClass.mapsToURI("/MYdemo/action"));
        assertTrue(grailsClass.mapsToURI("/MYdemo/action/**"));
    }

    public void testInterceptors() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { \n" +
                                        "def beforeInterceptor = [action:this.&before,only:'list']\n" +
                                        "def before() { return 'success' }\n" +
                                        "def list = { return 'test' }\n " +
                                        "} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        GroovyObject controller = (GroovyObject)grailsClass.newInstance();

        assertTrue(grailsClass.isInterceptedBefore(controller,"list"));
        assertFalse(grailsClass.isInterceptedAfter(controller,"list"));

        Closure<?> bi = grailsClass.getBeforeInterceptor(controller);
        assertNotNull(bi);
        assertEquals("success", bi.call());
        assertNull(grailsClass.getAfterInterceptor(controller));

        clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class AfterController { \n" +
                "def afterInterceptor = [action:this.&before,except:'list']\n" +
                "def after() { return 'success' }\n" +
                "def list = { return 'test' }\n " +
                "def save = { return 'test' }\n " +
            "} ");

        grailsClass = new DefaultGrailsControllerClass(clazz);
        controller = (GroovyObject)grailsClass.newInstance();

        assertFalse(grailsClass.isInterceptedAfter(controller,"list"));
        assertTrue(grailsClass.isInterceptedAfter(controller,"save"));
    }

    public void testBeforeInterceptorWithNoExcept() {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { \n" +
                                        "def beforeInterceptor = [action:this.&before]\n" +
                                        "def before() { return 'success' }\n" +
                                        "def list = { return 'test' }\n " +
                                        "def show = { return 'test' }\n " +
                                        "} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        GroovyObject controller = (GroovyObject)grailsClass.newInstance();

        assertTrue(grailsClass.isInterceptedBefore(controller,"list"));
        assertTrue(grailsClass.isInterceptedBefore(controller,"show"));
    }

    public void testAllowedMethods() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { \n" +
                "static def allowedMethods = [actionTwo:'POST', actionThree:['POST', 'PUT', 'PATCH']]\n" +
                "def actionOne = { return 'test' }\n " +
                "def actionTwo = { return 'test' }\n " +
                "def actionThree = { return 'test' }\n " +
        "} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        GroovyObject controller = (GroovyObject)grailsClass.newInstance();

        assertTrue("actionOne should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionOne"));
        assertTrue("actionOne should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionOne"));
        assertTrue("actionOne should have accepted a PATCH", grailsClass.isHttpMethodAllowedForAction(controller, "PATCH", "actionOne"));
        assertTrue("actionOne should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionOne"));
        assertTrue("actionOne should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionOne"));

        assertFalse("actionTwo should not have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionTwo"));
        assertFalse("actionTwo should not have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionTwo"));
        assertFalse("actionTwo should not have accepted a PATCH", grailsClass.isHttpMethodAllowedForAction(controller, "PATCH", "actionTwo"));
        assertFalse("actionTwo should not have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionTwo"));
        assertTrue("actionTwo should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionTwo"));

        assertFalse("actionThree should not have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionThree"));
        assertTrue("actionThree should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionThree"));
        assertTrue("actionThree should have accepted a PATCH", grailsClass.isHttpMethodAllowedForAction(controller, "PATCH", "actionThree"));
        assertFalse("actionThree should not have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionThree"));
        assertTrue("actionThree should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionThree"));
    }

    public void testAllowedMethodsWithNoDefinedRestrictions() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { \n" +
                "def actionOne = { return 'test' }\n " +
                "def actionTwo = { return 'test' }\n " +
        "} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        GroovyObject controller = (GroovyObject)grailsClass.newInstance();

        assertTrue("actionOne should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionOne"));
        assertTrue("actionOne should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionOne"));
        assertTrue("actionOne should have accepted a PATCH", grailsClass.isHttpMethodAllowedForAction(controller, "PATCH", "actionOne"));
        assertTrue("actionOne should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionOne"));
        assertTrue("actionOne should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionOne"));

        assertTrue("actionTwo should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionTwo"));
        assertTrue("actionTwo should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionTwo"));
        assertTrue("actionTwo should have accepted a PATCH", grailsClass.isHttpMethodAllowedForAction(controller, "PATCH", "actionTwo"));
        assertTrue("actionTwo should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionTwo"));
        assertTrue("actionTwo should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionTwo"));
    }

    public void testInterceptorCloning() throws Exception {
        GroovyClassLoader cl = new GrailsAwareClassLoader();
        Class<?> clazz = cl.parseClass("@grails.artefact.Artefact(\"Controller\") class TestController { \n" +
                                        "def someproperty='testvalue'\n" +
                                        "static def beforeInterceptor = { someproperty }\n" +
                                        "def list = { return 'test' }\n " +
                                        "} ");
        GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
        GroovyObject controller = (GroovyObject)grailsClass.newInstance();

        assertTrue(grailsClass.isInterceptedBefore(controller,"list"));

        Closure<?> bi = grailsClass.getBeforeInterceptor(controller);
        assertNotNull(bi);
        assertEquals("testvalue", bi.call());
    }

    private void assignGrailsApplication(GroovyClassLoader cl,
            GrailsControllerClass grailsClass, UrlConverter urlConverter) {
        GrailsApplication ga = new DefaultGrailsApplication(cl.getLoadedClasses(), cl);

        MockApplicationContext ctx = new MockApplicationContext();
        ctx.registerMockBean(UrlConverter.BEAN_NAME, urlConverter);
        ga.setMainContext(ctx);
        ga.initialise();
        grailsClass.setGrailsApplication(ga);
    }

    private void assignGrailsApplication(GroovyClassLoader cl, GrailsControllerClass grailsClass) {
        assignGrailsApplication(cl, grailsClass, new CamelCaseUrlConverter());
    }
}
