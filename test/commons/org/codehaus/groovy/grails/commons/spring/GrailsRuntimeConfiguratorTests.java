package org.codehaus.groovy.grails.commons.spring;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClassRegistry;

import java.util.Properties;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsServiceClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClassCreationHandle;
import org.codehaus.groovy.grails.orm.hibernate.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.support.ClassEditor;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;

public class GrailsRuntimeConfiguratorTests extends TestCase {

	
    /* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(new ExpandoMetaClassCreationHandle());

		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(new MetaClassRegistry.MetaClassCreationHandle());

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

        assertNotNull(hibProps);
        assertEquals("create-drop",hibProps.getProperty("hibernate.hbm2ddl.auto"));

        // test domain class setup correctly in the ctx
        GrailsDomainClass domainClass = (GrailsDomainClass)ctx.getBean("TestDomainClass");

        assertNotNull(domainClass);
        assertEquals("Test", domainClass.getShortName());

        Class persistentClass = (Class)ctx.getBean("TestPersistentClass");
        assertEquals(dc,persistentClass);

        GrailsDomainClassValidator validator = (GrailsDomainClassValidator)ctx.getBean("TestValidator");
        assertTrue(validator.supports(dc));

        // test service config
        GroovyObject serviceInstance = (GroovyObject)ctx.getBean("testService");
        assertEquals("hello",serviceInstance.invokeMethod("serviceMethod", null));

        // test controller config
        HotSwappableTargetSource ts = (HotSwappableTargetSource)ctx.getBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE);
        assertNotNull(ts.getTarget());
        GrailsUrlHandlerMapping mapping = (GrailsUrlHandlerMapping)ts.getTarget();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/list");

        HandlerExecutionChain h = mapping.getHandler(request);
        assertNotNull(h);
        assertNotNull(h.getHandler());
        assertEquals(SimpleGrailsController.class,h.getHandler().getClass());
//        assertEquals(2,h.getInterceptors().length);
        
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

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        assertNotNull(ctx);

        HotSwappableTargetSource ts = (HotSwappableTargetSource)ctx.getBean("TestControllerTargetSource");
        GrailsControllerClass gcc = (GrailsControllerClass)ts.getTarget();

        assertTrue(gcc.isScaffolding());
    }

    public void testConfigureScheduledJobs() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class c = gcl.parseClass(
        "class MyJob {\n " +
        " def startDelay = 100\n" +
        " def timeout = 1000\n" +
        " def name = 'MyJob'\n" +
        " def group = 'MyGroup'\n" +
        " def concurrent = true\n" +
        " def execute(){\n" +
        "    print 'Job run!'\n" +
        "  }\n" +
        "}" );

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{c}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        assertNotNull(ctx);

    }

    public void testRegisterAdditionalBean() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
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

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        Class tag = gcl.parseClass("class TestTagLib { def myTag = { attrs -> } }");
        GrailsTagLibClass tagLibClass = app.addTagLibClass(tag);
        conf.registerTagLibrary(tagLibClass, ctx);

        GroovyObject tagLib = (GroovyObject)ctx.getBean("TestTagLib");
        assertEquals(tag, tagLib.getClass());
        assertTrue(ctx.containsBean("TestTagLibTargetSource"));
        assertTrue(ctx.containsBean("TestTagLibProxy"));
    }

    public void testRegisterService() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        Class service = gcl.parseClass("class TestService { boolean transactional = true;def serviceMethod() { 'hello' } }");
        GrailsServiceClass serviceClass = app.addServiceClass(service);
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

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

    }

    public void testAutowireServiceClasses() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class s1 =  gcl.parseClass("class TestService { def serviceMethod() { 'hello' } }");
        
        Class s2 =  gcl.parseClass("class AnotherService { TestService testService; def anotherMethod() { testService.serviceMethod() } }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{s1,s2}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext(), false);
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

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        assertEquals( s1, ctx.getBean("testService").getClass());
        assertEquals( s2, ctx.getBean("anotherService").getClass());

        GroovyObject anotherService = (GroovyObject)ctx.getBean("anotherService");
        assertNotNull(anotherService.getProperty("testService"));
        assertEquals("hello",anotherService.invokeMethod("anotherMethod", null));
    }

    public void testAfterConfSet() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsWebApplicationContext ctx = (GrailsWebApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);
        
        GrailsMockDependantObject gdo = (GrailsMockDependantObject)ctx.getBean("grailsDependent");
        assertNotNull(gdo.getSessionFactory());
    	
    }
}
