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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.Closure;

import java.lang.reflect.Modifier;

import org.codehaus.groovy.runtime.NewInstanceMetaMethod;

/**
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
public class ClosureMetaMethod extends NewInstanceMetaMethod {

	private Closure callable;
	private Class[] paramTypes;
	private Class declaringClass;
	
	public ClosureMetaMethod(String name, Closure c) {		
		this(name, c.getOwner().getClass(), c);
	}
	
	public ClosureMetaMethod(String name, Class declaringClass,Closure c) {
		super(name, declaringClass, c.getParameterTypes(), Object.class,Modifier.PUBLIC);
		paramTypes = c.getParameterTypes();
		if(paramTypes == null) {
			paramTypes = new Class[0];
		}
		this.callable = c;
		this.declaringClass = declaringClass;
	}	
	

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.runtime.NewInstanceMetaMethod#getDeclaringClass()
	 */
	public Class getDeclaringClass() {
		return declaringClass;
	}

	/* (non-Javadoc)
	 * @see groovy.lang.MetaMethod#invoke(java.lang.Object, java.lang.Object[])
	 */
	public Object invoke(final Object object, final Object[] arguments) {
		Closure cloned = (Closure) callable.clone();
		cloned.setDelegate(object);
		
		return cloned.call(arguments);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.runtime.NewInstanceMetaMethod#getParameterTypes()
	 */
	public Class[] getParameterTypes() {
		return paramTypes;
	}
}
