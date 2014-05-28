package org.grails.boot.support

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enhances any ApplicationContext with plugin manager capabilities
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    GrailsApplicationPostProcessor(Class...classes) {
        grailsApplication = new DefaultGrailsApplication( classes as Class[] )
        grailsApplication.initialise()
        pluginManager = new DefaultGrailsPluginManager(grailsApplication)
        pluginManager.loadPlugins()
    }

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        def springConfig = new DefaultRuntimeSpringConfiguration()
        springConfig.setBeanFactory((ListableBeanFactory) registry)
        pluginManager.doRuntimeConfiguration(springConfig)
        springConfig.registerBeansWithRegistry(registry)

    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, grailsApplication)
        beanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, pluginManager)
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(this)
        }
    }

    @Override
    void onApplicationEvent(ContextRefreshedEvent event) {
        def context = event.applicationContext
        pluginManager.setApplicationContext(context)
        pluginManager.doDynamicMethods()
        pluginManager.doPostProcessing(context)
    }

}