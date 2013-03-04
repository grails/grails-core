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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

/**
 * @author Jeff Brown
 * @since 0.4
 */
public class DefaultGrailsCodecClass extends AbstractInjectableGrailsClass implements GrailsCodecClass {

    public static final String CODEC = "Codec";
    private Closure<?> encodeMethod;
    private Closure<?> decodeMethod;
    
    private static EncodingStateLookup encodingStateLookup=null;
    
    public static void setEncodingStateLookup(EncodingStateLookup lookup) {
        encodingStateLookup = lookup;
    }

    public DefaultGrailsCodecClass(Class<?> clazz) {
        super(clazz, CODEC);

        encodeMethod = getMethodOrClosureMethod(clazz, "encode", true);
        decodeMethod = getMethodOrClosureMethod(clazz, "decode", false);
    }

    public Closure<?> getDecodeMethod() {
        return decodeMethod;
    }

    public Closure<?> getEncodeMethod() {
        return encodeMethod;
    }
    
    private static abstract class AbstractCallingClosure extends Closure<Object> implements Encoder {
        private static final long serialVersionUID = 1L;
        private String codecName;
        private boolean encode;
        
        public AbstractCallingClosure(Object owner, String codecName, boolean encode) {
            super(owner);
            maximumNumberOfParameters = 1;
            parameterTypes = new Class[]{Object.class};
            this.codecName = codecName;
            this.encode = encode;
        }
        
        protected abstract Object callMethod(Object argument);

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
            if (encode) {
                return encode(target);
            } else {
                return callMethod(target);
            }
        }

        public String getCodecName() {
            return codecName;
        }

        public CharSequence encode(Object target) {
            if (target instanceof Encodeable) {
                return ((Encodeable)target).encode(this);
            }
            
            String targetSrc = String.valueOf(target);
            if(targetSrc.length() == 0) {
                return "";
            }
            EncodingState encodingState=encodingStateLookup.lookup();
            if(encodingState != null) {
                Set<String> tags = encodingState.getEncodingTagsFor(targetSrc);
                if(tags != null && tags.contains(codecName)) {
                    return targetSrc;
                }
            }
            String encoded = String.valueOf(callMethod(targetSrc));
            if(encodingState != null)
                encodingState.registerEncodedWith(codecName, encoded);
            return encoded;
        }

        public void markEncoded(CharSequence string) {
            EncodingState encodingState=encodingStateLookup.lookup();
            if(encodingState != null) {
                encodingState.registerEncodedWith(codecName, string);
            }
        }
    }
    
    private static class MethodCallerClosure extends AbstractCallingClosure {
        private static final long serialVersionUID = 1L;
        Method method;
        public MethodCallerClosure(Object owner, String codecName, boolean encode, Method method) {
            super(owner, codecName, encode);
            this.method = method;
        }
       
        protected Object callMethod(Object argument) {
            return ReflectionUtils.invokeMethod(method, !Modifier.isStatic(method.getModifiers()) ? getOwner() : null, argument);
        }
    }

    private static class ClosureCallerClosure extends AbstractCallingClosure {
        private static final long serialVersionUID = 1L;
        Closure<?> closure;
        public ClosureCallerClosure(Object owner, String codecName, boolean encode, Closure<?> closure) {
            super(owner, codecName, encode);
            this.closure = closure;
        }
       
        protected Object callMethod(Object argument) {
            return closure.call(new Object[]{argument});
        }
    }
    
    private Closure<?> getMethodOrClosureMethod(Class<?> clazz, String methodName, boolean encode) {
        Closure<?> closure = (Closure<?>) getPropertyOrStaticPropertyOrFieldValue(methodName, Closure.class);
        if (closure == null) {
            Method method = ReflectionUtils.findMethod(clazz, methodName, null);
            if(method != null) {
                Object owner;
                if(Modifier.isStatic(method.getModifiers())) {
                    owner=clazz;
                } else {
                    owner=getReferenceInstance();
                }
                return new MethodCallerClosure(owner, getName(), encode, method);
            }
            return null;
        } else {
            return new ClosureCallerClosure(clazz, getName(), encode, closure);
        }
    }
}
