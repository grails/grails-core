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
import grails.util.Environment

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
                               Base64Codec,
                               MD5Codec,
                               MD5BytesCodec,
                               HexCodec,
                               SHA1Codec,
                               SHA1BytesCodec,
                               SHA256Codec,
                               SHA256BytesCodec                               
                            ]

	def onChange = { event ->
		if(application.isArtefactOfType(CodecArtefactHandler.TYPE, event.source)) {
			application.addArtefact(CodecArtefactHandler.TYPE, event.source)
		}
	}

	def doWithDynamicMethods = { applicationContext ->
        for(GrailsCodecClass c in application.codecClasses) {
            def codecClass = c
            def codecName = codecClass.name
            def encodeMethodName = "encodeAs${codecName}"
            def decodeMethodName = "decode${codecName}"

            def encoder
            def decoder
            if (Environment.current == Environment.DEVELOPMENT) {
                // Resolve codecs in every call in case of a codec reload
                encoder = {->
                    def encodeMethod = codecClass.getEncodeMethod()
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
                    def decodeMethod = codecClass.getDecodeMethod()
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