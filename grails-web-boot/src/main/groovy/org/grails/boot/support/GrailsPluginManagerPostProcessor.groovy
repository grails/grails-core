package org.grails.boot.support

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
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
class GrailsPluginManagerPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    GrailsPluginManager pluginManager

    GrailsPluginManagerPostProcessor(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
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