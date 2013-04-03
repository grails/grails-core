package org.codehaus.groovy.grails.support.encoding

import grails.util.Environment
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsCodecClass
import org.codehaus.groovy.runtime.GStringImpl

class CodecMetaClassSupport {
    static final Object[] EMPTY_ARGS = []
    
    static final String ENCODE_AS_PREFIX="encodeAs"
    static final String DECODE_PREFIX="decode"
    
    @CompileStatic
    public void configureCodecMethods(GrailsCodecClass codecClass) {
        if(codecClass==null) {
            throw new NullPointerException("Jee")
        }
        
        
        //String codecName = codecClass.name
        Closure<String> encodeMethodNameClosure = { String codecName -> "${ENCODE_AS_PREFIX}${codecName}".toString() }
        Closure<String> decodeMethodNameClosure = { String codecName -> "${DECODE_PREFIX}${codecName}".toString() }
        
        String encodeMethodName = encodeMethodNameClosure(codecClass.name)
        String decodeMethodName = decodeMethodNameClosure(codecClass.name)

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
        if(codecClass.encoder) {
            addAliasMetaMethods(codecClass.encoder.codecIdentifier.codecAliases, encodeMethodNameClosure, encoderClosure)
        }

        addMetaMethod(decodeMethodName, decoderClosure)
        if(codecClass.decoder) {
            addAliasMetaMethods(codecClass.decoder.codecIdentifier.codecAliases, decodeMethodNameClosure, decoderClosure)
        }
    }

    @CompileStatic
    private addAliasMetaMethods(Set<String> aliases, Closure<String> methodNameClosure, Closure methodClosure) {
        aliases?.each { String aliasName ->
            addMetaMethod(methodNameClosure(aliasName), methodClosure)
        }
    }
    
    protected void addMetaMethod(String methodName, Closure closure) {
        [String, GStringImpl, StringBuffer, StringBuilder, Object].each { it.getMetaClass()."${methodName}" << closure }
    }
}
