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

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.codecs.*
import org.codehaus.groovy.grails.commons.*

/**
 * A plug-in that configures pluggable codecs 
 * 
 * @author Jeff Brown
 * @since 0.4
 */
class CodecsGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [core:version]
	def watchedResources = "file:./grails-app/utils/**/*Codec.groovy"
	def providedArtefacts = [
                               HTMLCodec,
                               JavaScriptCodec,
                               URLCodec,
                               Base64Codec
                            ]

	def onChange = { event ->
		if(application.isArtefactOfType(CodecArtefactHandler.TYPE, event.source)) {
			application.addArtefact(CodecArtefactHandler.TYPE, event.source)
		}
	}

	def doWithDynamicMethods = { applicationContext ->
		application.codecClasses.each {
			def codecName = it.name
			def codecClassName = it.fullName


			def encodeMethodName = "encodeAs${codecName}"
			def decodeMethodName = "decode${codecName}"

            def encoder
            def decoder
            if (GrailsUtil.isDevelopmentEnv()) {
                // Resolve codecs in every call in case of a codec reload
                encoder = {->
                    def codecClass = application.getCodecClass(codecClassName)
                    def encodeMethod = codecClass.encodeMethod
                    if(encodeMethod) {
                        return encodeMethod(delegate)
                    } else {
                        // note the call to delegate.getClass() instead of the more groovy delegate.class.
                        // this is because the delegate might be a Map, in which case delegate.class doesn't
                        // do what we want here...
                        throw new MissingMethodException(encodeMethodName, delegate.getClass(), [] as Object[])
                    }
                }
                decoder = {->
                    def codecClass = application.getCodecClass(codecClassName)
                    def decodeMethod = codecClass.decodeMethod
                    if(decodeMethod) {
                        return decodeMethod(delegate)
                    } else {
                        // note the call to delegate.getClass() instead of the more groovy delegate.class.
                        // this is because the delegate might be a Map, in which case delegate.class doesn't
                        // do what we want here...
                        throw new MissingMethodException(decodeMethodName, delegate.getClass(), [] as Object[])
                    }
                }
            } else {
                // Resolve codec methods once only at startup
                def codecClass = application.getCodecClass(codecClassName)
                def encodeMethod = codecClass.encodeMethod
                def decodeMethod = codecClass.decodeMethod
                if(encodeMethod) {
                    encoder = {-> encodeMethod(delegate) }
                } else {
                    // note the call to delegate.getClass() instead of the more groovy delegate.class.
                    // this is because the delegate might be a Map, in which case delegate.class doesn't
                    // do what we want here...
                    encoder = {-> throw new MissingMethodException(encodeMethodName, delegate.getClass(), [] as Object[]) }
                }
                if(decodeMethod) {
                    decoder = {-> decodeMethod(delegate) }
                } else {
                    // note the call to delegate.getClass() instead of the more groovy delegate.class.
                    // this is because the delegate might be a Map, in which case delegate.class doesn't
                    // do what we want here...
                    decoder = {-> throw new MissingMethodException(decodeMethodName, delegate.getClass(), [] as Object[]) }
                }
            }

			Object.metaClass."${encodeMethodName}" << encoder
			Object.metaClass."${decodeMethodName}" << decoder
		}
	}
}