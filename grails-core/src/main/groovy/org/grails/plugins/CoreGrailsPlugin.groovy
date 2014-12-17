/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins

import grails.config.Settings
import grails.plugins.Plugin
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsUtil
import org.grails.core.legacy.LegacyGrailsApplication
import org.grails.spring.DefaultRuntimeSpringConfiguration
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator
import org.grails.spring.context.support.GrailsPlaceholderConfigurer
import org.grails.spring.context.support.MapBasedSmartPropertyOverrideConfigurer
import org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory
import org.grails.spring.RuntimeSpringConfigUtilities
import org.grails.core.io.DefaultResourceLocator
import org.grails.spring.beans.GrailsApplicationAwareBeanPostProcessor
import org.grails.spring.beans.PluginManagerAwareBeanPostProcessor
import org.grails.core.support.ClassEditor
import org.grails.dev.support.DevelopmentShutdownHook
import org.grails.beans.support.PropertiesEditor
import grails.core.support.proxy.DefaultProxyHandler
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.context.annotation.ConfigurationClassPostProcessor
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils

/**
 * Configures the core shared beans within the Grails application context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class CoreGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def watchedResources = ["file:./grails-app/conf/spring/resources.xml","file:./grails-app/conf/spring/resources.groovy"]

    @Override
    Closure doWithSpring() { {->
        xmlns grailsContext:"http://grails.org/schema/context"
        def application = grailsApplication

        // enable post-processing of @Configuration beans defined by plugins
        grailsConfigurationClassPostProcessor ConfigurationClassPostProcessor

        addBeanFactoryPostProcessor(new MapBasedSmartPropertyOverrideConfigurer(application))
        final springEnvironment = getUnrefreshedApplicationContext().getEnvironment()
        final placeholderConfigurer = new GrailsPlaceholderConfigurer(application)
        placeholderConfigurer.environment = springEnvironment
        addBeanFactoryPostProcessor(placeholderConfigurer)
        legacyGrailsApplication(LegacyGrailsApplication, application)

        // replace AutoProxy advisor with Groovy aware one
        if (ClassUtils.isPresent('org.aspectj.lang.annotation.Around', application.classLoader) && !config.getProperty(Settings.SPRING_DISABLE_ASPECTJ, Boolean)) {
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareAspectJAwareAdvisorAutoProxyCreator)
        }
        else {
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareInfrastructureAdvisorAutoProxyCreator)
        }

        def packagesToScan = []

        def beanPackages = config.getProperty(Settings.SPRING_BEAN_PACKAGES, List)
        if (beanPackages) {
            packagesToScan += beanPackages
        }

        if (packagesToScan) {
            grailsContext.'component-scan'('base-package':packagesToScan.join(','))
        }

        grailsApplicationPostProcessor(GrailsApplicationAwareBeanPostProcessor, ref("grailsApplication"))
        pluginManagerPostProcessor(PluginManagerAwareBeanPostProcessor)

        classLoader(MethodInvokingFactoryBean) {
            targetObject = ref("grailsApplication")
            targetMethod = "getClassLoader"
        }

        // add shutdown hook if not running in war deployed mode
        final warDeployed = Environment.isWarDeployed()
        final devMode = !warDeployed && environment == Environment.DEVELOPMENT
        if (devMode && ClassUtils.isPresent('jline.Terminal', application.classLoader)) {
            shutdownHook(DevelopmentShutdownHook)
        }
        abstractGrailsResourceLocator {
            searchLocations = [BuildSettings.BASE_DIR.absolutePath]
        }
        grailsResourceLocator(DefaultResourceLocator) { bean ->
            bean.parent = "abstractGrailsResourceLocator"
        }

        customEditors(CustomEditorConfigurer) {
            customEditors = [(Class): ClassEditor,
                             (Properties): PropertiesEditor]
        }

        proxyHandler(DefaultProxyHandler)
    }}

    @Override
    void doWithDynamicMethods() {
        MetaClassRegistry registry = GroovySystem.metaClassRegistry

        def metaClass = registry.getMetaClass(Class)
        if (!(metaClass instanceof ExpandoMetaClass)) {
            registry.removeMetaClass(Class)
            def emc = new ExpandoMetaClass(Class, false, true)
            emc.initialize()
            registry.setMetaClass(Class, emc)

            metaClass = emc
        }

        metaClass.getMetaClass = { ->
            def mc = registry.getMetaClass(delegate)
            if (mc instanceof ExpandoMetaClass) {
                return mc
            }

            registry.removeMetaClass(delegate)
            if (registry.metaClassCreationHandler instanceof ExpandoMetaClassCreationHandle) {
                return registry.getMetaClass(delegate)
            }

            def emc = new ExpandoMetaClass(delegate, false, true)
            emc.initialize()
            registry.setMetaClass(delegate, emc)
            return emc
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        if (event.source instanceof Resource) {
            def xmlBeans = new OptimizedAutowireCapableBeanFactory()
            new XmlBeanDefinitionReader(xmlBeans).loadBeanDefinitions(event.source)
            xmlBeans.beanDefinitionNames.each { name ->
                applicationContext.registerBeanDefinition(name, xmlBeans.getBeanDefinition(name))
            }
        }
        else if (event.source instanceof Class) {
            RuntimeSpringConfiguration springConfig = event.ctx != null ? new DefaultRuntimeSpringConfiguration(event.ctx) : new DefaultRuntimeSpringConfiguration()
            RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(springConfig, grailsApplication, event.source)
            springConfig.registerBeansWithContext(applicationContext)
        }
    }
}
