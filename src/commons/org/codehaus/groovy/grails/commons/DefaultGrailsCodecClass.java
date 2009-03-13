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
import groovy.lang.MetaMethod;
import org.codehaus.groovy.runtime.MethodClosure;

/**
 * 
 * 
 * @author Jeff Brown
 * @since 0.4
 */
public class DefaultGrailsCodecClass extends AbstractInjectableGrailsClass 
	implements GrailsCodecClass {
	
	public static final String CODEC = "Codec";
    private Closure encodeMethod;
    private Closure decodeMethod;

    public DefaultGrailsCodecClass(Class clazz) {
		super(clazz, CODEC);

        this.encodeMethod = getMethodOrClosureMethod("encode");
        this.decodeMethod = getMethodOrClosureMethod("decode");
	}

	public Closure getDecodeMethod() {
        return this.decodeMethod;
	}

    public Closure getEncodeMethod() {
		return this.encodeMethod;
	}

    private Closure getMethodOrClosureMethod(String methodName) {
        Closure closure = (Closure) getPropertyOrStaticPropertyOrFieldValue(methodName, Closure.class);
        if(closure == null) {
            MetaMethod method = getMetaClass().getMetaMethod(methodName, new Object[]{Object.class});
            if(method!=null) {
                closure = new MethodClosure(getReference().getWrappedInstance(), methodName);
            }
        }
        return closure;
    }


}