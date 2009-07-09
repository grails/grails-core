package org.codehaus.groovy.grails.web.servlet.filter

import grails.util.GrailsWebUtil

import javax.servlet.Filter
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

/**
 * Abstract test case to make testing servlet filters easier.
 */
abstract class AbstractServletFilterTests extends GroovyTestCase {
    GroovyClassLoader     gcl
    ServletContext        servletContext
    GrailsWebRequest      webRequest
    HttpServletRequest    request
    HttpServletResponse   response
    WebApplicationContext appCtx
    GrailsApplication     application
    GrailsPluginManager   pluginManager
    def evaluator
    def filter

    void setUp() {
        super.setUp()

        servletContext = new MockServletContext();
        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        appCtx = new MockApplicationContext()

        evaluator = new DefaultUrlMappingEvaluator()

        // Mimic AbstractGrailsPluginTests: create a new class loader
        // and allow sub-classes to use for parsing and loading classes.
        gcl = new GroovyClassLoader()
        onSetup()
    }

    void tearDown() {
        ServletContextHolder.setServletContext(null);
        PluginManagerHolder.setPluginManager(null)
    }

    protected void onSetup() {}

    /**
     * Initialise the given filter so that it can tested.
     */
    protected final void initFilter(Filter filter) {
        filter.init(new MockFilterConfig(this.servletContext))
    }

    /**
     * Set up the mock parent application context and bind it to the
     * servlet context.
     */
    protected final void bindApplicationContext() {
        this.servletContext.setAttribute(ApplicationAttributes.PARENT_APPLICATION_CONTEXT, this.appCtx);
    }

    /**
     * Set up the Grails application and bind it to the servlet context.
     */
    protected final void bindGrailsApplication() {
        // Create a new Grails application with the stored Groovy class
        // loader.
        this.application = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        this.pluginManager = new MockGrailsPluginManager(this.application)

        // Register the application instance and plugin manager with
        // the mock application context.
        this.appCtx.registerMockBean(GrailsApplication.APPLICATION_ID, this.application)
        this.appCtx.registerMockBean(GrailsPluginManager.BEAN_NAME, this.pluginManager)
        PluginManagerHolder.setPluginManager(this.pluginManager)

        // Configure everything as if it's a running app.
        GrailsConfigUtils.configureWebApplicationContext(this.servletContext, this.appCtx)
    }
}
