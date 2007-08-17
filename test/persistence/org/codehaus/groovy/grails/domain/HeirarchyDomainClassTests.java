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
package org.codehaus.groovy.grails.domain;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;
/**
 * Tests that class heirarchies get calculated appropriately
 *
 * @author Graeme Rocher
 * @since 0.2
 * 
 * 
 */
public class HeirarchyDomainClassTests extends TestCase {

	public void testClassHeirarchy() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		
		gcl.parseClass("class Super { Long id;Long version;}\n" +
						"class Sub extends Super { }\n" +
						"class Sub2 extends Sub { }");
		
		GrailsApplication ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
        ga.initialise();

        GrailsDomainClass gdc1 = (GrailsDomainClass) ga.getArtefact(DomainClassArtefactHandler.TYPE, "Super");
		assertNotNull(gdc1);
		assertTrue(gdc1.isRoot());
		assertEquals(2,gdc1.getSubClasses().size());
		assertFalse(gdc1.getPropertyByName("id").isInherited());
		
		GrailsDomainClass gdc2 = (GrailsDomainClass) ga.getArtefact(DomainClassArtefactHandler.TYPE, "Sub");
		
		assertFalse(gdc2.isRoot());
		assertEquals(1,gdc2.getSubClasses().size());
		assertTrue(gdc2.getPropertyByName("id").isInherited());
		
		GrailsDomainClass gdc3 = (GrailsDomainClass) ga.getArtefact(DomainClassArtefactHandler.TYPE, "Sub2");
		
		assertFalse(gdc3.isRoot());
		assertEquals(0,gdc3.getSubClasses().size());
		assertTrue(gdc3.getPropertyByName("id").isInherited());
		
		
	}
}
