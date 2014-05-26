package grails.web.servlet.initializer

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplicationFactoryBean
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManagerFactoryBean
import org.codehaus.groovy.grails.web.context.GrailsContextLoaderListener
import org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.context.support.ServletContextResource
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer

import javax.servlet.Filter
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRegistration

/**
 * An initializer for starting up Grails in the Servlet 3.0 environment
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class StandardGrailsServletInitializer extends AbstractDispatcherServletInitializer{


    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager
    WebApplicationContext parent
    ServletContext servletContext

    @Override
    protected String[] getServletMappings() {
        ["*.dispatch"] as String[]
    }

    @Override
    void onStartup(ServletContext servletContext) throws ServletException {
        GrailsApplicationFactoryBean grailsApplicationFactoryBean = new GrailsApplicationFactoryBean()
        def grailsDescriptor = new ServletContextResource(servletContext, '/WEB-INF/grails.xml')
        grailsApplicationFactoryBean.setGrailsDescriptor(grailsDescriptor)
        grailsApplicationFactoryBean.afterPropertiesSet()
        this.grailsApplication = grailsApplicationFactoryBean.getObject()

        GrailsPluginManagerFactoryBean pluginManagerFactoryBean = new GrailsPluginManagerFactoryBean()
        pluginManagerFactoryBean.setGrailsDescriptor(grailsDescriptor)
        pluginManagerFactoryBean.setApplication(grailsApplication)
        pluginManagerFactoryBean.afterPropertiesSet()

        this.pluginManager = pluginManagerFactoryBean.getObject()
        this.servletContext = servletContext
        pluginManager.loadPlugins()

        for (GrailsPlugin plugin in pluginManager.allPlugins ) {
            def instance = plugin.instance
            if(instance.respondsTo('onStartup', [servletContext] as Object[])) {
                instance.invokeMethod('onStartup', servletContext)
            }
        }
        super.onStartup(servletContext)
    }

    @Override
    protected void registerDispatcherServlet(ServletContext servletContext) {
        String servletName = getServletName();
        Assert.hasLength(servletName, "getServletName() may not return empty or null");

        WebApplicationContext servletAppContext = createServletApplicationContext();
        Assert.notNull(servletAppContext,
                "createServletApplicationContext() did not return an application " +
                        "context for servlet [" + servletName + "]");

        DispatcherServlet dispatcherServlet = new GrailsDispatcherServlet(servletAppContext);
        ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
        Assert.notNull(registration,
                "Failed to register servlet with name '" + servletName + "'." +
                        "Check if there is another servlet registered under the same name.");

        registration.setLoadOnStartup(1);
        registration.addMapping(getServletMappings());
        registration.setAsyncSupported(isAsyncSupported());

        Filter[] filters = getServletFilters();
        if (!ObjectUtils.isEmpty(filters)) {
            for (Filter filter : filters) {
                registerServletFilter(servletContext, filter);
            }
        }

        customizeRegistration(registration);
    }

    @Override
    protected WebApplicationContext createServletApplicationContext() {
        return new GrailsWebApplicationContext(grailsApplication)
    }

    @Override
    protected void registerContextLoaderListener(ServletContext servletContext) {
        def rootContext = createRootApplicationContext()
        servletContext.addListener(new GrailsContextLoaderListener(rootContext))
    }

    @Override
    protected WebApplicationContext createRootApplicationContext() {

        def genericContext = new GenericWebApplicationContext()
        parent = genericContext

        def beanFactory = genericContext.defaultListableBeanFactory
        beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, grailsApplication)
        pluginManager.setApplicationContext(this.parent)
        beanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, pluginManager)
        genericContext.refresh()


        def root = new GenericWebApplicationContext()
        root.parent = parent
        return root
    }
}


