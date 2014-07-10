/*
 * Copyright 2013-2014 SpringSource
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
package org.grails.test.runner.phase

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Holders
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import grails.web.servlet.context.GrailsWebApplicationContext
import org.codehaus.groovy.grails.project.container.GrailsProjectRunner
import grails.persistence.support.PersistenceContextInterceptorExecutor
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
    boolean httpsBaseUrl

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

        https = testOptions.https ? true : false
        warMode = testOptions.war ? true : false
        baseUrl =  testOptions.baseUrl
        httpsBaseUrl = testOptions.httpsBaseUrl ? true : false

        final packager = projectRunner.projectPackager
        packager.packageApplication()
        final isServerRunning = projectRunner.isServerRunning()
        registryCleaner = MetaClassRegistryCleaner.createAndRegister()

        if (!isServerRunning) {

            packager.createConfig()
            if (warMode) {
                // need to swap out the args map so any test phase/targeting patterns
                // aren't interpreted as the war name.
                if( !Environment.isFork() ) {
                    projectRunner.warCreator.packageWar()
                }

                if (https) {
                    projectRunner.runWarHttps()
                }
                else {
                    projectRunner.runWar()
                }
                initFunctionalBaseUrl()
            } else {
                final config = projectRunner.projectPackager.packageApplication()
                testExecutionContext.setVariable("config", config)

                if (https) {
                    projectRunner.runAppHttps()
                }
                else {
                    projectRunner.runApp()
                }
                initFunctionalBaseUrl()

                if (!isForkedRun) {
                    initPersistenceContext()
                }
                else {
                    waitForServer()
                }
            }

        }
        else {
            existingServer = true
            initFunctionalBaseUrl()
        }
    }

    private void waitForServer() {
        final console = GrailsConsole.getInstance()
        console.updateStatus("Waiting for server availability")

        int maxWait = 10000
        int timeout = 0
        while (timeout <= maxWait) {
            try {
                new URL(functionalBaseUrl).getText(connectTimeout: 1000, readTimeout: 1000, "UTF-8")
                break
            } catch (Throwable ignored) {
                console.indicateProgress()
                timeout += 1000
                sleep(1000)
            }
        }
    }

    @Override
    void cleanup(Binding testExecutionContext, Map<String, Object> testOptions) {
        if (!warMode && !isForkedRun) {
            destroyPersistenceContext()
        }

        if (!existingServer) {
            projectRunner.stopServer()
        }

        clearFunctionalBaseUrl()

        if (registryCleaner) {
            MetaClassRegistryCleaner.cleanAndRemove(registryCleaner)
        }
    }

    private void initFunctionalBaseUrl() {
        if (baseUrl) {
            functionalBaseUrl = baseUrl
        } else {
            functionalBaseUrl = httpsBaseUrl ? projectRunner.urlHttps : projectRunner.url
            functionalBaseUrl += '/'
        }

        System.setProperty(buildSettings.FUNCTIONAL_BASE_URL_PROPERTY, functionalBaseUrl)
    }

    private void clearFunctionalBaseUrl() {
        functionalBaseUrl = null
        System.setProperty(BuildSettings.FUNCTIONAL_BASE_URL_PROPERTY, '')
    }

    private static void initPersistenceContext() {
        try {
            def appCtx = Holders.applicationContext
            PersistenceContextInterceptorExecutor.initPersistenceContext(appCtx)
        } catch (IllegalStateException ignored) {
            // no appCtx configured, ignore
        } catch (IllegalArgumentException ignored) {
            // no appCtx configured, ignore
        }
    }

    private static void destroyPersistenceContext() {
        GrailsWebApplicationContext appCtx
        try {
            appCtx = (GrailsWebApplicationContext)Holders.applicationContext
        } catch (IllegalStateException ignored) {
            // no configured app ctx
        } catch (IllegalArgumentException ignored) {
            // no configured app ctx
        }
        if (appCtx) {
            PersistenceContextInterceptorExecutor.destroyPersistenceContext(appCtx)
            appCtx?.close()
        }
    }
}
