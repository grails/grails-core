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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.exceptions.InvalidPropertyException;
import org.codehaus.groovy.grails.commons.*;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

/**
 * @author Graeme Rocher
 * @since 06-Jul-2005
 */
public class DefaultGrailsDomainClassTests extends TestCase {
	GroovyClassLoader cl = new GroovyClassLoader();
	private Class relClass;
	private Class manyToManyClass;
	private Class oneToManyClass;
	private Class oneToOneClass;
	
	

	
	protected void setUp() throws Exception {
		Thread.currentThread().setContextClassLoader(cl);
		
		relClass = cl.loadClass( "org.codehaus.groovy.grails.domain.RelationshipsTest" );
        manyToManyClass = cl.loadClass( "org.codehaus.groovy.grails.domain.ManyToManyTest" );
        oneToManyClass = cl.loadClass( "org.codehaus.groovy.grails.domain.OneToManyTest2" );
        oneToOneClass = cl.loadClass( "org.codehaus.groovy.grails.domain.OneToOneTest" );

				
		super.setUp();
	}

    public void testPersistantPropertyInheritance() {
        Class topClass = cl.parseClass("class Top {\n" +
                "int id\n" +
                "int version\n" +
                "String topString\n" +
                "}");
        Class middleClass = cl.parseClass("class Middle extends Top {\n" +
                "String middleString\n" +
        "}");
        Class bottomClass = cl.parseClass("class Bottom extends Middle {\n" +
                "String bottomString\n" +
        "}");
        
        DefaultGrailsDomainClass topDomainClass = new DefaultGrailsDomainClass(topClass);
        DefaultGrailsDomainClass middleDomainClass = new DefaultGrailsDomainClass(middleClass);
        DefaultGrailsDomainClass bottomDomainClass = new DefaultGrailsDomainClass(bottomClass);
        
        assertEquals("bottom class had wrong number of persistent properties", 3, bottomDomainClass.getPersistantProperties().length);
        assertEquals("middle class had wrong number of persistent properties", 2, middleDomainClass.getPersistantProperties().length);
        assertEquals("top class had wrong number of persistent properties", 1, topDomainClass.getPersistantProperties().length);
        
        GrailsDomainClassProperty topStringProperty = topDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in topDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in topDomainClass", topStringProperty.isPersistent());
        
        topStringProperty = middleDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in middleDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in middleDomainClass", topStringProperty.isPersistent());
        
        GrailsDomainClassProperty middleStringProperty = middleDomainClass.getPropertyByName("middleString");
        assertNotNull("middleString property not found in middleDomainClass", middleStringProperty);
        assertTrue("middleString property was not persistent in middleDomainClass", middleStringProperty.isPersistent());
        
        topStringProperty = bottomDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in bottomDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in bottomDomainClass", topStringProperty.isPersistent());
        
        middleStringProperty = bottomDomainClass.getPropertyByName("middleString");
        assertNotNull("middleString property not found in bottomDomainClass", middleStringProperty);
        assertTrue("middleString property was not persistent in bottomDomainClass", middleStringProperty.isPersistent());
        
        GrailsDomainClassProperty bottomStringProperty = bottomDomainClass.getPropertyByName("bottomString");
        assertNotNull("bottomString property not found in bottomDomainClass", bottomStringProperty);
        assertTrue("bottomString property was not persistent in bottomDomainClass", bottomStringProperty.isPersistent());
    }
    
	public void testDefaultGrailsDomainClass()
		throws Exception {
	
		Class clazz = cl.parseClass("class UserTest { " +
                " int id; " +
                " int version; " +
                " List transients = [ \"age\" ]; " +
                " List optionals  = [ \"lastName\" ]; " +
                " String firstName; " +
                " String lastName; " +
                " java.util.Date age; " +
                "}");

		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(clazz);
				
		assertEquals("UserTest",domainClass.getName());
		
		assertNotNull(domainClass.getIdentifier());
		assertNotNull(domainClass.getVersion());
		assertTrue(domainClass.getIdentifier().isIdentity());
		
		try {
			domainClass.getPropertyByName("rubbish");
			fail("should throw exception");
		}
		catch(InvalidPropertyException ipe) {
			// expected
		}
		
		GrailsDomainClassProperty age = domainClass.getPropertyByName( "age" );
		assertNotNull(age);
		assertFalse(age.isPersistent());
		
		GrailsDomainClassProperty lastName = domainClass.getPropertyByName( "lastName" );
		assertNotNull(lastName);
		assertTrue(lastName.isOptional());
		
		GrailsDomainClassProperty firstName = domainClass.getPropertyByName( "firstName" );
		assertNotNull(firstName);
		assertFalse(firstName.isOptional());
		assertTrue(firstName.isPersistent());
		

		GrailsDomainClassProperty[] persistantProperties = domainClass.getPersistantProperties();
		for(int i = 0; i < persistantProperties.length;i++) {
			assertTrue(persistantProperties[i].isPersistent());
		}
	}

	public void testOneToManyRelationships()
		throws Exception {		
						
										
		GrailsApplication grailsApplication = new DefaultGrailsApplication(new Class[]{ relClass,oneToManyClass,oneToOneClass,manyToManyClass },cl);
		
		GrailsDomainClass c1dc = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            relClass.getName());
		GrailsDomainClass c2dc = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            oneToManyClass.getName());
		
		// test relationship property
		assertEquals( c1dc.getPropertyByName("ones").getOtherSide(), c2dc.getPropertyByName("other") );
		assertTrue( c1dc.getPropertyByName( "ones" ).isOneToMany() );
		assertTrue( c1dc.getPropertyByName( "ones" ).isPersistent() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isManyToMany() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isManyToOne() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isOneToOne() );		
		
		assertEquals( c2dc.getPropertyByName("other").getOtherSide(), c1dc.getPropertyByName("ones") );
		assertTrue( c2dc.getPropertyByName( "other" ).isPersistent() );
		assertTrue( c2dc.getPropertyByName( "other" ).isManyToOne() );	
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToMany() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToMany() );				
	}
	
	public void testCircularOneToManyRelationship() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class a = gcl.parseClass("class A { \n" +
									" Long id\n" +
									" Long version\n" +
									" def relatesToMany = [ children : A]\n" +
									" A parent\n" +
									" Set children\n" +
									"}");
		GrailsDomainClass dc = new DefaultGrailsDomainClass(a);
		GrailsDomainClass[] dcs = new GrailsDomainClass[1];
		dcs[0] =dc;
		Map domainMap = new HashMap();
		domainMap.put(dc.getFullName(),dc);
		GrailsDomainConfigurationUtil.configureDomainClassRelationships(dcs,domainMap);
		
		assertTrue(dc.getPropertyByName("children").isAssociation());
		assertTrue(dc.getPropertyByName("children").isOneToMany());
		assertTrue(dc.getPropertyByName("parent").isAssociation());
		assertTrue(dc.getPropertyByName("parent").isManyToOne());
		assertTrue(dc.getPropertyByName("children").getOtherSide().equals(dc.getPropertyByName("parent")));
		assertTrue(dc.getPropertyByName("parent").getOtherSide().equals(dc.getPropertyByName("children")));
	
	}
/*	
 * TODO: Re-instate test once many-to-manys are supported
 * public void testManyToManyRelationships()
		throws Exception {
		

		
		GrailsDomainClass c1dc = new DefaultGrailsDomainClass(relClass);
		GrailsDomainClass c2dc = new DefaultGrailsDomainClass(manyToManyClass);
		
		// test relationships
		assertTrue( c1dc.getPropertyByName( "manys" ).isPersistent() );
		assertTrue( c1dc.getPropertyByName( "manys" ).isManyToMany() );		
		assertFalse( c1dc.getPropertyByName( "manys" ).isOneToMany() );
		assertFalse( c1dc.getPropertyByName( "manys" ).isManyToOne() );
		assertFalse( c1dc.getPropertyByName( "manys" ).isOneToOne() );			
		
		assertTrue( c2dc.getPropertyByName( "manys" ).isPersistent() );
		assertTrue( c2dc.getPropertyByName( "manys" ).isManyToMany() );
		assertFalse( c2dc.getPropertyByName( "manys" ).isManyToOne() );
		assertFalse( c2dc.getPropertyByName( "manys" ).isOneToOne() );
		assertFalse( c2dc.getPropertyByName( "manys" ).isOneToMany() );		
	}*/
	
	public void testOneToOneRelationships() 
		throws Exception {
		GrailsDomainClass c1dc = new DefaultGrailsDomainClass(relClass);
		GrailsDomainClass c2dc = new DefaultGrailsDomainClass(oneToOneClass);
		
		// test relationships
		assertTrue( c1dc.getPropertyByName( "one" ).isPersistent() );
		assertTrue( c1dc.getPropertyByName( "one" ).isOneToOne() );	
		assertFalse( c1dc.getPropertyByName( "one" ).isManyToMany() );
		assertFalse( c1dc.getPropertyByName( "one" ).isManyToOne() );
		assertFalse( c1dc.getPropertyByName( "one" ).isOneToMany() );
		
		assertTrue( c2dc.getPropertyByName( "other" ).isPersistent() );
		assertTrue( c2dc.getPropertyByName( "other" ).isOneToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToMany() );
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToMany() );		
	}

}
