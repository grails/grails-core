package org.codehaus.groovy.grails.orm.hibernate

import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.hibernate.EntityMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.metadata.ClassMetadata
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockServletContext
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.Log4jConfigurer
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGrailsHibernateTests extends GroovyTestCase {

    GroovyClassLoader gcl = new GroovyClassLoader(getClass().classLoader)
    GrailsApplication ga
    GrailsApplication grailsApplication
    GrailsPluginManager mockManager
    MockApplicationContext ctx
    ApplicationContext appCtx
    ApplicationContext applicationContext
    def originalHandler
    SessionFactory sessionFactory
    Session session

    protected void onSetUp() {
    }

    protected void setUp() {
        super.setUp()

        if (new File("src/test/groovy/log4j.properties").exists()) {
            Log4jConfigurer.initLogging("src/test/groovy/log4j.properties")
        }
        else if (new File("grails-test-suite-persistence/src/test/groovy/log4j.properties").exists()) {
            Log4jConfigurer.initLogging("grails-test-suite-persistence/src/test/groovy/log4j.properties")
        }

        ExpandoMetaClass.enableGlobally()

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        configureDataSource()

        ctx = new MockApplicationContext()

        onSetUp()

        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
        grailsApplication = ga

        def dependentPlugins = configurePlugins()

        afterPluginInitialization()

        initializeApplication()

        ctx.registerMockBean("messageSource", new StaticMessageSource())

        def springConfig = new WebRuntimeSpringConfiguration(ctx, gcl)
        doWithRuntimeConfiguration dependentPlugins, springConfig

        ga.setMainContext(springConfig.getUnrefreshedApplicationContext())
        appCtx = springConfig.getApplicationContext()
        applicationContext = appCtx
        dependentPlugins*.doWithApplicationContext(appCtx)

        mockManager.applicationContext = appCtx
        mockManager.doDynamicMethods()

        registerHibernateSession()
    }

     protected void configureDataSource() {
        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = "create-drop"
    url = "jdbc:h2:mem:grailsIntTestDB"
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
''', "DataSource")
    }

    protected setCurrentController(controller) {
        RequestContextHolder.requestAttributes.controllerName = GrailsNameUtils.getLogicalName(controller.class.name, "Controller")
    }

    void onApplicationCreated() {}

    /**
     * Subclasses may override this method to return a list of classes which should
     * be added to the GrailsApplication as domain classes
     *
     * @return a list of classes
     */
    protected getDomainClasses() {
        Collections.EMPTY_LIST
    }

    protected void doWithRuntimeConfiguration(dependentPlugins, springConfig) {
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
     }

    GrailsWebRequest buildMockRequest(ConfigObject config = null) throws Exception {
        if (config != null) {
            ga.config = config
        }
        def servletContext = new MockServletContext()
        appCtx.setServletContext(servletContext)
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        return GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    protected void tearDown() {

        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory)
            Session s = holder.session
            TransactionSynchronizationManager.unbindResource(sessionFactory)
            SessionFactoryUtils.releaseSession(s, sessionFactory)
        }

        def classMetadata = sessionFactory.allClassMetadata
        for (entry in classMetadata) {
            ClassMetadata metadata = entry.value
            GroovySystem.getMetaClassRegistry().removeMetaClass(metadata.getMappedClass(EntityMode.POJO))
        }

        GroovySystem.stopThreadedReferenceManager()

        try {
            TransactionSynchronizationManager.clear()
        }
        catch(e) {
            // means it is not active, ignore
        }
        try {
            getClass().classLoader.loadClass("net.sf.ehcache.CacheManager")
                                    .getInstance()?.shutdown()
        }
        catch(e) {
            // means there is no cache, ignore
        }
        gcl = null
        ga = null
        mockManager = null
        appCtx.close()
        ConverterUtil.clearInstance()
        ctx = null
        appCtx = null

        ExpandoMetaClass.disableGlobally()
        RequestContextHolder.setRequestAttributes(null)
        PluginManagerHolder.setPluginManager(null)

        originalHandler = null

        onTearDown()

        super.tearDown()
    }

    protected MockApplicationContext createMockApplicationContext() {
        return new MockApplicationContext()
    }

    protected Resource[] getResources(String pattern) throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(pattern);
    }

    protected void onTearDown() {
    }

    protected List configurePlugins() {
        mockManager = new MockGrailsPluginManager(ga)

        ctx.registerMockBean("pluginManager", mockManager)
        PluginManagerHolder.setPluginManager(mockManager)

        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
        dependantPluginClasses << MockHibernateGrailsPlugin

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga) }

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager())

        dependentPlugins
    }

    protected void afterPluginInitialization() {
    }

    protected void initializeApplication() {
        ga.initialise()
        onApplicationCreated()
        domainClasses?.each { dc -> ga.addArtefact 'Domain', dc }
        ga.setApplicationContext(ctx)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
    }

    protected void registerHibernateSession() {
        sessionFactory = appCtx.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN)

        if (!TransactionSynchronizationManager.hasResource(sessionFactory)) {
            session = sessionFactory.openSession()
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))
        }
    }
}

class MockHibernateGrailsPlugin {

    def version = GrailsUtil.grailsVersion
    def dependsOn = [dataSource: version,
                     i18n: version,
                     core: version,
                     domainClass: version]

    def artefacts = [new AnnotationDomainClassArtefactHandler()]
    def loadAfter = ['controllers']
    def doWithSpring = HibernatePluginSupport.doWithSpring
    def doWithDynamicMethods = HibernatePluginSupport.doWithDynamicMethods
}
