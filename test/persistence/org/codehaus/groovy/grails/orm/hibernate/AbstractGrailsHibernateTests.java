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

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import groovy.util.GroovyTestCase;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Log4jConfigurer;
import org.springframework.web.context.WebApplicationContext;


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
public abstract class AbstractGrailsHibernateTests extends GroovyTestCase {

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

         Log4jConfigurer.initLogging("file:./test/persistence/log4j.properties");
        ExpandoMetaClass.enableGlobally();
        PluginManagerHolder.setPluginManager(null);

        onSetUp();

        ConfigObject config = new ConfigSlurper().parse("hibernate.cache.use_second_level_cache=true\n" +
                "hibernate.cache.use_query_cache=true\n" +
                "hibernate.cache.provider_class='org.hibernate.cache.OSCacheProvider'\n" +
                "dataSource {\n" +
                "dbCreate = \"create-drop\" \n" +
                "url = \"jdbc:hsqldb:mem:devDB\"\n" +
                "pooling = false                          \n" +
                "driverClassName = \"org.hsqldb.jdbcDriver\"\t\n" +
                "username = \"sa\"\n" +
                "password = \"\"\n" +
                "}");
        ConfigurationHolder.setConfig(config);
        for (int i = 0; i < gcl.getLoadedClasses().length; i++) {
            Class aClass = gcl.getLoadedClasses()[i];
            GroovySystem.getMetaClassRegistry().removeMetaClass(aClass);
        }
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
        ApplicationHolder.setApplication(ga);

        MockApplicationContext mc = new MockApplicationContext();
        mc.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
        mc.registerMockBean("messageSource", new StaticMessageSource());
        mc.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
       
        GrailsRuntimeConfigurator grc = new GrailsRuntimeConfigurator(ga, mc);
        this.applicationContext = grc.configure(new MockServletContext());
        this.sessionFactory = (SessionFactory)this.applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN);

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
    protected void onSetUp() throws Exception {}


    protected final void tearDown() throws Exception {
        ConfigurationHolder.setConfig(null);
        ApplicationHolder.setApplication(null);
        MetaClassRegistry metaClassRegistry = GroovySystem.getMetaClassRegistry();
        Class[] loadedClasses = gcl.getLoadedClasses();
        for (int i = 0; i < loadedClasses.length; i++) {
            Class loadedClass = loadedClasses[i];
            metaClassRegistry.removeMetaClass(loadedClass);
        }
        metaClassRegistry.setMetaClassCreationHandle(new MetaClassRegistry.MetaClassCreationHandle());
        PluginManagerHolder.setPluginManager(null);
        if(TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
		    SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(this.sessionFactory);
		    org.hibernate.Session s = holder.getSession();
		    //s.flush();
		    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
		    SessionFactoryUtils.releaseSession(s, this.sessionFactory);
		}
        onTearDown();


        gcl = null;
        ga = null;
        session = null;
        sessionFactory = null;
        applicationContext = null;
        super.tearDown();
    }

    /**
     * Called directly before destruction of the TestCase in the junit.framework.TestCase#tearDown() method
     */
    protected void onTearDown() throws Exception {}

}
