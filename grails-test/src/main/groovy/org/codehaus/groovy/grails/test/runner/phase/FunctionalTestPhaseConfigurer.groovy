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
import org.codehaus.groovy.grails.support.PersistenceContextInterceptorExecutor

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
        this.buildSettings = projectRunner.buildSettings
        isForkedRun = buildSettings.forkSettings.run
    }

    @Override
    void prepare() {
        registryCleaner = org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner.createAndRegister()
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener(registryCleaner)


        if (baseUrl) {
            functionalBaseUrl = baseUrl
        } else {
            functionalBaseUrl = (httpsBaseUrl ? 'https' : 'http') + "://${projectRunner.serverHost}:$projectRunner.serverPort$projectRunner.serverContextPath/"
        }

        if (!projectRunner.isServerRunning()) {

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
                projectRunner.projectPackager.packageApplication()
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
                    } catch (IllegalArgumentException e) {
                        // no appCtx configured, ignore
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
    void cleanup() {
        if (!warMode && !isForkedRun) {
            GrailsWebApplicationContext appCtx
            try {
                appCtx = (GrailsWebApplicationContext)Holders.applicationContext
            } catch (IllegalArgumentException e) {
                // no configured app ctx
            }
            if (appCtx) {
                PersistenceContextInterceptorExecutor.destroyPersistenceContext(appCtx)
                appCtx?.close()
            }
        }

        if (!existingServer)
            projectRunner.stopServer()

        functionalBaseUrl = null
        System.setProperty(BuildSettings.FUNCTIONAL_BASE_URL_PROPERTY, '')
        registryCleaner.clean()
        GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(registryCleaner)
    }
}
