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

import grails.spring.WebBeanBuilder
import org.codehaus.groovy.grails.support.CommandLineResourceLoader
import org.codehaus.groovy.grails.cli.support.JndiBindingSupport
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean
import org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

/**
 * Gant script that bootstraps a running Grails instance without a
 * servlet container.
 *
 * @author Graeme Rocher
 */

includeTargets << grailsScript("_GrailsPackage")

parentContext = null // default parent context is null

target(loadApp:"Loads the Grails application object") {
    event("AppLoadStart", ["Loading Grails Application"])
    profile("Loading parent ApplicationContext") {
        def builder = parentContext ? new WebBeanBuilder(parentContext) :  new WebBeanBuilder()
        beanDefinitions = builder.beans {
            resourceHolder(GrailsResourceHolder) {
                resources = pluginSettings.artefactResources
            }
            grailsResourceLoader(GrailsResourceLoaderFactoryBean) {
                grailsResourceHolder = resourceHolder
            }
            grailsApplication(DefaultGrailsApplication, ref("grailsResourceLoader"))
        }
    }

    appCtx = beanDefinitions.createApplicationContext()
    def ctx = appCtx

    // The mock servlet context needs to resolve resources relative to the 'web-app'
    // directory. We also need to use a FileSystemResourceLoader, otherwise paths are
    // evaluated against the classpath - not what we want!
    servletContext = new MockServletContext('web-app', new FileSystemResourceLoader())
    ctx.servletContext = servletContext
    grailsApp = ctx.grailsApplication
    ApplicationHolder.application = grailsApp
    classLoader = grailsApp.classLoader
    packageApp()
    PluginManagerHolder.pluginManager = null
    loadPlugins()
    pluginManager = PluginManagerHolder.pluginManager
    pluginManager.application = grailsApp
    pluginManager.doArtefactConfiguration()

    def builder = new WebBeanBuilder(ctx)
    newBeans = builder.beans {
        delegate."pluginManager"(MethodInvokingFactoryBean) {
            targetClass = PluginManagerHolder
            targetMethod = "getPluginManager"
        }
    }
    newBeans.beanDefinitions.each { name, definition ->
        ctx.registerBeanDefinition(name, definition)
    }

    grailsApp.initialise()
    event("AppLoadEnd", ["Loading Grails Application"])
}

target(configureApp:"Configures the Grails application and builds an ApplicationContext") {
    event("AppCfgStart", ["Configuring Grails Application"])
    appCtx.resourceLoader = new  CommandLineResourceLoader()
    profile("Performing runtime Spring configuration") {
        def configurer = new GrailsRuntimeConfigurator(grailsApp, appCtx)
        def jndiEntries = config?.grails?.naming?.entries

        if (jndiEntries instanceof Map) {
            def jndiBindingSupport = new JndiBindingSupport(jndiEntries)
            jndiBindingSupport.bind()
        }
        appCtx = configurer.configure(servletContext)
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }
	applicationLoaded = true
    event("AppCfgEnd", ["Configuring Grails Application"])
}

// Flag that determines whether the monitor loop should keep running.
keepMonitoring = true

// Callback invoked by the monitor each time it has checked for changes.
monitorCheckCallback = {}

// Callback invoked by the monitor each time it recompiles the app and
// restarts it.
monitorRecompileCallback = {}

target(monitorApp:"Monitors an application for changes using the PluginManager and reloads changes") {
    depends(classpath)
	// do nothing. Deprecated, purely here for compatibility
}

target(bootstrap: "Loads and configures a Grails instance") {
	packageApp()
    loadApp()
    configureApp()
}

target(bootstrapOnce:"Loads and configures a Grails instance only if it is not already loaded and configured") {
	if(!binding.variables.applicationLoaded) {
		loadApp()
		configureApp()
	}
}