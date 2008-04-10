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
package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.apache.commons.logging.LogFactory
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * A plug-in that provides a lazy initialized commons logging log property
 * for all classes
 *
 * @author Marc Palmer
 * @since 0.4
 */
class LoggingGrailsPlugin {

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version]
    def observe = ['*']

    def doWithWebDescriptor = {xml ->
        def contextParams = xml.'context-param'
        def log4j = contextParams.find {it.'param-name'.text() == 'log4jConfigLocation'}

        def runningScript = System.getProperty('current.gant.script')

        if(log4j) {

             if(runningScript != 'war' && runningScript != 'run-war') {
                 def resources = System.getProperty(GrailsApplication.PROJECT_RESOURCES_DIR)
                 log4j.'param-value' = "file:$resources/log4j.properties"
                 if (GrailsUtil.isDevelopmentEnv() && !application.warDeployed) {
                     log4j + {
                         'context-param' {
                             'param-name'('log4jRefreshInterval')
                             'param-value'(1000)
                         }
                     }
                 }
             }
        }

    }

    def doWithDynamicMethods = {applicationContext ->
        application.artefactHandlers.each() {handler ->
            application."${handler.type}Classes".each() {
                addLogMethod(it.clazz, handler)
            }
        }
    }

    def onConfigChange = {event ->
        def log4jConfig = event.source.log4j
        if (log4jConfig) {
            def props = log4jConfig.toProperties('log4j')
            log.info "Updating Log4j configuration.."
            def resourcesDir = System.getProperty(GrailsApplication.PROJECT_RESOURCES_DIR)
            if(resourcesDir) {
                new File("$resourcesDir/log4j.properties").withOutputStream {out ->
                    props.store(out, "Grails' Log4j Configuration")
                }
            }
        }
    }

    def onChange = {event ->
        if (event.source instanceof Class) {
            log.debug "Adding log method to modified artefact [${event.source}]"
            def handler = application.artefactHandlers.find {it.isArtefact(event.source)}
            if (handler) {
                addLogMethod(event.source, handler)
            }
        }
    }

    def addLogMethod(artefactClass, handler) {
        // Formulate a name of the form grails.<artefactType>.classname
        // Do it here so not calculated in every getLog call :)
        def type = GCU.getPropertyNameRepresentation(handler.type)
        def logName = "grails.app.${type}.${GCU.getShortName(artefactClass)}".toString()

        def log = LogFactory.getLog(logName)

        artefactClass.metaClass.getLog << {-> log}
    }
}