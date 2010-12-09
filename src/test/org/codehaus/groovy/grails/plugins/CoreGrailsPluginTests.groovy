package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator

import grails.spring.BeanBuilder

class CoreGrailsPluginTests extends AbstractGrailsMockTests {

    void testCorePlugin() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
        assert appCtx.getBean("org.springframework.aop.config.internalAutoProxyCreator") instanceof GroovyAwareAspectJAwareAdvisorAutoProxyCreator
    }

    void testDisableAspectj() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()
        ga.config.grails.spring.disable.aspectj.autoweaving=true
        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
        assert appCtx.getBean("org.springframework.aop.config.internalAutoProxyCreator") instanceof GroovyAwareInfrastructureAdvisorAutoProxyCreator

    }

    protected void onSetUp() {
        // needed for testBeanPropertyOverride
        gcl.parseClass("""
            class SomeTransactionalService {
                boolean transactional = true
                Integer i
            }
            class NonTransactionalService {
                boolean transactional = false
                Integer i
            }
        """)
    }

    /**
     * Tests the ability to set bean properties via the application config.
     *
     * @author Luke Daley
     */
    void testBeanPropertyOverride() {
        ConfigurationHolder.config = new ConfigSlurper().parse('''
            dataSource {
                pooled = true
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                dbCreate = "create-drop"
            }
            beans {
                someTransactionalService {
                    i = 1
                }
                nonTransactionalService {
                    i = 2
                }
            }
        ''')

        def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
        def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
        def hibernatePluginClass = gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.MockHibernateGrailsPlugin")
        def hibernatePlugin = new DefaultGrailsPlugin(hibernatePluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        corePlugin.doWithRuntimeConfiguration(springConfig)
        dataSourcePlugin.doWithRuntimeConfiguration(springConfig)
        hibernatePlugin.doWithRuntimeConfiguration(springConfig)

        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assertEquals(1, appCtx.getBean('someTransactionalService').i)
        assertEquals(2, appCtx.getBean('nonTransactionalService').i)

        // test that the overrides are applied on a reload - GRAILS-5763
        plugin.manager = [informObservers: { String pluginName, Map event -> }] as GrailsPluginManager
        plugin.applicationContext = appCtx
        
        ["SomeTransactionalService", "NonTransactionalService"].each {
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, gcl.loadClass(it))
        }
        
        assertEquals(1, appCtx.getBean('someTransactionalService').i)
        assertEquals(2, appCtx.getBean('nonTransactionalService').i)
    }
    
    // See GRAILS-6790
    void testAopConfigurationIsEffective() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        
        plugin.doWithRuntimeConfiguration(springConfig)
        
        def bb = new BeanBuilder(ctx, springConfig, null)
        bb.beans {
            xmlns aop: "http://www.springframework.org/schema/aop"

            aop.config("proxy-target-class": true) {

                aspect(id: "myPlainJavaAspect-id", ref: "myAspect") { 
                    "before" method: "myBeforeAdvice", pointcut: "execution(* myMethod1(..))" 
                }
            }

            myInterfaceImpl(CoreGrailsPluginTestsAopConfig.MyInterfaceImpl)
            myAspect(CoreGrailsPluginTestsAopConfig.MyAspect)
        }

        def appCtx = springConfig.getApplicationContext()
        def bean = appCtx.getBean("myInterfaceImpl")

        // if we can call the following method, then the proxy was class based
        // because 'myMethod2' is not on the only interface this implements
        bean.myMethod2()
    }
    
    
}
