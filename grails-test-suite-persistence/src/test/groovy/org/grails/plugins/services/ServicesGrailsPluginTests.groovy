package org.grails.plugins.services

import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.grails.commons.test.AbstractGrailsMockTests
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockHibernateGrailsPlugin
import org.springframework.context.ApplicationContext
import org.springframework.transaction.NoTransactionException

class ServicesGrailsPluginTests extends AbstractGrailsMockTests {

    void onSetUp() {
        gcl.parseClass('''
            dataSource {
                pooled = true
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
        def status = TransactionAspectSupport.currentTransactionStatus()
        return "hasTransaction = \${status!=null}"
    }
}

class PerMethodTransactionalService {
    @Transactional
    def methodOne() {
        def status = TransactionAspectSupport.currentTransactionStatus()
        return "hasTransaction = \${status!=null}"
    }

    def methodTwo() {
        def status = TransactionAspectSupport.currentTransactionStatus()
        return "hasTransaction = \${status!=null}"
    }
}
"""
    }

    void testPerMethodTransactionAnnotations() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("perMethodTransactionalService")
        assertEquals "hasTransaction = true", springService.methodOne()
        shouldFail(NoTransactionException) {
            springService.methodTwo()
        }
    }

    void testSpringConfiguredService() {
        def appCtx = initializeContext()

        def springService = appCtx.getBean("springTransactionalService")
        assertEquals "hasTransaction = true", springService.serviceMethod()
    }

    void testServicesPlugin() {
        def appCtx = initializeContext()

        assertTrue appCtx.containsBean("dataSource")
        assertTrue appCtx.containsBean("someTransactionalService")
        assertTrue appCtx.containsBean("nonTransactionalService")
    }

    private ApplicationContext initializeContext() {

        def corePluginClass = gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
        def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
        def dataSourcePluginClass = gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")

        def domainPluginClass = gcl.loadClass("org.grails.plugins.domain.DomainClassGrailsPlugin")
        def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
        def hibernatePlugin = new DefaultGrailsPlugin(MockHibernateGrailsPlugin, ga)
        def domainPlugin = new DefaultGrailsPlugin(domainPluginClass, ga)

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
