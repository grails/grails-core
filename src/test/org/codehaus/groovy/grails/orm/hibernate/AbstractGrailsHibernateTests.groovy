package org.codehaus.groovy.grails.orm.hibernate

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport
import org.codehaus.groovy.grails.support.MockApplicationContext

import org.hibernate.Session
import org.hibernate.SessionFactory

import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.util.Log4jConfigurer

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 14, 2009
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

        Log4jConfigurer.initLogging("src/test/log4j.properties") 
        ExpandoMetaClass.enableGlobally()


        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = "org.hsqldb.jdbcDriver"
    username = "sa"
    password = ""
    dbCreate = "create-drop" // one of 'create', 'create-drop','update'
    url = "jdbc:hsqldb:mem:grailsIntTestDB"    
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
''', "DataSource")

        ctx = new MockApplicationContext()
        onSetUp()
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
        grailsApplication = ga
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

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager())

        afterPluginInitialization()

        ga.initialise()
        ga.setApplicationContext(ctx)
        ApplicationHolder.setApplication(ga)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        ctx.registerMockBean("messageSource", new StaticMessageSource())

        def springConfig = new WebRuntimeSpringConfiguration(ctx, gcl)
		  doWithRuntimeConfiguration dependentPlugins, springConfig

        appCtx = springConfig.getApplicationContext()
        ga.setMainContext(appCtx)
        applicationContext = appCtx
        dependentPlugins*.doWithApplicationContext(appCtx)

        mockManager.applicationContext = appCtx
        mockManager.doDynamicMethods()

        sessionFactory = appCtx.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN)

        if(!TransactionSynchronizationManager.hasResource(sessionFactory)) {
            session = sessionFactory.openSession()
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))
        }
    }

    protected void doWithRuntimeConfiguration(dependentPlugins, springConfig) {
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
	 }

    protected void tearDown() {

        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory)
            Session s = holder.session
            TransactionSynchronizationManager.unbindResource(sessionFactory)
            SessionFactoryUtils.releaseSession(s, sessionFactory)
        }

        gcl = null
        ga = null
        mockManager = null
        ctx = null
        appCtx = null

        ApplicationHolder.setApplication(null)
        ExpandoMetaClass.disableGlobally()

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
	 
    protected void afterPluginInitialization() {
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
