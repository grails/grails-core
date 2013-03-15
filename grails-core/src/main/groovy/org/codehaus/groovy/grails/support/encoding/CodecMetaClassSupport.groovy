package org.codehaus.groovy.grails.support.encoding

import grails.util.Environment
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.runtime.GStringImpl

class CodecMetaClassSupport {
    static final Object[] EMPTY_ARGS = []
    
    @CompileStatic
    public void configureCodecMethods(GrailsCodecClass codecClass) {
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
    
    protected void addMetaMethod(String methodName, Closure closure) {
        [String, GStringImpl, StringBuffer, StringBuilder, Object].each { it.getMetaClass()."${methodName}" << closure }
    }
}
