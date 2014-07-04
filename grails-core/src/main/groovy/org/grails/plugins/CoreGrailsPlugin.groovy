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
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.GrailsUtil
import org.grails.core.LegacyGrailsApplication
import org.grails.spring.DefaultRuntimeSpringConfiguration
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator
import grails.core.GrailsApplication
import org.grails.spring.context.support.GrailsPlaceholderConfigurer
import org.grails.spring.context.support.MapBasedSmartPropertyOverrideConfigurer
import org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory
import org.grails.spring.RuntimeSpringConfigUtilities
import org.grails.core.io.DefaultResourceLocator
import grails.core.support.GrailsApplicationAware
import org.grails.spring.beans.GrailsApplicationAwareBeanPostProcessor
import org.grails.spring.beans.PluginManagerAwareBeanPostProcessor
import org.grails.core.support.ClassEditor
import org.grails.dev.support.DevelopmentShutdownHook
import org.grails.beans.support.PropertiesEditor
import grails.core.support.proxy.DefaultProxyHandler
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils

/**
 * Configures the core shared beans within the Grails application context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class CoreGrailsPlugin implements GrailsApplicationAware {

    def version = GrailsUtil.getGrailsVersion()
    def watchedResources = ["file:./grails-app/conf/spring/resources.xml","file:./grails-app/conf/spring/resources.groovy"]

    GrailsApplication grailsApplication

    def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"
        xmlns grailsContext:"http://grails.org/schema/context"
        def application = grailsApplication

        addBeanFactoryPostProcessor(new MapBasedSmartPropertyOverrideConfigurer(application))
        final springEnvironment = getUnrefreshedApplicationContext().getEnvironment()
        final placeholderConfigurer = new GrailsPlaceholderConfigurer(application)
        placeholderConfigurer.environment = springEnvironment
        addBeanFactoryPostProcessor(placeholderConfigurer)
        legacyGrailsApplication(LegacyGrailsApplication, application)

        // replace AutoProxy advisor with Groovy aware one
        def grailsConfig = application.flatConfig
        if (ClassUtils.isPresent('org.aspectj.lang.annotation.Around', application.classLoader) && !grailsConfig.get(Settings.SPRING_DISABLE_ASPECTJ)) {
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareAspectJAwareAdvisorAutoProxyCreator)
        }
        else {
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareInfrastructureAdvisorAutoProxyCreator)
        }

        // Allow the use of Spring annotated components
        context.'annotation-config'()

        def packagesToScan = []

        def beanPackages = grailsConfig.get(Settings.SPRING_BEAN_PACKAGES)
        if (beanPackages instanceof List) {
            packagesToScan += beanPackages
        }

        if (packagesToScan) {
            grailsContext.'component-scan'('base-package':packagesToScan.join(','))
        }

        grailsApplicationPostProcessor(GrailsApplicationAwareBeanPostProcessor, ref("grailsApplication"))

        if (getParentCtx()?.containsBean('pluginManager')) {
            pluginManagerPostProcessor(PluginManagerAwareBeanPostProcessor, ref('pluginManager'))
        } else {
            pluginManagerPostProcessor(PluginManagerAwareBeanPostProcessor)
        }

        classLoader(MethodInvokingFactoryBean) {
            targetObject = ref("grailsApplication")
            targetMethod = "getClassLoader"
        }

        // add shutdown hook if not running in war deployed mode
        final warDeployed = Environment.isWarDeployed()
        final devMode = !warDeployed && Environment.currentEnvironment == Environment.DEVELOPMENT
        if (devMode && ClassUtils.isPresent('jline.Terminal', application.classLoader)) {
            shutdownHook(DevelopmentShutdownHook)
        }
        abstractGrailsResourceLocator {
            if (!warDeployed) {
                BuildSettings settings = BuildSettingsHolder.settings
                if (settings) {
                    def locations = new ArrayList(settings.pluginDirectories.collect { it.absolutePath })
                    locations << settings.baseDir.absolutePath
                    searchLocations = locations
                }
            }
        }
        grailsResourceLocator(DefaultResourceLocator) { bean ->
            bean.parent = "abstractGrailsResourceLocator"
        }

        customEditors(CustomEditorConfigurer) {
            customEditors = [(Class.name): ClassEditor.name,
                             (Properties.name): PropertiesEditor.name]
        }

        proxyHandler(DefaultProxyHandler)
    }

    def doWithDynamicMethods = {
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

    def onChange = { event ->
        if (event.source instanceof Resource) {
            def xmlBeans = new OptimizedAutowireCapableBeanFactory()
            new XmlBeanDefinitionReader(xmlBeans).loadBeanDefinitions(event.source)
            xmlBeans.beanDefinitionNames.each { name ->
                event.ctx.registerBeanDefinition(name, xmlBeans.getBeanDefinition(name))
            }
        }
        else if (event.source instanceof Class) {
            RuntimeSpringConfiguration springConfig = event.ctx != null ? new DefaultRuntimeSpringConfiguration(event.ctx) : new DefaultRuntimeSpringConfiguration()
            RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(springConfig, application, event.source)
            springConfig.registerBeansWithContext(event.ctx)
        }
    }
}
