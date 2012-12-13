package org.codehaus.groovy.grails.orm.hibernate

import spock.lang.Specification
import org.springframework.util.Log4jConfigurer
import org.codehaus.groovy.grails.support.MockApplicationContext
import spock.lang.Shared
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.support.StaticMessageSource
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.springframework.context.ApplicationContext
import org.grails.datastore.mapping.transactions.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.hibernate.SessionFactory
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.hibernate.Session
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.core.type.ClassMetadata
import org.hibernate.EntityMode
import org.springframework.orm.hibernate3.SessionHolder
import grails.util.Metadata


abstract class GormSpec extends Specification {

    MockApplicationContext parentCtx
    ApplicationContext applicationContext
    GrailsApplication grailsApplication
    GroovyClassLoader gcl = new GroovyClassLoader(getClass().classLoader)
    GrailsPluginManager mockManager
    SessionFactory sessionFactory
    Session session

    def setup() {
        if (new File("src/test/groovy/log4j.properties").exists()) {
            Log4jConfigurer.initLogging("src/test/groovy/log4j.properties")
        }
        else if (new File("grails-test-suite-persistence/src/test/groovy/log4j.properties").exists()) {
            Log4jConfigurer.initLogging("grails-test-suite-persistence/src/test/groovy/log4j.properties")
        }

        ExpandoMetaClass.enableGlobally()

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        configureDataSource()

        parentCtx = new MockApplicationContext()

        grailsApplication = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
		grailsApplication.metadata[Metadata.APPLICATION_NAME] = getClass().name

        def dependentPlugins = configurePlugins()

        initializeApplication()

        parentCtx.registerMockBean("messageSource", new StaticMessageSource())

        def springConfig = new WebRuntimeSpringConfiguration(parentCtx, gcl)
        doWithRuntimeConfiguration dependentPlugins, springConfig

        grailsApplication.setMainContext(springConfig.getUnrefreshedApplicationContext())
        applicationContext = springConfig.getApplicationContext()
        dependentPlugins*.doWithApplicationContext(applicationContext)

        mockManager.applicationContext = applicationContext
        mockManager.doDynamicMethods()

        registerHibernateSession()
    }

    def cleanup() {
        unbindSessionFactory sessionFactory
        sessionFactory = null

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
        grailsApplication = null
        mockManager = null
        applicationContext.close()
        applicationContext = null

        ExpandoMetaClass.disableGlobally()
        RequestContextHolder.setRequestAttributes(null)
        PluginManagerHolder.setPluginManager(null)

    }

    protected void unregisterHibernateSession() {
        unbindSessionFactory sessionFactory
    }

    protected void unbindSessionFactory(SessionFactory sessionFactory) {
        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            Session s = TransactionSynchronizationManager.getResource(sessionFactory).session
            TransactionSynchronizationManager.unbindResource sessionFactory
            SessionFactoryUtils.releaseSession s, sessionFactory
        }

        for (def metadata in sessionFactory.allClassMetadata.values()) {
            GroovySystem.getMetaClassRegistry().removeMetaClass metadata.getMappedClass(EntityMode.POJO)
        }
    }

    protected void registerHibernateSession() {
        sessionFactory = applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN)
        bindSessionFactory sessionFactory
        session = sessionFactory.currentSession
    }

    protected void bindSessionFactory(SessionFactory sessionFactory) {
        if (!TransactionSynchronizationManager.hasResource(sessionFactory)) {
            TransactionSynchronizationManager.bindResource(sessionFactory,
                new SessionHolder(sessionFactory.openSession()))
        }
    }

    protected void initializeApplication() {
        grailsApplication.initialise()
        domainClasses?.each { dc -> grailsApplication.addArtefact 'Domain', dc }
        grailsApplication.setApplicationContext(applicationContext)
        parentCtx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication)
    }

    protected void doWithRuntimeConfiguration(dependentPlugins, springConfig) {
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
    }

    protected List configurePlugins() {
        mockManager = new MockGrailsPluginManager(grailsApplication)

        parentCtx.registerMockBean("pluginManager", mockManager)

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

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, grailsApplication) }

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        dependentPlugins
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

    abstract List getDomainClasses()
}

