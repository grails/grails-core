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

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public class DefaultGrailsControllerClass2Tests extends TestCase {

	public DefaultGrailsControllerClass2Tests() {
		super();
	}

	public DefaultGrailsControllerClass2Tests(String arg0) {
		super(arg0);
	}
	
	public void testDefaultGrailsControllerClassURIs() throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class OverviewController { }");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
		assertEquals(0, grailsClass.getURIs().length);
	}
	
	public void testDefaultGrailsControllerViewNames() throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { def action = { return null }; } ");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);

        assertEquals("Test", grailsClass.getName());
		assertEquals("TestController", grailsClass.getFullName());
		assertEquals("/test/action", grailsClass.getViewByURI("/test/action"));
        assertEquals("action",grailsClass.getClosurePropertyName("/test"));
        assertEquals("action",grailsClass.getClosurePropertyName("/test/action"));
        assertEquals(4, grailsClass.getURIs().length);
        assertTrue(grailsClass.mapsToURI("/test"));
        assertTrue(grailsClass.mapsToURI("/test/action"));
        assertTrue(grailsClass.mapsToURI("/test/action/**"));
    }
	
	public void testScaffoldedController() throws Exception {
		
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { def scaffold = Test.class } \nclass Test {  Long id\n Long version\n}");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);

        assertEquals("Test", grailsClass.getName());
		assertEquals("TestController", grailsClass.getFullName());
		assertEquals("/test/list", grailsClass.getViewByURI("/test/list"));
        assertEquals("index",grailsClass.getClosurePropertyName("/test"));
        assertEquals("create",grailsClass.getClosurePropertyName("/test/create"));
        assertTrue(grailsClass.mapsToURI("/test"));
        assertTrue(grailsClass.mapsToURI("/test/show"));
        assertTrue(grailsClass.mapsToURI("/test/show/**"));
        assertTrue(grailsClass.mapsToURI("/test/list"));
        assertTrue(grailsClass.mapsToURI("/test/list/**"));
        assertTrue(grailsClass.mapsToURI("/test/create"));
        assertTrue(grailsClass.mapsToURI("/test/create/**"));
        assertTrue(grailsClass.mapsToURI("/test/save"));
        assertTrue(grailsClass.mapsToURI("/test/save/**"));
        assertTrue(grailsClass.mapsToURI("/test/edit"));
        assertTrue(grailsClass.mapsToURI("/test/edit/**"));
        assertTrue(grailsClass.mapsToURI("/test/delete"));
        assertTrue(grailsClass.mapsToURI("/test/delete/**"));
        assertTrue(grailsClass.mapsToURI("/test/update"));
        assertTrue(grailsClass.mapsToURI("/test/update/**"));        
        assertEquals(18, grailsClass.getURIs().length);

	}

	public void testInterceptors() throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { \n" +
										"def beforeInterceptor = [action:this.&before,only:'list']\n" +
										"def before() { return 'success' }\n" +
										"def list = { return 'test' }\n " +										
									"} ");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
		GroovyObject controller = (GroovyObject)grailsClass.newInstance();
		
		
		assertTrue(grailsClass.isInterceptedBefore(controller,"list"));
		assertFalse(grailsClass.isInterceptedAfter(controller,"list"));
		
		Closure bi = grailsClass.getBeforeInterceptor(controller);
		assertNotNull(bi);
		assertEquals("success", bi.call());
		assertNull(grailsClass.getAfterInterceptor(controller));
		
		clazz = cl.parseClass("class AfterController { \n" +
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
        GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { \n" +
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
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { \n" +
				"def allowedMethods = [actionTwo:'POST', actionThree:['POST', 'PUT']]\n" +
				"def actionOne = { return 'test' }\n " +										
				"def actionTwo = { return 'test' }\n " +										
				"def actionThree = { return 'test' }\n " +										
		"} ");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
		GroovyObject controller = (GroovyObject)grailsClass.newInstance();
		
		assertTrue("actionOne should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionOne"));
		assertTrue("actionOne should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionOne"));
		assertTrue("actionOne should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionOne"));
		assertTrue("actionOne should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionOne"));
		
		assertFalse("actionTwo should not have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionTwo"));
		assertFalse("actionTwo should not have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionTwo"));
		assertFalse("actionTwo should not have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionTwo"));
		assertTrue("actionTwo should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionTwo"));
		
		assertFalse("actionThree should not have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionThree"));
		assertTrue("actionThree should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionThree"));
		assertFalse("actionThree should not have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionThree"));
		assertTrue("actionThree should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionThree"));
	}
	
	public void testAllowedMethodsWithNoDefinedRestrictions() throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { \n" +
				"def actionOne = { return 'test' }\n " +										
				"def actionTwo = { return 'test' }\n " +										
		"} ");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);
		GroovyObject controller = (GroovyObject)grailsClass.newInstance();
		
		assertTrue("actionOne should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionOne"));
		assertTrue("actionOne should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionOne"));
		assertTrue("actionOne should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionOne"));
		assertTrue("actionOne should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionOne"));
		
		assertTrue("actionTwo should have accepted a GET", grailsClass.isHttpMethodAllowedForAction(controller, "GET", "actionTwo"));
		assertTrue("actionTwo should have accepted a PUT", grailsClass.isHttpMethodAllowedForAction(controller, "PUT", "actionTwo"));
		assertTrue("actionTwo should have accepted a DELETE", grailsClass.isHttpMethodAllowedForAction(controller, "DELETE", "actionTwo"));
		assertTrue("actionTwo should have accepted a POST", grailsClass.isHttpMethodAllowedForAction(controller, "POST", "actionTwo"));
	}
}
