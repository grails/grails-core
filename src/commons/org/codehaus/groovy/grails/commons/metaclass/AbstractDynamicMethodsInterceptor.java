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

import java.beans.IntrospectionException;

/**
 * Implements an the Interceptor interface to add support for using ProxyMetaClass to define 
 * dynamic methods
 * 
 * @author Graeme Rocher
 * @since Oct 24, 2005
 */
public abstract class AbstractDynamicMethodsInterceptor extends AbstractDynamicMethods
		implements Interceptor,PropertyAccessInterceptor,ConstructorInterceptor {

	public AbstractDynamicMethodsInterceptor() {
		super();
	}

	public AbstractDynamicMethodsInterceptor(Class theClass, boolean inRegistry) throws IntrospectionException {
		super(theClass, inRegistry);
	}

	public AbstractDynamicMethodsInterceptor(Class theClass) throws IntrospectionException {
		super(theClass);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.ConstructorInterceptor#afterConstructor(java.lang.Object[], java.lang.Object)
	 */
	public Object afterConstructor(Object[] args, Object instantiatedInstance) {
		return instantiatedInstance;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.ConstructorInterceptor#beforeConstructor(java.lang.Object[], org.codehaus.groovy.grails.commons.metaclass.InvocationCallback)
	 */
	public Object beforeConstructor(Object[] args, InvocationCallback callback) {
		Object result = invokeConstructor(args,callback);
		return callback.isInvoked() ? result : null;
	}

	public Object beforeInvoke(Object target, String methodName,
			Object[] arguments, InvocationCallback callback) {
		Object returnValue = invokeMethod(target, methodName, arguments, callback);
		// if the method was invoked as dynamic 
		// don't invoke true target
		return callback.isInvoked() ? returnValue : null;
	}

	public Object afterInvoke(Object object, String methodName,
			Object[] arguments, Object result) {
		return result;
	}

	public Object beforeGet(Object object, String property, InvocationCallback callback) {
		Object returnValue = getProperty(object,property,callback);
		// if the method was invoked as dynamic 
		// don't invoke true target
		return callback.isInvoked() ? returnValue : null;
	}

	public void beforeSet(Object object, String property, Object newValue, InvocationCallback callback) {
		setProperty(object,property,newValue,callback);
	}
}
