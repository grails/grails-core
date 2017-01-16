package org.grails.plugins.services

import grails.config.Settings
import grails.core.GrailsApplication
import grails.plugins.GrailsPlugin
import grails.plugins.GrailsPluginManager
import grails.plugins.PluginFilter
import grails.plugins.exceptions.PluginException
import grails.web.servlet.plugins.GrailsWebPluginManager
import org.grails.commons.test.AbstractGrailsMockTests
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockHibernateGrailsPlugin
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.core.type.filter.TypeFilter

class ServicesGrailsPluginTests extends AbstractGrailsMockTests {

    void onSetUp() {
        gcl.parseClass('''
            dataSource {
                pooled = false
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                dbCreate = "create-drop"
            }
''', 'Config')

        gcl.parseClass """
import org.springframework.transaction.annotation.*
import org.springframework.transaction.interceptor.TransactionAspectSupport
class SomeTransactionalService {
    boolean transactional = true
    def serviceMethod() {
         return "hello"
    }
}
class NonTransactionalService {
    boolean transactional = false
    def serviceMethod() {
        return "goodbye"
    }
}

@Transactional(readOnly = true)
class SpringTransactionalService {

    def serviceMethod() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status!=null}"
    }
}

class PerMethodTransactionalService {

    @Transactional
    def methodOne() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status!=null}"
    }

    def methodTwo() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status!=null}"
    }
}

class TransactionalFalseService {

    static transactional = false

    def methodOne() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }

    def methodTwo() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }
}

class TransactionalTrueService {

    static transactional = true

    def methodOne() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }

    def methodTwo() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }
}


class TransactionalAbsentService {

    def methodOne() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }

    def methodTwo() {
        def status = null
        try { status = TransactionAspectSupport.currentTransactionStatus() } catch(e){ println e.message }
        return "hasTransaction = \${status != null}"
    }
}
"""
    }

    void testTransactionalFalseService() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("transactionalFalseService")
        assertEquals "hasTransaction = false", springService.methodOne()
        springService.methodTwo()
    }

    void testTransactionalFalseServiceConfigDisabled() {
        def appCtx = initializeContext(false)

        def springService = appCtx.getBean("transactionalFalseService")
        assertEquals "hasTransaction = false", springService.methodOne()
        springService.methodTwo()
    }

    void testTransactionalTrueService() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("transactionalTrueService")
        assertEquals "hasTransaction = true", springService.methodOne()
        assertEquals "hasTransaction = true", springService.methodTwo()
    }

    void testTransactionalTrueServiceConfigDisabled() {
        def appCtx = initializeContext(false)

        def springService = appCtx.getBean("transactionalTrueService")
        assertEquals "hasTransaction = false", springService.methodOne()
        assertEquals "hasTransaction = false", springService.methodTwo()
    }

    void testTransactionalAbsentService() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("transactionalAbsentService")
        assertEquals "hasTransaction = false", springService.methodOne()
        springService.methodTwo()
    }

    void testPerMethodTransactionAnnotations() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("perMethodTransactionalService")
        assertEquals "hasTransaction = true", springService.methodOne()

        //Given one method having annotation, then the whole service will be be proxied
        assertEquals "hasTransaction = true", springService.methodTwo()
    }

    void testPerMethodTransactionAnnotationsConfigDisabled() {
        def appCtx = initializeContext(false)

        def springService = appCtx.getBean("perMethodTransactionalService")
        assertEquals "hasTransaction = false", springService.methodOne()
        assertEquals "hasTransaction = false", springService.methodTwo()
    }

    void testSpringConfiguredService() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("springTransactionalService")
        assertEquals "hasTransaction = true", springService.serviceMethod()
    }

    void testSpringConfiguredServiceConfigDisabled() {
        def appCtx = initializeContext(false)

        def springService = appCtx.getBean("springTransactionalService")
        assertEquals "hasTransaction = false", springService.serviceMethod()
    }

    void testServicesPlugin() {
        def appCtx = initializeContext()

        assertTrue appCtx.containsBean("dataSource")
        assertTrue appCtx.containsBean("someTransactionalService")
        assertTrue appCtx.containsBean("nonTransactionalService")
    }

    private ApplicationContext initializeContext(boolean transactionManagement = true) {

        ga.getConfig().put(Settings.SPRING_TRANSACTION_MANAGEMENT, transactionManagement)
        ga.getConfig().put("dataSources", [dataSource: [pooled: false]])
        def corePluginClass = gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
        def corePlugin = new DefaultGrailsPlugin(corePluginClass, ga)
        def dataSourcePluginClass = gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")

        def domainPluginClass = gcl.loadClass("org.grails.plugins.domain.DomainClassGrailsPlugin")
        def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
        def hibernatePlugin = new DefaultGrailsPlugin(MockHibernateGrailsPlugin, ga)
        def domainPlugin = new DefaultGrailsPlugin(domainPluginClass, ga)
        dataSourcePlugin.manager = new GrailsWebPluginManager([dataSourcePluginClass, MockHibernateGrailsPlugin, domainPluginClass] as Class[], ga)
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        corePlugin.doWithRuntimeConfiguration(springConfig)
        dataSourcePlugin.doWithRuntimeConfiguration(springConfig)
        domainPlugin.doWithRuntimeConfiguration(springConfig)
        hibernatePlugin.doWithRuntimeConfiguration(springConfig)

        def pluginClass = gcl.loadClass("org.grails.plugins.services.ServicesGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        plugin.doWithRuntimeConfiguration(springConfig)


        springConfig.getApplicationContext()
    }
}
