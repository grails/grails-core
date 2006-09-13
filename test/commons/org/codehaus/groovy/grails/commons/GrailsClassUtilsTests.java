/* Copyright 2004-2005 the original author or authors.
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

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Graeme Rocher
 * @since 15-Feb-2006
 */
public class GrailsClassUtilsTests extends TestCase {

        public void testGetNaturalName() throws Exception {
            assertEquals("First Name", GrailsClassUtils.getNaturalName("firstName"));
            assertEquals("URL", GrailsClassUtils.getNaturalName("URL"));
            assertEquals("Local URL", GrailsClassUtils.getNaturalName("localURL"));
            assertEquals("URL local", GrailsClassUtils.getNaturalName("URLlocal"));
        }
        
        public void testIsDomainClass() throws Exception {
    		GroovyClassLoader gcl = new GroovyClassLoader();
    		
    		Class c = gcl.parseClass("class Test { Long id;Long version;}\n" );
    		
        	assertTrue(GrailsClassUtils.isDomainClass(c));
        }
        
        public void testIsController() throws Exception {
    		GroovyClassLoader gcl = new GroovyClassLoader();
    		
    		Class c = gcl.parseClass("class TestController { }\n" );
    		
        	assertTrue(GrailsClassUtils.isControllerClass(c));
        }
        
        public void testIsTagLib() throws Exception {
    		GroovyClassLoader gcl = new GroovyClassLoader();
    		
    		Class c = gcl.parseClass("class TestTagLib { }\n" );
    		
        	assertTrue(GrailsClassUtils.isTagLibClass(c));
        }                
        
        public void testIsBootStrap() throws Exception {
    		GroovyClassLoader gcl = new GroovyClassLoader();
    		
    		Class c = gcl.parseClass("class TestBootStrap { }\n" );
    		
        	assertTrue(GrailsClassUtils.isBootstrapClass(c));
        }

        public void testConvertCollectionToArray() throws Exception {
            Collection c = new ArrayList();
            c.add("one");
            c.add("two");

            Object[] a = GrailsClassUtils.collectionToObjectArray(c);
            assertNotNull(a);
            assertEquals(2,a.length);
            assertEquals("one",a[0]);
            assertEquals("two",a[1]);

            a = GrailsClassUtils.collectionToObjectArray(null);
            assertNotNull(a);
            assertEquals(0,a.length);
        }
}
