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

import org.codehaus.groovy.grails.plugins.support.*
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

/**
 * A plug-in that configures pluggable encoders 
 * 
 * @author Jeff Brown
 * @since 0.4
 */
class EncodersGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [core:version]
	def watchedResources = "**/grails-app/utils/*Encoder.groovy"

	def onChange = { event ->
		if(GCU.isEncoderClass(event.source)) {
			def encoderClass = application.addEncoderClass(event.source)
		}
	}

	def doWithDynamicMethods = { applicationContext ->
		application.encoderClasses.each {
			def encoderName = it.fullName
			String.metaClass."encodeAs${encoderName - 'Encoder'}" << {
				def encoderClass = application.getGrailsEncoderClass(encoderName)
				def encodeMethod = encoderClass.encodeMethod
				if(encodeMethod) {
					return encodeMethod(delegate)
				} else {
					throw new Exception("Could not find encode method for ${encoderName}")
				}
			}

			String.metaClass."decodeAs${encoderName - 'Encoder'}" << {
				def encoderClass = application.getGrailsEncoderClass(encoderName)
				def decodeMethod = encoderClass.decodeMethod
				if(decodeMethod) {
					return decodeMethod(delegate)
				} else {
					throw new Exception("Could not find decode method for ${encoderName}")
				}
			}
		}
	}
}