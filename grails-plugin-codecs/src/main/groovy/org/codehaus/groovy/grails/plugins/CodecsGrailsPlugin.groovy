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

import grails.util.Environment
import grails.util.GrailsUtil
import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.grails.plugins.codecs.Base64Codec
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import org.codehaus.groovy.grails.plugins.codecs.HexCodec
import org.codehaus.groovy.grails.plugins.codecs.JavaScriptCodec
import org.codehaus.groovy.grails.plugins.codecs.MD5BytesCodec
import org.codehaus.groovy.grails.plugins.codecs.MD5Codec
import org.codehaus.groovy.grails.plugins.codecs.SHA1BytesCodec
import org.codehaus.groovy.grails.plugins.codecs.SHA1Codec
import org.codehaus.groovy.grails.plugins.codecs.SHA256BytesCodec
import org.codehaus.groovy.grails.plugins.codecs.SHA256Codec
import org.codehaus.groovy.grails.plugins.codecs.URLCodec
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.internal.runners.statements.InvokeMethod;

/**
 * Configures pluggable codecs.
 *
 * @author Jeff Brown
 * @since 0.4
 */
class CodecsGrailsPlugin {

    static final Object[] EMPTY_ARGS = []

    def version = GrailsUtil.getGrailsVersion()
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
        if (application.isArtefactOfType(CodecArtefactHandler.TYPE, event.source)) {
            def codecClass = application.addArtefact(CodecArtefactHandler.TYPE, event.source)
            configureCodecMethods codecClass
        }
    }

    def doWithDynamicMethods = { applicationContext ->
        for (GrailsCodecClass c in application.codecClasses) {
            configureCodecMethods c
        }
    }

    @CompileStatic
    private configureCodecMethods(GrailsCodecClass codecClass) {
        String codecName = codecClass.name
        String encodeMethodName = "encodeAs${codecName}"
        String decodeMethodName = "decode${codecName}"

        Closure encoderClosure
        Closure decoderClosure
        if (Environment.current == Environment.DEVELOPMENT) {
            // Resolve codecs in every call in case of a codec reload
            encoderClosure = { ->
                def encoder = codecClass.getEncoder()
                if (encoder) {
                    return encoder.encode(delegate)
                }

                // note the call to delegate.getClass() instead of the more groovy delegate.class.
                // this is because the delegate might be a Map, in which case delegate.class doesn't
                // do what we want here...
                throw new MissingMethodException(encodeMethodName, delegate.getClass(), EMPTY_ARGS)
            }

            decoderClosure = { ->
                def decoder = codecClass.getDecoder()
                if (decoder) {
                    return decoder.decode(delegate)
                }

                // note the call to delegate.getClass() instead of the more groovy delegate.class.
                // this is because the delegate might be a Map, in which case delegate.class doesn't
                // do what we want here...
                throw new MissingMethodException(decodeMethodName, delegate.getClass(), EMPTY_ARGS)
            }
        }
        else {
            // Resolve codec methods once only at startup
            def encoder = codecClass.getEncoder()
            if (encoder) {
                encoderClosure = { -> encoder.encode(delegate) }
            } else {
                encoderClosure = { -> throw new MissingMethodException(encodeMethodName, delegate.getClass(), EMPTY_ARGS) }
            }
            def decoder = codecClass.getDecoder()
            if (decoder) {
                decoderClosure = { -> decoder.decode(delegate) }
            } else {
                decoderClosure = { -> throw new MissingMethodException(decodeMethodName, delegate.getClass(), EMPTY_ARGS) }
            }
        }

        addMetaMethod(encodeMethodName, encoderClosure)
        addMetaMethod(decodeMethodName, decoderClosure)
    }
    
    private def addMetaMethod(methodName, closure) {
        Object.metaClass."${methodName}" << closure
    }
}
