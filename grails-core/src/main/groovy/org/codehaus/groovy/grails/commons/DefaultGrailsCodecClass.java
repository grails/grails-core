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

import org.springframework.util.ReflectionUtils;

/**
 * @author Jeff Brown
 * @since 0.4
 */
public class DefaultGrailsCodecClass extends AbstractInjectableGrailsClass implements GrailsCodecClass {

    public static final String CODEC = "Codec";
    private Closure<?> encodeMethod;
    private Closure<?> decodeMethod;

    public DefaultGrailsCodecClass(Class<?> clazz) {
        super(clazz, CODEC);

        encodeMethod = getMethodOrClosureMethod(clazz, "encode");
        decodeMethod = getMethodOrClosureMethod(clazz, "decode");
    }

    public Closure<?> getDecodeMethod() {
        return decodeMethod;
    }

    public Closure<?> getEncodeMethod() {
        return encodeMethod;
    }
    
    private static class MethodCallerClosure extends Closure {
        private static final long serialVersionUID = 1L;
        Method method;
        public MethodCallerClosure(Object owner, Method method) {
            super(owner);
            this.method = method;
            maximumNumberOfParameters = 1;
            parameterTypes = new Class[]{Object.class};
        }
        
        public Method getMethod() {
            return method;
        }
        
        protected Object doCall(Object arguments) {
            return ReflectionUtils.invokeMethod(method, !Modifier.isStatic(method.getModifiers()) ? getOwner() : null, (Object[])arguments);
        }

        @Override
        public Object call(Object... args) {
            return doCall(args);
        }
    }

    private Closure<?> getMethodOrClosureMethod(Class<?> clazz, String methodName) {
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
                return new MethodCallerClosure(owner, method);
            }
        }
        return closure;
    }
}
