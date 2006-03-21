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
public class DefaultGrailsControllerClassTests extends TestCase {

	public DefaultGrailsControllerClassTests() {
		super();
	}

	public DefaultGrailsControllerClassTests(String arg0) {
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
		Class clazz = cl.parseClass("class TestController { @Property action = { return null }; } ");
		GrailsControllerClass grailsClass = new DefaultGrailsControllerClass(clazz);

        assertEquals("Test", grailsClass.getName());
		assertEquals("TestController", grailsClass.getFullName());
		assertEquals("/test/action", grailsClass.getViewByURI("/test/action"));
        assertEquals("action",grailsClass.getClosurePropertyName("/test"));
        assertEquals("action",grailsClass.getClosurePropertyName("/test/action"));
        assertEquals(3, grailsClass.getURIs().length);
        assertTrue(grailsClass.mapsToURI("/test"));
        assertTrue(grailsClass.mapsToURI("/test/action"));
    }

	public void testInterceptors() throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestController { \n" +
										"@Property beforeInterceptor = [action:this.&before,only:'list']\n" +
										"def before() { return 'success' }\n" +
										"@Property list = { return 'test' }\n " +										
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
				"@Property afterInterceptor = [action:this.&before,except:'list']\n" +
				"def after() { return 'success' }\n" +
				"@Property list = { return 'test' }\n " +
				"@Property save = { return 'test' }\n " +
			"} ");	
		
		grailsClass = new DefaultGrailsControllerClass(clazz);
		controller = (GroovyObject)grailsClass.newInstance();	
		
		assertFalse(grailsClass.isInterceptedAfter(controller,"list"));
		assertTrue(grailsClass.isInterceptedAfter(controller,"save"));
	}
}
