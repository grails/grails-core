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
package org.codehaus.groovy.grails.commons;

import groovy.lang.Closure;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.codehaus.groovy.grails.support.encoding.CodecFactory;
import org.codehaus.groovy.grails.support.encoding.Decoder;
import org.codehaus.groovy.grails.support.encoding.Encodeable;
import org.codehaus.groovy.grails.support.encoding.EncodedAppender;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingState;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistryLookup;
import org.codehaus.groovy.grails.support.encoding.StreamingEncoder;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jeff Brown
 * @since 0.4
 */
public class DefaultGrailsCodecClass extends AbstractInjectableGrailsClass implements GrailsCodecClass {
    public static final String CODEC = CodecArtefactHandler.TYPE;
    private static EncodingStateRegistryLookup encodingStateRegistryLookup=null;
    private Encoder encoder;
    private Decoder decoder;
    
    public static void setEncodingStateRegistryLookup(EncodingStateRegistryLookup lookup) {
        encodingStateRegistryLookup = lookup;
    }
    
    public static EncodingStateRegistryLookup getEncodingStateRegistryLookup() {
        return encodingStateRegistryLookup;
    }

    public DefaultGrailsCodecClass(Class<?> clazz) {
        super(clazz, CODEC);
        initializeCodec();
    }
    
    private void initializeCodec() {
        if(Encoder.class.isAssignableFrom(getClazz())) {
            encoder = (Encoder)getReferenceInstance();
        }
        if(Decoder.class.isAssignableFrom(getClazz())) {
            decoder = (Decoder)getReferenceInstance();
        }
        if(encoder==null && decoder==null) {
            CodecFactory codecFactory=null;
            if(CodecFactory.class.isAssignableFrom(getClazz())) {
                codecFactory=(CodecFactory)getReferenceInstance();
            }
            if(codecFactory==null) {
                codecFactory=(CodecFactory)getPropertyOrStaticPropertyOrFieldValue("codecFactory", CodecFactory.class);
            }
            if(codecFactory==null) {
                codecFactory=new ClosureCodecFactory();
            }
            encoder=codecFactory.getEncoder();
            decoder=codecFactory.getDecoder();
        }
        if(encoder != null) {
            if(encoder instanceof StreamingEncoder) {
                encoder=new StreamingStateAwareEncoderWrapper((StreamingEncoder)encoder);
            } else {
                encoder=new StateAwareEncoderWrapper(encoder);
            }
        }
    }

    private class ClosureCodecFactory implements CodecFactory {
        private Encoder encoder;
        private Decoder decoder;
        
        ClosureCodecFactory() {
            Closure<Object> encoderClosure = getMethodOrClosureMethod(getClazz(), "encode");
            if(encoderClosure != null) {
                encoder=new ClosureEncoder(getName(), encoderClosure);
            }
            Closure<Object> decoderClosure = getMethodOrClosureMethod(getClazz(), "decode");
            if(decoderClosure != null) {
                decoder=new ClosureDecoder(getName(), decoderClosure);
            }
        }

        public Encoder getEncoder() {
            return encoder;
        }

        public Decoder getDecoder() {
            return decoder;
        }
        
        private Closure<Object> getMethodOrClosureMethod(Class<?> clazz, String methodName) {
            @SuppressWarnings("unchecked")
            Closure<Object> closure = (Closure<Object>) getPropertyOrStaticPropertyOrFieldValue(methodName, Closure.class);
            if (closure == null) {
                Method method = ReflectionUtils.findMethod(clazz, methodName, (Class<?>[])null);
                if(method != null) {
                    Object owner;
                    if(Modifier.isStatic(method.getModifiers())) {
                        owner=clazz;
                    } else {
                        owner=getReferenceInstance();
                    }
                    return new MethodCallingClosure(owner, method);
                }
                return null;
            } else {
                return closure;
            }
        }        
    }
    
    private static class ClosureDecoder implements Decoder {
        private String codecName;
        private Closure<Object> closure;
        
        public ClosureDecoder(String codecName, Closure<Object> closure) {
            this.codecName=codecName;
            this.closure=closure;
        }
        
        public String getCodecName() {
            return codecName;
        }

        public Object decode(Object o) {
            return closure.call(o);
        }        
    }
    
    private static class StateAwareEncoderWrapper implements Encoder {
        private Encoder delegate;
        
        public StateAwareEncoderWrapper(Encoder delegate) {
            this.delegate=delegate;
        }
        
        public String getCodecName() {
            return delegate.getCodecName();
        }

        public Object encode(Object target) {
            if (target instanceof Encodeable) {
                return ((Encodeable)target).encode(this);
            }

            EncodingStateRegistry encodingState=lookupEncodingState();
            if(encodingState != null && target instanceof CharSequence) {
                if(!encodingState.shouldEncodeWith(this, (CharSequence)target)) {
                    return target;
                }
            }
            Object encoded = delegate.encode(target);
            if(encodingState != null && encoded instanceof CharSequence)
                encodingState.registerEncodedWith(this, (CharSequence)encoded);
            return encoded;
        }

        protected EncodingStateRegistry lookupEncodingState() {
            return encodingStateRegistryLookup != null ? encodingStateRegistryLookup.lookup() : null;
        }

        public void markEncoded(CharSequence string) {
            EncodingStateRegistry encodingState=lookupEncodingState();
            if(encodingState != null) {
                encodingState.registerEncodedWith(this, string);
            }
        }

        public Set<String> getEquivalentCodecNames() {
            return delegate.getEquivalentCodecNames();
        }

        public boolean isPreventAllOthers() {
            return delegate.isPreventAllOthers();
        }        
    }    
    
    private static class StreamingStateAwareEncoderWrapper extends StateAwareEncoderWrapper implements StreamingEncoder {
        private StreamingEncoder delegate;
        public StreamingStateAwareEncoderWrapper(StreamingEncoder delegate) {
            super(delegate);
            this.delegate=delegate;
        }
        public void encodeToStream(CharSequence source, int offset, int len, EncodedAppender appender,
                EncodingState encodingState) throws IOException {
            delegate.encodeToStream(source, offset, len, appender, encodingState);
        }
    }
    
    private static class ClosureEncoder implements Encoder {
        private String codecName;
        private Closure<Object> closure;
        
        public ClosureEncoder(String codecName, Closure<Object> closure) {
            this.codecName=codecName;
            this.closure=closure;
        }
        
        public String getCodecName() {
            return codecName;
        }

        public CharSequence encode(Object target) {
            if(target==null) return null;
            Object encoded = closure.call(target);
            if(encoded != null && !(encoded instanceof CharSequence)) {
                return String.valueOf(encoded);
            }
            return (CharSequence)encoded;
        }

        public void markEncoded(CharSequence string) {
            
        }

        public Set<String> getEquivalentCodecNames() {
            return null;
        }

        public boolean isPreventAllOthers() {
            return false;
        }        
    }
    
    private static class MethodCallingClosure extends Closure<Object> {
        private static final long serialVersionUID = 1L;
        private Method method;
        
        public MethodCallingClosure(Object owner, Method method) {
            super(owner);
            maximumNumberOfParameters = 1;
            parameterTypes = new Class[]{Object.class};
            this.method=method;
        }
        
        protected Object callMethod(Object argument) {
            return ReflectionUtils.invokeMethod(method, !Modifier.isStatic(method.getModifiers()) ? getOwner() : null, argument);
        }

        @Override
        public Object call(Object... args) {
            return doCall(args);
        }

        protected Object doCall(Object[] args) {
            Object target=null;
            if(args != null && args.length > 0)
                target=args[0];
            if(target==null) {
                return null;
            }
            return callMethod(target);
        }
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }
}
