package org.codehaus.groovy.grails.commons.spring;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.codehaus.groovy.grails.support.ClassEditor;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.Properties;

public class GrailsRuntimeConfiguratorTests extends TestCase {

	
    /* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();
        ConfigurationHolder.setConfig(null);

        super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
        ExpandoMetaClass.disableGlobally();
		super.tearDown();
	}

	/*
      * Test method for 'org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator.configure()'
      */
    public void testConfigure() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass("class Test { Long id; Long version; }");
        
        Class sc = gcl.parseClass("class TestService { boolean transactional = true;\n" +
                                        "def serviceMethod() {'hello'} }");
        
        Class c = gcl.parseClass("class TestController { def list = {} }");



        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,sc,c}, gcl );

        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean("classLoader", gcl);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
        app.setApplicationContext(parent);
        
        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[0], app);
        manager.setParentApplicationContext(parent);
        parent.registerMockBean("manager",manager);
        conf.setPluginManager(manager);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        
        // test class editor setup
        assertNotNull(ctx);
        assertTrue(ctx.getBean(GrailsRuntimeConfigurator.CLASS_LOADER_BEAN) instanceof GroovyClassLoader );
        assertTrue(ctx.getBean(GrailsRuntimeConfigurator.CLASS_EDITOR_BEAN) instanceof ClassEditor);

        // test exception resolver
        GrailsExceptionResolver er = (GrailsExceptionResolver)ctx.getBean(GrailsRuntimeConfigurator.EXCEPTION_HANDLER_BEAN);

        assertNotNull(er);
        ModelAndView mv = er.resolveException(new MockHttpServletRequest(),new MockHttpServletResponse(),null, new Exception());
        assertEquals("/error",mv.getViewName());

        // test multipart support
        assertTrue(ctx.getBean(GrailsRuntimeConfigurator.MULTIPART_RESOLVER_BEAN) instanceof CommonsMultipartResolver);

        // test message source
        MessageSource ms = (MessageSource)ctx.getBean(GrailsRuntimeConfigurator.MESSAGE_SOURCE_BEAN);
        assertNotNull(ms);

        Properties hibProps = (Properties)ctx.getBean(GrailsRuntimeConfigurator.HIBERNATE_PROPERTIES_BEAN);

        // test domain class setup correctly in the ctx
        GrailsDomainClass domainClass = (GrailsDomainClass)ctx.getBean("TestDomainClass");

        assertNotNull(domainClass);
        assertEquals("Test", domainClass.getShortName());

        Class persistentClass = (Class)ctx.getBean("TestPersistentClass");
        assertEquals(dc,persistentClass);

        org.codehaus.groovy.grails.validation.GrailsDomainClassValidator validator = (org.codehaus.groovy.grails.validation.GrailsDomainClassValidator)ctx.getBean("TestValidator");
        assertTrue(validator.supports(dc));

        // test service config
        GroovyObject serviceInstance = (GroovyObject)ctx.getBean("testService");
        assertEquals("hello",serviceInstance.invokeMethod("serviceMethod", null));

        // test controller config
        HotSwappableTargetSource ts = (HotSwappableTargetSource)ctx.getBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE);
        assertNotNull(ts.getTarget());
        
        ts = (HotSwappableTargetSource)ctx.getBean("TestControllerTargetSource");
        GrailsControllerClass gcc = (GrailsControllerClass)ts.getTarget();
        assertEquals(c,gcc.getClazz());

        GroovyObject controller = (GroovyObject)ctx.getBean("TestController");
        assertEquals(c,controller.getClass());
    }

    public void testConfigureScaffolding() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass("class Test { Long id; Long version; }");

        Class c = gcl.parseClass("class TestController { def scaffold = Test }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,c}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        assertNotNull(ctx);

        HotSwappableTargetSource ts = (HotSwappableTargetSource)ctx.getBean("TestControllerTargetSource");
        GrailsControllerClass gcc = (GrailsControllerClass)ts.getTarget();

        assertTrue(gcc.isScaffolding());
    }

    public void testRegisterAdditionalBean() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        ctx.registerSingleton("Test", dc);

        GroovyObject testInstance = (GroovyObject)ctx.getBean("Test");
        assertNotNull(testInstance);

        // now test override bean
        gcl = new GroovyClassLoader();
        dc = gcl.parseClass("class Test { Long id; Long version;String updatedProp = 'hello'; }");
        ctx.registerSingleton("Test",dc);
        testInstance = (GroovyObject)ctx.getBean("Test");
        assertNotNull(testInstance);
        assertEquals("hello",testInstance.getProperty("updatedProp"));
    }

    public void testRegisterTagLib() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        Class tag = gcl.parseClass("class TestTagLib { def myTag = { attrs -> } }");
        GrailsTagLibClass tagLibClass = (GrailsTagLibClass)app.addArtefact(TagLibArtefactHandler.TYPE, tag);
        GrailsRuntimeConfigurator.registerTagLibrary(tagLibClass, ctx);

        GroovyObject tagLib = (GroovyObject)ctx.getBean("TestTagLib");
        assertEquals(tag, tagLib.getClass());
        assertTrue(ctx.containsBean("TestTagLibTargetSource"));
        assertTrue(ctx.containsBean("TestTagLibProxy"));
    }

    public void testRegisterService() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
		gcl.parseClass("class ApplicationDataSource { String url = \"jdbc:hsqldb:mem:testDB\"; String driverClassName = \"org.hsqldb.jdbcDriver\" ;String username = \"sa\";String password = \"\"}");
        GrailsApplication app = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        Class service = gcl.parseClass("class TestService { boolean transactional = false;def serviceMethod() { 'hello' } }");
        GrailsServiceClass serviceClass = (GrailsServiceClass) app.addArtefact(ServiceArtefactHandler.TYPE, service);
        conf.registerService(serviceClass,ctx);

        assertTrue(ctx.containsBean("TestServiceClass"));
        GroovyObject serviceInstance = (GroovyObject)ctx.getBean("testService");

        assertEquals("hello",serviceInstance.invokeMethod("serviceMethod",null));
    }

   /* public void testRegisterDomainClass() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        Class dc = gcl.parseClass("class Test { Long id; Long version; }");
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);
        conf.registerDomainClass(domainClass, ctx);

        assertTrue(ctx.containsBean("TestDomainClass"));
        assertTrue(ctx.containsBean("TestTargetSource"));
        assertTrue(ctx.containsBean("TestProxy"));
        assertTrue(ctx.containsBean("TestPersistentClass"));
    }  */

   /*public void testRefreshSessionFactory() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        gcl = new GroovyClassLoader();
        dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);
        conf.updateDomainClass(domainClass, ctx);

        conf.refreshSessionFactory(app,ctx);
    }*/

    public void testCustomDialectConfiguration() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class ds = gcl.parseClass("class TestDataSource { def dialect = org.hibernate.dialect.Oracle9Dialect.class;String driverClassName = 'org.hsqldb.jdbcDriver';String url = 'jdbc:hsqldb:mem:testDB';String username ='sa'; String password =''; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{ds}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

    }

    public void testAutowireServiceClasses() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class s1 =  gcl.parseClass("class TestService { def serviceMethod() { 'hello' } }");
        
        Class s2 =  gcl.parseClass("class AnotherService { TestService testService; def anotherMethod() { testService.serviceMethod() } }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{s1,s2}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext(), false);
        assertNotNull(ctx);

        GroovyObject anotherService = (GroovyObject)ctx.getBean("anotherService");

        assertEquals("hello",anotherService.invokeMethod("anotherMethod", null));
    }
    public void testAutowireNonTransactionalServiceClasses() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class s1 =  gcl.parseClass("class TestService { boolean transactional = false;def serviceMethod() { 'hello' } }");
        Thread.sleep(1000);
        Class s2 =  gcl.parseClass("class AnotherService { boolean transactional = false;TestService testService; def anotherMethod() { testService.serviceMethod() } }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{s1,s2}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        assertEquals( s1, ctx.getBean("testService").getClass());
        assertEquals( s2, ctx.getBean("anotherService").getClass());

        GroovyObject anotherService = (GroovyObject)ctx.getBean("anotherService");
        assertNotNull(anotherService.getProperty("testService"));
        assertEquals("hello",anotherService.invokeMethod("anotherMethod", null));
    }

}
