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

/**
 * A plug-in that provides a lazy initializaed commons logging log property
 * for all classes
 *
 * @author Marc Palmer
 * @since 0.4
 */
class LoggingGrailsPlugin {

	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [core:version]

	def doWithDynamicMethods = { applicationContext ->
        application.artefactHandlers.each() { handler ->
            application."${handler.type}Classes".each() {
                // Formulate a name of the form grails.<artefactType>.classname
                // Do it here so not calculated in every getLog call :)
                def type = GCU.getPropertyNameRepresentation(handler.type)
                def logName = "grails.app.${type}.${it.clazz.name}".toString()

                def log = LogFactory.getLog( logName)

                it.clazz.metaClass.getLog << { -> log }
            }
        }
	}
}