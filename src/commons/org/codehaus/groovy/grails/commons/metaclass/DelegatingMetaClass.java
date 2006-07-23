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

import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;

import java.beans.IntrospectionException;

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * 
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since Aug 8, 2005
 */
public class DelegatingMetaClass extends MetaClassImpl {

	DynamicMethods dynamicMethods = null;
	MetaClass adaptee = null;
	
	public DelegatingMetaClass(Class clazz, DynamicMethods dynamicMethods)
			throws IntrospectionException {
		this(clazz,dynamicMethods,true);
	}
	
	public DelegatingMetaClass(Class clazz, DynamicMethods dynamicMethods, boolean inRegistry)
			throws IntrospectionException {
		super(InvokerHelper.getInstance().getMetaRegistry(), clazz);
		this.dynamicMethods = dynamicMethods;
		if(inRegistry) {
			adaptee = registry.getMetaClass(clazz);
			registry.setMetaClass(clazz, this);
		}
	}	

	public Object invokeMethod(Object target, String methodName, Object[] arguments) {
		InvocationCallback callback = new InvocationCallback();
		Object returnValue = this.dynamicMethods.invokeMethod(target, methodName, arguments, callback);
		if (callback.isInvoked()) {
			return returnValue;
		} else {
			return adaptee.invokeMethod(target, methodName, arguments);
		}
	}
	
	public Object invokeStaticMethod(Object target, String methodName, Object[] arguments) {
		InvocationCallback callback = new InvocationCallback();
		Object returnValue = this.dynamicMethods.invokeStaticMethod(target, methodName, arguments, callback);
		if (callback.isInvoked()) {
			return returnValue;
		} else {
			return adaptee.invokeStaticMethod(target, methodName, arguments);
		}
	}

	public void setProperty(Object object, String property, Object newValue) {
		InvocationCallback callback = new InvocationCallback();
		this.dynamicMethods.setProperty(object,property,newValue,callback);
		if (!callback.isInvoked()) {
			adaptee.setProperty(object, property, newValue);
		}		
	}

	public Object getProperty(Object object, String property) {
		InvocationCallback callback = new InvocationCallback();
		Object returnValue = this.dynamicMethods.getProperty(object,property,callback);
		if (callback.isInvoked()) {
			return returnValue;
		} else {
			return adaptee.getProperty(object,property);
		}	
	}

	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#invokeConstructor(java.lang.Object[])
	 */
	public Object invokeConstructor(Object[] arg0) {
		InvocationCallback callback = new InvocationCallback();
		Object instance = this.dynamicMethods.invokeConstructor(arg0,callback);
		if(callback.isInvoked()) {
			return instance;
		}
		else {
			return adaptee.invokeConstructor(arg0);
		}
	}

	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#invokeConstructorAt(java.lang.Class, java.lang.Object[])
	 */
	public Object invokeConstructorAt(Class arg0, Object[] arg1) {
		InvocationCallback callback = new InvocationCallback();
		Object instance = this.dynamicMethods.invokeConstructor(arg1,callback);
		if(callback.isInvoked()) {
			return instance;
		}
		else {
			return adaptee.invokeConstructor(arg1);
		}
	}
	
	
}
