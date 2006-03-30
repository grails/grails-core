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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Graeme Rocher
 * @since 08-Jul-2005
 */
public class CircularRelationshipTests extends AbstractDependencyInjectionSpringContextTests  {
	GroovyClassLoader cl = new GroovyClassLoader();
	
	
	protected GrailsApplication grailsApplication = null;
	protected SessionFactory sessionFactory = null;
	

	protected String[] getConfigLocations() {
		return new String[] { "org/codehaus/groovy/grails/orm/hibernate/grails-hibernate-configuration-tests.xml" };
	}
	
	
	protected void onSetUp() throws Exception {
		Thread.currentThread().setContextClassLoader(cl);
		cl.loadClass("org.codehaus.groovy.grails.domain.CircularRelationship");
		Class[] loadedClasses = cl.getLoadedClasses();
		grailsApplication = new DefaultGrailsApplication(loadedClasses,cl);
		

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
		Thread.currentThread().setContextClassLoader(this.cl);		
		this.sessionFactory = config.buildSessionFactory();
		
		
			
		if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
			Session hibSession = this.sessionFactory.openSession();
		    TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
		}		
		
	}	


	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onTearDown()
	 */
	protected void onTearDown() throws Exception {
		if(TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
		    SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(this.sessionFactory);
		    Session s = holder.getSession(); 
		    s.flush();
		    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
		    SessionFactoryUtils.releaseSession(s, this.sessionFactory);				
		}
	}

	
	public void testHibernateConfiguration() throws Exception {		
		assertNotNull(this.sessionFactory);
		
		GrailsDomainClass[] domainClasses = grailsApplication.getGrailsDomainClasses();
		assertEquals(1,domainClasses.length);
		
		
		//HibernateTemplate template = new HibernateTemplate(this.sessionFactory);
		GroovyObject obj = (GroovyObject)grailsApplication.getGrailsDomainClass("org.codehaus.groovy.grails.domain.CircularRelationship").newInstance();
		GroovyObject child = (GroovyObject)grailsApplication.getGrailsDomainClass("org.codehaus.groovy.grails.domain.CircularRelationship").newInstance();
		assertNotNull(obj);
		
		child.setProperty("parent",obj);
		Set children = new HashSet();
		children.add(child);
		obj.setProperty("children",children);
		obj.invokeMethod("save",new Object[0]);
		
		
		
		obj = (GroovyObject)obj.getMetaClass().invokeStaticMethod(obj,"get",new Object[] { new Long(1) });
		assertNotNull(obj.getProperty("children"));
		assertEquals(1,((Set)obj.getProperty("children")).size());
	}
	
}
