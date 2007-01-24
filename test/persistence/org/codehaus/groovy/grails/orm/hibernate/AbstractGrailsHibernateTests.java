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
package org.codehaus.groovy.grails.orm.hibernate;

import junit.framework.TestCase;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.context.support.StaticMessageSource;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;


/**
 * An abstract test harness that should be extended when testing Grails domain classes against Hibernate
 * persistence. Creates an in-memory database and boostraps the Grails environment before execution.
 * Implementors should use the GroovyClassLoader gcl field to parse or load whatever domain classes they want
 * to test inside the onSetUp() method.
 *
 * @author Graeme Rocher
 *
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 7:26:52 AM
 */
public abstract class AbstractGrailsHibernateTests extends TestCase {

    /**
     * A GroovyClassLoader instance
     */
    public GroovyClassLoader gcl = new GroovyClassLoader();
    /**
     * The GrailsApplication instance created during setup
     */
    public GrailsApplication ga;
    /**
     * A Hibernate SessionFactory created during setup
     */
    protected SessionFactory sessionFactory;
    /**
     * A Hibernate session that is bound to the current thread so that the Spring TransactionManager works correctly
     */
    protected Session session;
    protected WebApplicationContext applicationContext;

    protected final void setUp() throws Exception {
        super.setUp();

        onSetUp();

        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);


//        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration();
//        config.setGrailsApplication(this.ga);
//        Properties props = new Properties();
//        props.put("hibernate.connection.username","sa");
//        props.put("hibernate.connection.password","");
//        props.put("hibernate.connection.url","jdbc:hsqldb:mem:grailsDB");
//        props.put("hibernate.connection.driver_class","org.hsqldb.jdbcDriver");
//        props.put("hibernate.dialect","org.hibernate.dialect.HSQLDialect");
//        props.put("hibernate.hbm2ddl.auto","create-drop");
//        props.put("hibernate.log_sql","true");
//
//        //props.put("hibernate.hbm2ddl.auto","update");
//        config.setProperties(props);
//
//        //originalClassLoader = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(this.gcl);
//        this.sessionFactory = config.buildSessionFactory();
        MockApplicationContext mc = new MockApplicationContext();
        mc.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
        mc.registerMockBean("messageSource", new StaticMessageSource());
       
        GrailsRuntimeConfigurator grc = new GrailsRuntimeConfigurator(ga, mc);
        this.applicationContext = grc.configureDomainOnly();
        this.sessionFactory = (SessionFactory)this.applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN);
        GrailsHibernateUtil.configureDynamicMethods(applicationContext,ga);

        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            this.session = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(session));
        }


    }

    /**
     * Called directly before initialization of the TestCase in the junit.framework.TestCase#setUp() method.
     * This is where any classes should be created at runtime using the GroovyClassLoader gcl field's parseClass method.
     * Classes created here will then be passed to the GrailsApplication instance.
     *
     */
    protected abstract void onSetUp() throws Exception;


    protected final void tearDown() throws Exception {
        super.tearDown();
		if(TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
		    SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(this.sessionFactory);
		    org.hibernate.Session s = holder.getSession();
		    s.flush();
		    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
		    SessionFactoryUtils.releaseSession(s, this.sessionFactory);
		}
        onTearDown();
    }

    /**
     * Called directly before destruction of the TestCase in the junit.framework.TestCase#tearDown() method
     */
    protected abstract void onTearDown() throws Exception;

}
