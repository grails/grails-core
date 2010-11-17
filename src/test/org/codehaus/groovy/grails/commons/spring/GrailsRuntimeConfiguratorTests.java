package org.codehaus.groovy.grails.commons.spring;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.BeanConfiguration;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

public class GrailsRuntimeConfiguratorTests extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();
        ConfigurationHolder.setConfig(null);

        super.setUp();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        ExpandoMetaClass.disableGlobally();
        super.tearDown();
    }

    /*
      * Test method for 'org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator.configure()'
      */
    public void testConfigure() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> dc = gcl.parseClass("class Test { Long id; Long version; }");

        Class<?> c = gcl.parseClass("class TestController { def list = {} }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,c}, gcl );

        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        parent.registerMockBean("classLoader", gcl);

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

        // test exception resolver
        GrailsExceptionResolver er = getBean(ctx, GrailsRuntimeConfigurator.EXCEPTION_HANDLER_BEAN);

        assertNotNull(er);
        ModelAndView mv = er.resolveException(new MockHttpServletRequest(),new MockHttpServletResponse(),null, new Exception());
        assertEquals("/error",mv.getViewName());

        // test multipart support
        assertTrue(ctx.getBean(GrailsRuntimeConfigurator.MULTIPART_RESOLVER_BEAN) instanceof CommonsMultipartResolver);

        // test message source
        MessageSource ms = getBean(ctx, GrailsRuntimeConfigurator.MESSAGE_SOURCE_BEAN);
        assertNotNull(ms);

        // test domain class setup correctly in the ctx
        GrailsDomainClass domainClass = getBean(ctx, "TestDomainClass");

        assertNotNull(domainClass);
        assertEquals("Test", domainClass.getShortName());

        Class<?> persistentClass = getBean(ctx, "TestPersistentClass");
        assertEquals(dc,persistentClass);

        GrailsDomainClassValidator validator = getBean(ctx, "TestValidator");
        assertTrue(validator.supports(dc));

        GroovyObject controller = getBean(ctx, "TestController");
        assertEquals(c,controller.getClass());
    }

    public void testConfigureScaffolding() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> dc = gcl.parseClass("class Test { Long id; Long version; }");

        Class<?> c = gcl.parseClass("class TestController { def scaffold = Test }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,c}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        assertNotNull(ctx);
    }

    public void testRegisterAdditionalBean() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        ctx.registerSingleton("Test", dc);

        GroovyObject testInstance = getBean(ctx, "Test");
        assertNotNull(testInstance);

        // now test override bean
        gcl = new GroovyClassLoader();
        dc = gcl.parseClass("class Test { Long id; Long version;String updatedProp = 'hello'; }");
        ctx.registerSingleton("Test",dc);
        testInstance = getBean(ctx, "Test");
        assertNotNull(testInstance);
        assertEquals("hello",testInstance.getProperty("updatedProp"));
    }

    public void testAutowireServiceClasses() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> s1 =  gcl.parseClass("class TestService { def serviceMethod() { 'hello' } }");

        Class<?> s2 =  gcl.parseClass("class AnotherService { TestService testService; def anotherMethod() { testService.serviceMethod() } }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{s1,s2}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext(), false);
        assertNotNull(ctx);

        GroovyObject anotherService = getBean(ctx, "anotherService");

        assertEquals("hello",anotherService.invokeMethod("anotherMethod", null));
    }

    public void testAutowireNonTransactionalServiceClasses() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> s1 =  gcl.parseClass("class TestService { boolean transactional = false;def serviceMethod() { 'hello' } }");
        Thread.sleep(1000);
        Class<?> s2 =  gcl.parseClass("class AnotherService { boolean transactional = false;TestService testService; def anotherMethod() { testService.serviceMethod() } }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{s1,s2}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        conf.setLoadExternalPersistenceConfig(false);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());
        assertNotNull(ctx);

        assertEquals( s1, ctx.getBean("testService").getClass());
        assertEquals( s2, ctx.getBean("anotherService").getClass());

        GroovyObject anotherService = getBean(ctx, "anotherService");
        assertNotNull(anotherService.getProperty("testService"));
        assertEquals("hello",anotherService.invokeMethod("anotherMethod", null));
    }

    public void testApplicationIsAvailableInResources() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass("class Holder { def value }");
        Class<?> resourcesClass = gcl.parseClass("beans = { b(Holder, value: application) }", "resources.groovy");
        
        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl);
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
        GrailsRuntimeConfigurator.loadExternalSpringConfig(springConfig, app);

        assertTrue(springConfig.containsBean("b"));
        BeanConfiguration beanConfig = springConfig.getBeanConfig("b");
        assertTrue(beanConfig.hasProperty("value"));
        assertEquals(app, beanConfig.getPropertyValue("value"));
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getBean(ApplicationContext ctx, String name) {
        return (T)ctx.getBean(name);
    }
}
