/*
 * Copyright 2013 SpringSource
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
package org.codehaus.groovy.grails.project.loader

import grails.spring.BeanBuilder
import grails.spring.WebBeanBuilder
import grails.util.BuildSettings
import grails.util.Holders
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.jndi.JndiBindingSupport
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.compiler.GrailsProjectCompiler
import org.codehaus.groovy.grails.core.io.PluginPathAwareFileSystemResourceLoader
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.project.packaging.GrailsProjectPackager
import org.codehaus.groovy.grails.project.plugins.GrailsProjectPluginLoader
import org.codehaus.groovy.grails.support.CommandLineResourceLoader
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

import javax.servlet.ServletContext

/**
 * Capable of bootstrapping a Grails project and returning the loaded ApplicationContext
 *
 * @author Graeme Rocher
 * @since 2.3
 */
//@CompileStatic  // TODO: report Groovy bug, produces VerifierError with constructors
class GrailsProjectLoader extends BaseSettingsApi{

    ApplicationContext parentContext
    ApplicationContext applicationContext
    ServletContext servletContext
    GrailsPluginManager pluginManager
    boolean applicationLoaded

    GrailsProjectPackager projectPackager

    GrailsProjectLoader(BuildSettings buildSettings) {
        super(buildSettings, new GrailsBuildEventListener( new GroovyClassLoader(Thread.currentThread().contextClassLoader), new Binding(),buildSettings), false)
        final projectCompiler = new GrailsProjectCompiler(new PluginBuildSettings(buildSettings))
        projectCompiler.configureClasspath()
        this.projectPackager = new GrailsProjectPackager(projectCompiler)

    }

    GrailsProjectLoader(GrailsProjectPackager projectPackager) {
        super(projectPackager.buildSettings, projectPackager.buildEventListener, projectPackager.isInteractive)
        this.projectPackager = projectPackager
    }

    /**
     * Loads the Grails application object
     * @return
     */
    @CompileStatic
    GrailsApplication loadApplication() {
        buildEventListener.triggerEvent("AppLoadStart", ["Loading Grails Application"])
        BeanBuilder beanDefinitions
        profile("Loading parent ApplicationContext") {
            def builder = parentContext ? new WebBeanBuilder(parentContext) :  new WebBeanBuilder()
            beanDefinitions = defineParentBeans(builder)
        }

        applicationContext = beanDefinitions.createApplicationContext()
        def ctx = applicationContext

        // The mock servlet context needs to resolve resources relative to the 'web-app'
        // directory. We also need to use a FileSystemResourceLoader, otherwise paths are
        // evaluated against the classpath - not what we want!
        def resourceLoader = new PluginPathAwareFileSystemResourceLoader()
        def locations = new ArrayList(buildSettings.pluginDirectories.collect { File it -> it.absolutePath })
        locations << buildSettings.baseDir.absolutePath
        resourceLoader.searchLocations = locations
        servletContext = new MockServletContext('web-app', resourceLoader)
        if (ctx instanceof GrailsWebApplicationContext) {
            ctx.servletContext = servletContext
        }
        GrailsApplication grailsApp = ctx.getBean(GrailsApplication)
        Holders.setGrailsApplication(grailsApp)
        projectPackager.packageApplication()
        Holders.setPluginManager(null)
        GrailsProjectPluginLoader pluginLoader = new GrailsProjectPluginLoader(grailsApp, grailsApp.classLoader, buildSettings, buildEventListener)
        pluginManager = pluginLoader.loadPlugins()
        pluginManager.application = grailsApp
        pluginManager.doArtefactConfiguration()

        registerPluginManagerWithContext(ctx)

        grailsApp.initialise()
        buildEventListener.triggerEvent("AppLoadEnd", ["Loading Grails Application"])
        return grailsApp
    }

    /**
     * Configures the Grails application and builds an ApplicationContext
     * @return The ApplicationContext
     */
    @CompileStatic
    ApplicationContext configureApplication() {
        buildEventListener.triggerEvent("AppCfgStart", ["Configuring Grails Application"])
        GrailsApplication grailsApplication = loadApplication()
        if (applicationContext instanceof GrailsApplicationContext)
            applicationContext.resourceLoader = new  CommandLineResourceLoader()
            profile("Performing runtime Spring configuration") {
            def configurer = new GrailsRuntimeConfigurator(grailsApplication, applicationContext)
            configureJndi(grailsApplication)
            applicationContext = configurer.configure(servletContext)
            servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,applicationContext)
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext)
        }
        applicationLoaded = true
        buildEventListener.triggerEvent("AppCfgEnd", ["Configuring Grails Application"])
        return applicationContext
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureJndi(GrailsApplication grailsApplication) {
        def jndiEntries = grailsApplication.config?.grails?.naming?.entries

        if ((jndiEntries instanceof Map) && jndiEntries) {
            def jndiBindingSupport = new JndiBindingSupport(jndiEntries)
            jndiBindingSupport.bind()
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerPluginManagerWithContext(ApplicationContext ctx) {
        def builder = new WebBeanBuilder(ctx)
        def newBeans = builder.beans {
            delegate."pluginManager"(MethodInvokingFactoryBean) {
                targetClass = Holders
                targetMethod = "getPluginManager"
            }
        }
        newBeans.beanDefinitions.each { name, definition ->
            ctx.registerBeanDefinition(name, definition)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected BeanBuilder defineParentBeans(WebBeanBuilder builder) {
        builder.beans {
            grailsApplication(DefaultGrailsApplication, pluginSettings.getArtefactResourcesForCurrentEnvironment())
        }
    }
}
