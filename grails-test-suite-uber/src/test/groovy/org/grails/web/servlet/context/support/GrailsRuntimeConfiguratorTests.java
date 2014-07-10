package org.grails.web.servlet.context.support;

import grails.util.Holders;
import grails.util.Metadata;

import org.grails.spring.BeanConfiguration;
import org.grails.spring.DefaultRuntimeSpringConfiguration;
import org.grails.spring.GrailsApplicationContext;
import org.grails.spring.RuntimeSpringConfiguration;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import grails.core.GrailsDomainClass;
import grails.plugins.DefaultGrailsPluginManager;

import org.grails.support.MockApplicationContext;
import org.grails.validation.GrailsDomainClassValidator;
import org.grails.web.errors.GrailsExceptionResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

public class GrailsRuntimeConfiguratorTests extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();
        Holders.setConfig(null);
        Holders.setGrailsApplication(null);
        Holders.setPluginManager(null);

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
      * Test method for 'org.grails.web.servlet.context.support.GrailsRuntimeConfigurator.configure()'
      */
    public void testConfigure() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> dc = gcl.parseClass("class Test { Long id; Long version; }");

        Class<?> c = gcl.parseClass("class TestController { def list = {} }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,c}, gcl);

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
        assertTrue(ctx.getBean(GrailsApplication.CLASS_LOADER_BEAN) instanceof GroovyClassLoader);

        // test exception resolver
        GrailsExceptionResolver er = getBean(ctx, GrailsApplication.EXCEPTION_HANDLER_BEAN);

        assertNotNull(er);
        ModelAndView mv = er.resolveException(new MockHttpServletRequest(),new MockHttpServletResponse(),null, new Exception());
        assertEquals("/error",mv.getViewName());

        // test multipart support
        assertTrue(ctx.getBean(GrailsApplication.MULTIPART_RESOLVER_BEAN) instanceof StandardServletMultipartResolver);

        // test message source
        MessageSource ms = getBean(ctx, GrailsApplication.MESSAGE_SOURCE_BEAN);
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

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{dc,c}, gcl);
        app.getMetadata().put(Metadata.APPLICATION_NAME, getClass().getName());
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,parent);
        ApplicationContext ctx = conf.configure(new MockServletContext());
        assertNotNull(ctx);
    }

    public void testRegisterAdditionalBean() {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> dc = gcl.parseClass("class Test { Long id; Long version; }");

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl);
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

    public void testApplicationIsAvailableInResources() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass("class Holder { def value }");
        /*Class<?> resourcesClass =*/ gcl.parseClass("beans = { b(Holder, value: application); b2(Holder, value: grailsApplication) }", "resources.groovy");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl);
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
        GrailsRuntimeConfigurator.loadExternalSpringConfig(springConfig, app);

        for (String bean : new String[] { "b", "b2" }) {
            assertTrue(springConfig.containsBean(bean));
            BeanConfiguration beanConfig = springConfig.getBeanConfig(bean);
            assertTrue(beanConfig.hasProperty("value"));
            assertEquals(app, beanConfig.getPropertyValue("value"));
        }
    }

    // test for GRAILS-8764
    public void testAliasRegistrationInResources() throws Exception {

        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass(
                "beans = {\n" +
                "   foo(HashMap)\n" +
                "   springConfig.addAlias 'bar', 'foo'\n" +
                "   springConfig.addAlias 'grapp', 'grailsApplication'\n" +
                "}",
                "resources.groovy");

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl);
        app.getMetadata().put(Metadata.APPLICATION_NAME, getClass().getName());

        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app, parent);
        GrailsApplicationContext ctx = (GrailsApplicationContext)conf.configure(new MockServletContext());

        WebRuntimeSpringConfiguration springConfig = conf.getWebRuntimeSpringConfiguration();
        GrailsRuntimeConfigurator.loadExternalSpringConfig(springConfig, app);

        springConfig.registerBeansWithContext(ctx);
        assertTrue(ctx.containsBean("foo"));
        assertTrue(ctx.containsBean("bar"));
//        assertTrue(ctx.containsBean("grapp"));
    }

    @SuppressWarnings("unchecked")
    private <T> T getBean(ApplicationContext ctx, String name) {
        return (T)ctx.getBean(name);
    }
}
