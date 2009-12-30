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
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.mock.jndi.SimpleNamingContextBuilder


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
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				resources = pluginSettings.artefactResources
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication, ref("grailsResourceLoader"))
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
    grailsApp.initialise()
	event("AppLoadEnd", ["Loading Grails Application"])
}

target(configureApp:"Configures the Grails application and builds an ApplicationContext") {
    appCtx.resourceLoader = new  CommandLineResourceLoader()
	profile("Performing runtime Spring configuration") {
	    def configurer = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
        def jndiEntries = config?.grails?.naming?.entries
        if(jndiEntries instanceof Map) {            
            def builder = new SimpleNamingContextBuilder()
            jndiEntries.each { key, val ->
                builder.bind("java:comp/env/$key", val)
            }
            builder.activate()
        }
        appCtx = configurer.configure(servletContext)
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,appCtx );
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
	}
}

target(shutdownApp:"Shuts down the running Grails application") {
    appCtx?.close()
    ApplicationHolder.setApplication(null);
    ServletContextHolder.setServletContext(null);
    PluginManagerHolder.setPluginManager(null);
    ConfigurationHolder.setConfig(null);
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

    long lastModified = classesDir.lastModified()
    while(keepMonitoring) {
        sleep(10000)
        try {
            pluginManager.checkForChanges()

            lastModified = recompileCheck(lastModified) {
                compile()
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader()
                classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], contextLoader.rootLoader)
                Thread.currentThread().setContextClassLoader(classLoader)
                // reload plugins
                loadPlugins()
                loadApp()
                configureApp()
                monitorRecompileCallback()
            }

        } catch (Exception e) {
            logError("Error recompiling application",e)
        } finally {
            monitorCheckCallback()
        }
    }
}

target(bootstrap: "Loads and configures a Grails instance") {
    packageApp()
    loadApp()
    configureApp()
}
