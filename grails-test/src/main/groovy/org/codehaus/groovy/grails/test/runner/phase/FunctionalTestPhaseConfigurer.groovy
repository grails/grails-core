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
package org.codehaus.groovy.grails.test.runner.phase

import grails.util.BuildSettings
import grails.util.Holders
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.project.container.GrailsProjectRunner
import org.codehaus.groovy.grails.project.plugins.GrailsProjectPluginLoader
import org.codehaus.groovy.grails.support.PersistenceContextInterceptorExecutor
import grails.build.logging.GrailsConsole

/**
 * Test phase configurer for the functional phase
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class FunctionalTestPhaseConfigurer extends DefaultTestPhaseConfigurer {

    boolean https
    boolean warMode
    String baseUrl
    String httpsBaseUrl

    GrailsProjectRunner projectRunner

    private BuildSettings buildSettings

    private boolean isForkedRun
    private boolean existingServer
    String functionalBaseUrl
    protected MetaClassRegistryCleaner registryCleaner

    FunctionalTestPhaseConfigurer(GrailsProjectRunner projectRunner) {
        this.projectRunner = projectRunner
        buildSettings = projectRunner.buildSettings
        isForkedRun = buildSettings.forkSettings.run
    }

    @Override
    void prepare(Binding testExecutionContext, Map<String, Object> testOptions) {
        Holders.pluginManager = null
        Holders.grailsApplication = null

        warMode = testOptions.war ? true : false
        final packager = projectRunner.projectPackager
        packager.packageApplication()
        final isServerRunning = projectRunner.isServerRunning()
        if (!isServerRunning)  {
            def grailsProjectPluginLoader = new GrailsProjectPluginLoader(null, packager.classLoader,
                packager.buildSettings, projectRunner.buildEventListener)
            final pluginManager = grailsProjectPluginLoader.loadPlugins()
            testExecutionContext.setVariable('pluginManager', pluginManager)
            packager.buildEventListener.binding.setVariable('pluginManager', pluginManager)
            packager.generateWebXml(pluginManager)
        }

        registryCleaner = MetaClassRegistryCleaner.createAndRegister()
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener(registryCleaner)

        if (baseUrl) {
            functionalBaseUrl = baseUrl
        } else {
            functionalBaseUrl = (httpsBaseUrl ? 'https' : 'http') +
                "://${projectRunner.serverHost}:$projectRunner.serverPort$projectRunner.serverContextPath/"
        }

        if (!isServerRunning) {

            packager.createConfig()
            if (warMode) {

                // need to swap out the args map so any test phase/targetting patterns
                // aren't intepreted as the war name.
                projectRunner.warCreator.packageWar()

                if (https) {
                    projectRunner.runWarHttps()
                }
                else {
                    projectRunner.runWar()
                }
            } else {
                final config = projectRunner.projectPackager.packageApplication()
                testExecutionContext.setVariable("config", config)
                if (https) {
                    projectRunner.runAppHttps()
                }
                else {
                    projectRunner.runApp()
                }

                if (!isForkedRun) {
                    try {
                        def appCtx = Holders.applicationContext
                        PersistenceContextInterceptorExecutor.initPersistenceContext(appCtx)
                    } catch (IllegalStateException e) {
                        // no appCtx configured, ignore
                    } catch (IllegalArgumentException e) {
                        // no appCtx configured, ignore
                    }
                }
                else {
                    final console = GrailsConsole.getInstance()
                    console.updateStatus("Waiting for server availablility")
                    int maxWait = 10000
                    int timeout = 0
                    while(true) {
                        if (timeout>maxWait) break
                        try {
                            new URL(functionalBaseUrl).getText(connectTimeout:1000, readTimeout:1000)
                            break
                        } catch (Throwable e) {
                            console.indicateProgress()
                            timeout += 1000
                            sleep(1000)
                        }
                    }
                }
            }

        }
        else {
            existingServer = true
        }

        System.setProperty(buildSettings.FUNCTIONAL_BASE_URL_PROPERTY, functionalBaseUrl)
    }

    @Override
    void cleanup(Binding testExecutionContext, Map<String, Object> testOptions) {
        if (!warMode && !isForkedRun) {
            GrailsWebApplicationContext appCtx
            try {
                appCtx = (GrailsWebApplicationContext)Holders.applicationContext
            } catch (IllegalStateException e ) {
                // no configured app ctx
            } catch (IllegalArgumentException e ) {
                // no configured app ctx
            }
            if (appCtx) {
                PersistenceContextInterceptorExecutor.destroyPersistenceContext(appCtx)
                appCtx?.close()
            }
        }

        if (!existingServer) {
            projectRunner.stopServer()
        }

        functionalBaseUrl = null
        System.setProperty(BuildSettings.FUNCTIONAL_BASE_URL_PROPERTY, '')
        if (registryCleaner) {
            registryCleaner.clean()
            GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(registryCleaner)
        }
    }
}
