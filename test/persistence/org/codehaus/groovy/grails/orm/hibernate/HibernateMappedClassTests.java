package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.ExpandoMetaClass;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.core.io.Resource;

/**

 */
public class HibernateMappedClassTests extends
		AbstractDependencyInjectionSpringContextTests implements GrailsApplicationAware {
    private GrailsApplication grailsApplication;
    private SessionFactory sessionFactory;


    /* (non-Javadoc)
      * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
      */
	protected String[] getConfigLocations() {
		return new String[] { "org/codehaus/groovy/grails/orm/hibernate/hibernate-mapped-class-tests.xml" };
	}

    protected void onTearDown() throws Exception {
        ConfigurationHolder.setConfig(null);
    }

    protected void onSetUp() throws Exception {
        ConfigObject config = new ConfigSlurper().parse("dataSource {\n" +
                "\t\t\tdbCreate = \"create-drop\" // one of 'create', 'create-drop','update'\n" +
                "\t\t\turl = \"jdbc:hsqldb:mem:devDB\" \n" +
                "\tpooled = false\n" +
                "\tdriverClassName = \"org.hsqldb.jdbcDriver\"\n" +
                "\tusername = \"sa\"\n" +
                "\tpassword = \"\"\n" +
                "}");

        ConfigurationHolder.setConfig(config);

        ExpandoMetaClass.enableGlobally();
        grailsApplication.initialise();


        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
        parent.registerMockBean("messageSource", new StaticMessageSource());
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
        GrailsRuntimeConfigurator configurator = new GrailsRuntimeConfigurator(grailsApplication,parent);
        ApplicationContext appCtx = configurator.configure( new MockServletContext( ));
        this.sessionFactory = (SessionFactory)appCtx.getBean("sessionFactory");



        if(!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
            Session hibSession = this.sessionFactory.openSession();
            TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(hibSession));
        }


        super.onSetUp();
    }

    public void testDynamicMethods() {
		HibernateMappedClass hmc = new HibernateMappedClass();
		hmc.setMyProp("somevalue");
		InvokerHelper.invokeMethod(hmc, "save", new Object[0]);
		String className = hmc.getClass().getName();
		hmc = (HibernateMappedClass)InvokerHelper.invokeStaticMethod(hmc.getClass(), "get", new Object[] { new Integer(1) });
	
		assertEquals("somevalue", hmc.getMyProp());
	}

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
