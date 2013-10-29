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
package org.codehaus.groovy.grails.project.ui

import grails.util.BuildSettings
import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.ui.Console

import java.awt.Window

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.project.loader.GrailsProjectLoader
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.context.ApplicationContext

/**
 * Loads the Grails console Swing UI
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsProjectConsole extends BaseSettingsApi {

    GrailsProjectLoader projectLoader

    GrailsProjectConsole(BuildSettings buildSettings) {
        super(buildSettings, false)
        projectLoader = new GrailsProjectLoader(buildSettings)
    }

    GrailsProjectConsole(GrailsProjectLoader projectLoader) {
        super(projectLoader.buildSettings, projectLoader.buildEventListener, false)
        this.projectLoader = projectLoader
    }

    Console run()  {
        ApplicationContext applicationContext = projectLoader.configureApplication()
        GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication)

        Console groovyConsole = createConsole(applicationContext, grailsApplication)
        groovyConsole.run()

        if(GrailsProjectWatcher.isReloadingAgentPresent()) {
            def watcher = new GrailsProjectWatcher(projectLoader.projectPackager.projectCompiler, applicationContext.getBean(GrailsPluginManager))
            watcher.start()
        }

        sleepWhileActive()

        return groovyConsole
    }

    Console createConsole() {
        ApplicationContext applicationContext = projectLoader.configureApplication()
        GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication)

        createConsole(applicationContext, grailsApplication)
    }

    protected void sleepWhileActive() {
        // Keep the console running until all windows are closed unless the
        // interactive console is in use. The interactive console keeps the
        // VM alive so we don't need to keep this thread running.
        while (!InteractiveMode.isActive() && Window.windows.any { Window it -> it.visible }) {
            sleep 3000
        }
    }

    protected Console createConsole(ApplicationContext applicationContext, GrailsApplication grailsApplication) {
        def b = new Binding()
        b.setVariable("ctx",applicationContext)
        b.setVariable("grailsApplication", grailsApplication)
        def groovyConsole = new Console(grailsApplication.classLoader, b)  {
            @Override
            void exit(EventObject evt) {
                super.exit(evt)
                if (Environment.isFork()) {
                    System.exit(0)
                }
            }
        }
        groovyConsole.beforeExecution = {
            applicationContext.getBeansOfType(PersistenceContextInterceptor).each { String k,  PersistenceContextInterceptor v ->
                v.init()
            }
        }
        groovyConsole.afterExecution = {
            applicationContext.getBeansOfType(PersistenceContextInterceptor).each { String k,  PersistenceContextInterceptor v ->
                v.flush()
                v.destroy()
            }
        }
        groovyConsole
    }
}
