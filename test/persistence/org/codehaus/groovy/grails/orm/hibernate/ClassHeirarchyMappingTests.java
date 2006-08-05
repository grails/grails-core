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
package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.util.List;
import java.util.Properties;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import junit.framework.TestCase;

/**
 * Test a class heirarchy mapping with table-per-heirachy persistence
 * 
 * @author Graeme Rocher
 * @since 0.2
 */
public class ClassHeirarchyMappingTests extends TestCase {

	private DefaultGrailsApplication grailsApplication;
	private SessionFactory sessionFactory;
	private Session hibSession;
	private GroovyClassLoader gcl;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		this.gcl = new GroovyClassLoader();
		
		gcl.parseClass("class Car { Long id;Long version;String type;}\n" +
						"class Alpha extends Car { }\n" +
						"class Fiat extends Car { }\n" +
						"class Ferrari extends Car { }");
		
        grailsApplication = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);


        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration();
        config.setGrailsApplication(this.grailsApplication);
        Properties props = new Properties();
        props.put("hibernate.connection.username","sa");
        props.put("hibernate.connection.password","");
        props.put("hibernate.connection.url","jdbc:hsqldb:mem:grailsDB");
        props.put("hibernate.connection.driver_class","org.hsqldb.jdbcDriver");
        props.put("hibernate.dialect","org.hibernate.dialect.HSQLDialect");
        props.put("hibernate.hbm2ddl.auto","create-drop");

        //props.put("hibernate.hbm2ddl.auto","update");
        config.setProperties(props);
        //originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(gcl);
        this.sessionFactory = config.buildSessionFactory();



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            this.hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if(TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
		    SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(this.sessionFactory);
		    Session s = holder.getSession(); 
		    s.flush();
		    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
		    SessionFactoryUtils.releaseSession(s, this.sessionFactory);				
		}
	}

	
	public void testPolymorphicQuery() throws Exception {
		GroovyObject car = (GroovyObject)BeanUtils.instantiateClass(grailsApplication.getGrailsDomainClass("Car").getClazz());
		GroovyObject alpha = (GroovyObject)BeanUtils.instantiateClass(grailsApplication.getGrailsDomainClass("Alpha").getClazz());		
		GroovyObject fiat = (GroovyObject)BeanUtils.instantiateClass(grailsApplication.getGrailsDomainClass("Fiat").getClazz());
		GroovyObject ferrari = (GroovyObject)BeanUtils.instantiateClass(grailsApplication.getGrailsDomainClass("Ferrari").getClazz());
		
		fiat.setProperty("type", "cheap");
		alpha.setProperty("type", "luxury");
		ferrari.setProperty("type", "luxury");
		
		alpha.invokeMethod("save", new Object[0]);
		fiat.invokeMethod("save", new Object[0]);
		ferrari.invokeMethod("save", new Object[0]);
		
		// test polymorphic query
		List cars = (List)car.getMetaClass().invokeStaticMethod(car, "findAll", new Object[]{"from Car as c where c.type='luxury'"});
		assertEquals(2,cars.size());

		// Check that queries on specific sub-classes work correctly.
		cars = (List)fiat.getMetaClass().invokeStaticMethod(fiat, "findAll", new Object[0]);
		assertEquals(1, cars.size());        

    }
}

