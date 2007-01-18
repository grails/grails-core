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
 * A plug-in that configures pluggable codecs 
 * 
 * @author Jeff Brown
 * @since 0.4
 */
class CodecsGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [core:version]
	def watchedResources = "**/grails-app/utils/*Codec.groovy"

	def onChange = { event ->
		if(GCU.isCodecClass(event.source)) {
			application.addCodecClass(event.source)
		}
	}

	def doWithDynamicMethods = { applicationContext ->
		application.codecClasses.each {
			def codecName = it.name
			def codecClassName = it.fullName

			def encodeMethodName = "encode${codecName}"
			String.metaClass."${encodeMethodName}" << {
				def codecClass = application.getGrailsCodecClass(codecClassName)
				def encodeMethod = codecClass.encodeMethod
				if(encodeMethod) {
					return encodeMethod(delegate)
				} else {
					throw new MissingMethodException(encodeMethodName, String, [] as Object[])
				}
			}

			def decodeMethodName = "decode${codecName}" 
			String.metaClass."${decodeMethodName}" << {
				def codecClass = application.getGrailsCodecClass(codecClassName)
				def decodeMethod = codecClass.decodeMethod
				if(decodeMethod) {
					return decodeMethod(delegate)
				} else {
					throw new MissingMethodException(decodeMethodName, String, [] as Object[])
				}
			}
		}
	}
}