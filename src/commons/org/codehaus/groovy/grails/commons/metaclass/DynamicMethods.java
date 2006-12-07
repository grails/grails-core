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
/**
 * An interface that defines methods for a handling dynamic method, static method and property 
 * invocations
 * 
 * @author Graeme Rocher
 * @since 0.1
 * 
 * Created: Oct 27, 2005
 */
public interface DynamicMethods {
	
	/**
	 * Adds a dynamic constructor
	 * 
	 * @param constructor The constructor to add
	 */
	public void addDynamicConstructor(DynamicConstructor constructor);

	/**
	 * Adds a new dynamic method invocation
	 * @param methodInvocation
	 */
	public abstract void addDynamicMethodInvocation(
			DynamicMethodInvocation methodInvocation);

	/**
	 * Adds a new static method invocation
	 * @param methodInvocation
	 */
	public abstract void addStaticMethodInvocation(
			StaticMethodInvocation methodInvocation);

	/**
	 * Adds a new dynamic property
	 * @param property
	 */
	public abstract void addDynamicProperty(DynamicProperty property);

	/**
	 * Retrieves a dynamic property for the specified property name
	 * @param propertyName The name of the property
	 * @return A DynamicProperty instance of null if none exists
	 */
	public abstract DynamicProperty getDynamicProperty(String propertyName);
	
	/**
	 * Retrieves a dynamic method for the specified method name
	 * @param method_signature Then signature of the method
	 * @return The method instance or null if non exists
	 */
	public DynamicMethodInvocation getDynamicMethod(String method_signature);
	
	/**
	 * Attempts to get a dynamic property. If successful the InvocationCallback
	 * instance is marked as invoked
	 *  
	 * @param object The instance
	 * @param propertyName The property name to get
	 * @param callback The callback object
	 * 
	 * @return The property value if it exists
	 */
	public abstract Object getProperty(Object object, String propertyName,
			InvocationCallback callback);


	/**
	 * Attempts to set a dynamic property. If successful the InvocationCallback
	 * instance is marked as invoked
	 *  
	 * @param object The instance
	 * @param propertyName The property name to set
	 * @param callback The callback object
	 */
	public abstract void setProperty(Object object, String propertyName,
			Object newValue, InvocationCallback callback);

	/**
	 * Attempts to invoke a dynamic method with the specified name and arguments
	 * If successful the callback object is marked as invoked.
	 * 
	 * @param object The instance to invoke on
	 * @param methodName The name of the method
	 * @param arguments The arguments of the method
	 * @param callback The callback object
	 * 
	 * @return The method return value
	 */
	public abstract Object invokeMethod(Object object, String methodName,
			Object[] arguments, InvocationCallback callback);

	/**
	 * Attempts to invoke a dynamic static method with the specified name and arguments
	 * If successful the callback object is marked as invoked.
	 * 
	 * @param object The instance to invoke on
	 * @param methodName The name of the method
	 * @param arguments The arguments of the method
	 * @param callBack The callback object
	 * 
	 * @return The method return value
	 */	
	public abstract Object invokeStaticMethod(Object object, String methodName,
			Object[] arguments, InvocationCallback callBack);

	/**
	 * Attempts to invoke a dynamic constructor. If successful the callback object
	 * is marked as invoked.
	 * 
	 * @param arguments The arguments
	 * @param callBack The callback object
	 * 
	 * @return The constructed instance
	 */
	public Object invokeConstructor(Object[] arguments, InvocationCallback callBack);
}
