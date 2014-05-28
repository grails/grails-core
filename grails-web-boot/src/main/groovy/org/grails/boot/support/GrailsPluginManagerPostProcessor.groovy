package org.grails.boot.support

import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enhances any ApplicationContext with plugin manager capabilities
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GrailsPluginManagerPostProcessor implements BeanDefinitionRegistryPostProcessor {

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
}