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

/**
 * Gant script that bootstraps a running Grails instance without a
 * servlet container.
 *
 * @author Graeme Rocher
 */

includeTargets << grailsScript("_GrailsPackage")

parentContext = null // default parent context is null

projectLoader = new org.codehaus.groovy.grails.project.loader.GrailsProjectLoader(projectPackager)

target(loadApp:"Loads the Grails application object") {
    grailsApp = projectLoader.loadApplication()
    pluginManager = projectLoader.pluginManager
    servletContext = projectLoader.servletContext
    appCtx = projectLoader.applicationContext

    event("AppLoadEnd", ["Loading Grails Application"])
}

target(configureApp:"Configures the Grails application and builds an ApplicationContext") {
    appCtx = projectLoader.configureApplication()
    pluginManager = projectLoader.pluginManager
    servletContext = projectLoader.servletContext
    appCtx = projectLoader.applicationContext
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
    configureApp()
}

target(bootstrapOnce:"Loads and configures a Grails instance only if it is not already loaded and configured") {
    depends(enableExpandoMetaClass)
    if (!binding.variables.applicationLoaded) {
        loadApp()
        configureApp()
    }
}

target(enableExpandoMetaClass: "Calls ExpandoMetaClass.enableGlobally()") {
    ExpandoMetaClass.enableGlobally()
}
