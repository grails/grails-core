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
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode
import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.project.container.GrailsProjectRunner

/**
 * Executes Grails using an embedded server.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsWar")

SCHEME_HTTP = GrailsProjectRunner.SCHEME_HTTP
SCHEME_HTTPS = GrailsProjectRunner.SCHEME_HTTPS

projectRunner = new GrailsProjectRunner(projectPackager, warCreator, classLoader)

// Keep track of whether we're running in HTTPS mode in case we need
// to restart the server.
usingSecureServer = false

grailsServer = null
grailsContext = null
autoRecompile = System.getProperty("disable.auto.recompile") ? !(System.getProperty("disable.auto.recompile").toBoolean()) : true

// How often should recompilation occur while the application is running (in seconds)?
// Defaults to 3s.
recompileFrequency = System.getProperty("recompile.frequency")
recompileFrequency = recompileFrequency ? recompileFrequency.toInteger() : 3

// Should the reloading agent be enabled? By default, yes...
isReloading = System.getProperty("grails.reload.enabled")
isReloading = isReloading != null ? isReloading.toBoolean() : true

// This isn't used within this script but may come in handy for scripts
// that depend on this one.
ant.path(id: "grails.runtime.classpath", runtimeClasspath)

/**
 * Runs the application in dev mode, i.e. with class-reloading.
 */
target(runApp: "Main implementation that executes a Grails application") {
    grailsServer = projectRunner.runApp()
}

/**
 * Runs the application in dev mode over HTTPS.
 */
target(runAppHttps: "Main implementation that executes a Grails application with an HTTPS listener") {
    grailsServer = projectRunner.runAppHttps()
}

/**
 * Runs the application using the WAR file directly.
 */
target(runWar: "Main implementation that executes a Grails application WAR") {
    grailsServer = projectRunner.runWar()
}

/**
 * Runs the application over HTTPS using the WAR file directly.
 */
target(runWarHttps: "Main implementation that executes a Grails application WAR") {
    grailsServer = projectRunner.runWarHttps()
}

/**
 * Starts the plugin scanner. Call this after starting the server if you
 * want changes to artifacts automatically detected and loaded.
 */
target(startPluginScanner: "Starts the plugin manager's scanner that detects changes to artifacts.") {
    if (!GrailsProjectWatcher.isReloadingAgentPresent() || GrailsProjectWatcher.isActive()) {
        return
    }

    if (isReloading) {        
        new GrailsProjectWatcher(projectCompiler, pluginManager).with {
            reloadExcludes = (config?.grails?.reload?.excludes instanceof List) ? config?.grails?.reload?.excludes : []
            reloadIncludes = (config?.grails?.reload?.includes instanceof List) ? config?.grails?.reload?.includes : []
            start()
        }
    }
}

target(stopPluginScanner: "Stops the plugin manager's scanner that detects changes to artifacts.") {
    // do nothing, here for compatibility
}

/**
 * Keeps the server alive and checks for changes in domain classes or
 * source files under "src". If any changes are detected, the servlet
 * container is restarted.
 */
target(watchContext: "Watches the WEB-INF/classes directory for changes and restarts the server if necessary") {
    depends(classpath)

    def im = InteractiveMode.current
    if (!im) {
        keepServerAlive()
        return
    }

    Thread.start {
        im.grailsServer = grailsServer
        im.run()
    }

    keepServerAlive()
}

target(keepServerAlive: "Idles the script, ensuring that the server stays running.") {
    boolean keepRunning = true
    def killFile = new File(basedir, '.kill-run-app')
    if (killFile.exists()) {
        grailsConsole.warning ".kill-run-app file exists - perhaps a previous server stop didn't work?. Deleting and continuing anyway."
        killFile.delete()
    }

    while (keepRunning || Boolean.getBoolean("TomcatKillSwitch.active")) {
        sleep(recompileFrequency * 1000)

        // Check whether the kill file exists. This is a hack for the functional
        // tests so that we can stop the servers that are started.
        if (killFile.exists()) {
            grailsConsole.updateStatus "Stopping server..."
            grailsServer.stop()
            killFile.delete()
            keepRunning = false
        }
    }
}

target(stopServer: "Stops the Grails servlet container") {
    projectWatcher?.stopServer()
}
