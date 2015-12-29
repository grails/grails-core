package org.grails.web.servlet.mvc

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.util.GrailsNameUtils
import grails.util.GrailsWebMockUtil
import grails.util.Holders
import grails.util.Metadata
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import grails.web.databinding.DataBindingUtils
import grails.web.databinding.GrailsWebDataBinder
import grails.web.pages.GroovyPagesUriService
import org.grails.web.util.GrailsApplicationAttributes

import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.databinding.converters.DateConversionHelper
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.support.MockApplicationContext
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.pages.DefaultGroovyPagesUriService
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

abstract class AbstractGrailsControllerTests extends GroovyTestCase {

    def servletContext
    GrailsWebRequest webRequest
    MockHttpServletRequest request
    MockHttpServletResponse response
    GroovyClassLoader gcl = new GrailsAwareClassLoader(getClass().classLoader)
    GrailsApplication ga
    def mockManager
    MockApplicationContext ctx
    ApplicationContext appCtx
    def originalHandler

    /**
     * Subclasses may override this method to return a list of classes which should
     * be added to the GrailsApplication as controller classes
     *
     * @return a list of classes
     */
    protected Collection<Class> getControllerClasses() {
        Collections.EMPTY_LIST
    }

    /**
     * Subclasses may override this method to return a list of classes which should
     * be added to the GrailsApplication as domain classes
     *
     * @return a list of classes
     */
    protected Collection<Class> getDomainClasses() {
        Collections.EMPTY_LIST
    }

    protected void onSetUp() {}

    protected void setUp() {
        super.setUp()

        ExpandoMetaClass.enableGlobally()

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        ctx = new MockApplicationContext()
        onSetUp()
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses().findAll { clazz -> !Closure.isAssignableFrom(clazz) } as Class[], gcl)

        def binder = new GrailsWebDataBinder(ga)
        binder.registerConverter new DateConversionHelper()

        ctx.registerMockBean(DataBindingUtils.DATA_BINDER_BEAN_NAME, binder)

        ga.metadata[Metadata.APPLICATION_NAME] = getClass().name
        mockManager = new MockGrailsPluginManager(ga)
        ctx.registerMockBean("manager", mockManager)
        def dependantPluginClasses = [
            "org.grails.plugins.CoreGrailsPlugin",
            "org.grails.plugins.CodecsGrailsPlugin",
            "org.grails.plugins.domain.DomainClassGrailsPlugin",
            "org.grails.plugins.i18n.I18nGrailsPlugin",
            "org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin",
            "org.grails.plugins.web.controllers.ControllersGrailsPlugin",
            "org.grails.plugins.web.GroovyPagesGrailsPlugin",
            "org.grails.plugins.web.mime.MimeTypesGrailsPlugin",
            "org.grails.plugins.converters.ConvertersGrailsPlugin",
            "org.grails.plugins.web.rest.plugin.RestResponderGrailsPlugin"
        ].collect { className ->
            gcl.loadClass(className)
        }
        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()

        ga.initialise()

        ga.setApplicationContext(ctx)
        controllerClasses.each { cc -> ga.addArtefact 'Controller', cc }
        domainClasses.each { c -> ga.addArtefact 'Domain', c }

        ctx.registerMockBean("pluginManager", mockManager)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        ctx.registerMockBean("messageSource", new StaticMessageSource())
        ctx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        servletContext = ctx.getServletContext()

        springConfig.servletContext = servletContext

        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }

        ctx.registerMockBean("grailsDomainClassMappingContext", new GrailsDomainClassMappingContext(ga))
        ga.mainContext = springConfig.getUnrefreshedApplicationContext()
        appCtx = springConfig.getApplicationContext()

        dependentPlugins*.doWithApplicationContext(appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        mockManager.applicationContext = appCtx
        mockManager.doDynamicMethods()

        request = new GrailsMockHttpServletRequest(characterEncoding: "utf-8")
        response = new GrailsMockHttpServletResponse()
        webRequest = GrailsWebMockUtil.bindMockWebRequest(appCtx, request, response)
    }

    protected setCurrentController(controller) {
        RequestContextHolder.requestAttributes.controllerName = GrailsNameUtils.getLogicalName(controller.class.name, "Controller")
    }

    protected void tearDown() {
        ga.mainContext.close()
        RequestContextHolder.resetRequestAttributes()
        ExpandoMetaClass.disableGlobally()

        Holders.config = null
        Holders.grailsApplication = null
        Holders.setPluginManager(null)

        ConvertersConfigurationHolder.getInstance().clear()

        super.tearDown()
    }

    def withConfig(String text, Closure callable) {
        def config = new ConfigSlurper().parse(text)
        try {
            buildMockRequest(config)
            callable()
        }
        finally {
            RequestContextHolder.resetRequestAttributes()

        }
    }

    GrailsWebRequest buildMockRequest(ConfigObject config) throws Exception {
        def appCtx = new MockApplicationContext()
        appCtx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())

        ga.config = config

        appCtx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        appCtx.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        appCtx.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        return GrailsWebMockUtil.bindMockWebRequest(appCtx)
    }

    void runTest(Closure callable) {
        callable.call()
    }

    protected MockServletContext createMockServletContext() { new MockServletContext() }

    protected MockApplicationContext createMockApplicationContext() { new MockApplicationContext() }

    protected Resource[] getResources(String pattern) {
        new PathMatchingResourcePatternResolver().getResources(pattern)
    }

    protected createGrailsApplication() {
        def app = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        def mainContext = new MockApplicationContext()
        mainContext.registerMockBean UrlConverter.BEAN_NAME, new CamelCaseUrlConverter()
        app.mainContext = mainContext
        app
    }

}
