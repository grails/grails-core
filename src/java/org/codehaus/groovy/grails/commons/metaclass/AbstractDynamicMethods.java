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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import groovy.lang.MissingMethodException;

import java.beans.IntrospectionException;
import java.util.*;



/**
 * This class provides the base implementation responsible for performing dynamic method invocation such as the dynamic
 * finders in GORM
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.1
 * 
 * Created: Aug 7, 2005
 */
public abstract class AbstractDynamicMethods implements DynamicMethods {

	protected Collection dynamicMethodInvocations = null;
	protected Collection staticMethodInvocations = null;
	protected Collection dynamicConstructors = null;
	protected Map dynamicProperties = null;
	protected Class clazz = null;
	
	private static final Log LOG = LogFactory.getLog(AbstractDynamicMethods.class);
	
	/**
	 * Creates and registers a DelegatingMetaClass instance in the registry that delegates to this class
	 * 
	 * @param theClass
	 * @throws IntrospectionException
	 */
	public AbstractDynamicMethods(Class theClass)
			throws IntrospectionException {
		this(theClass, true);		
	}
	
	/**
	 * Creates and optionally registers a DelegatingMetaClass in the MetaClasRegistry that delegates to this class
	 * @param theClass
	 * @param inRegistry
	 * @throws IntrospectionException
	 */
	public AbstractDynamicMethods(Class theClass, boolean inRegistry)
	throws IntrospectionException {
			super();
			this.clazz = theClass;
			this.dynamicMethodInvocations = new ArrayList();
			this.staticMethodInvocations = new ArrayList();
			this.dynamicProperties = new HashMap();
			this.dynamicConstructors = new ArrayList();
	}	
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.DynamicMethods#addDynamicConstructor(org.codehaus.groovy.grails.commons.metaclass.DynamicConstructor)
	 */
	public void addDynamicConstructor(DynamicConstructor constructor) {
		this.dynamicConstructors.add(constructor);
	}

	/**
	 * A non-registering constructor that simple creates an instance
	 *
	 */
	public AbstractDynamicMethods() {
		this.dynamicMethodInvocations = new ArrayList();
		this.staticMethodInvocations = new ArrayList();
		this.dynamicProperties = new HashMap();
		this.dynamicConstructors = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addDynamicMethodInvocation(org.codehaus.groovy.grails.metaclass.DynamicMethodInvocation)
	 */
	public void addDynamicMethodInvocation(DynamicMethodInvocation methodInvocation) {		
		this.dynamicMethodInvocations.add(methodInvocation);
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addStaticMethodInvocation(org.codehaus.groovy.grails.metaclass.StaticMethodInvocation)
	 */
	public void addStaticMethodInvocation(StaticMethodInvocation methodInvocation) {
		this.staticMethodInvocations.add(methodInvocation);
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addDynamicProperty(org.codehaus.groovy.grails.metaclass.DynamicProperty)
	 */
	public void addDynamicProperty(DynamicProperty property) {
		this.dynamicProperties.put(property.getPropertyName(), property);
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#getProperty(java.lang.Object, java.lang.String, org.codehaus.groovy.grails.metaclass.InvocationCallback)
	 */
	public Object getProperty(Object object, String propertyName, InvocationCallback callback) {		
		DynamicProperty getter = (DynamicProperty)this.dynamicProperties.get(propertyName);
		if (getter != null && getter.isPropertyMatch(propertyName)) {
			callback.markInvoked();
			return getter.get(object);
		}
		return null;		
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#setProperty(java.lang.Object, java.lang.String, java.lang.Object, org.codehaus.groovy.grails.metaclass.InvocationCallback)
	 */
	public void setProperty(Object object, String propertyName,Object newValue, InvocationCallback callback) {
		DynamicProperty setter = (DynamicProperty)this.dynamicProperties.get(propertyName);
		if (setter != null && setter.isPropertyMatch(propertyName)) {
			callback.markInvoked();
			setter.set(object,newValue);
		}		
	}	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object[], org.codehaus.groovy.grails.metaclass.InvocationCallback)
	 */
	public Object invokeMethod(Object object, String methodName,
		Object[] arguments, InvocationCallback callback) {
		if(LOG.isTraceEnabled()) {
			LOG.debug("[DynamicMethods] Attempting invocation of dynamic method ["+methodName+"] on target ["+object+"] with arguments ["+ArrayUtils.toString( arguments )+"]");			
		}		
		for (Iterator iter = this.dynamicMethodInvocations.iterator(); iter.hasNext();) {
			DynamicMethodInvocation methodInvocation = (DynamicMethodInvocation)iter.next();
			if (methodInvocation.isMethodMatch(methodName)) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("[DynamicMethods] Dynamic method ["+methodName+"] matched, attempting to invoke.");			
				}				
				
				try {
					Object result = methodInvocation.invoke(object, methodName,arguments);
					if(LOG.isDebugEnabled()) {
						LOG.debug("[DynamicMethods] Instance method ["+methodName+"] invoked successfully with result ["+result+"]. Marking as invoked");			
					}	
					callback.setInvoker(methodInvocation);
					callback.markInvoked();
					return result;
				} catch (MissingMethodException e) {
					if(LOG.isDebugEnabled()) {					
						LOG.debug("[DynamicMethods] Instance method ["+methodName+"] threw MissingMethodException. Returning null and falling back to standard MetaClass",e);			
					}
					return null;
				}
			}
		}
		return null;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.DynamicMethods#invokeConstructor(java.lang.Object[], org.codehaus.groovy.grails.commons.metaclass.InvocationCallback)
	 */
	public Object invokeConstructor(Object[] arguments, InvocationCallback callBack) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("[DynamicMethods] Attempting invocation of dynamic constructor with arguments ["+ArrayUtils.toString( arguments )+"]");			
		}
		
		for (Iterator i = this.dynamicConstructors.iterator(); i.hasNext();) {
			DynamicConstructor constructor = (DynamicConstructor) i.next();
			if(constructor.isArgumentsMatch(arguments)) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("[DynamicMethods] Dynamic constructor found, marked and invoking...");			
				}				
				callBack.markInvoked();
				return constructor.invoke(this.clazz,arguments);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#invokeStaticMethod(java.lang.Object, java.lang.String, java.lang.Object[], org.codehaus.groovy.grails.metaclass.InvocationCallback)
	 */
	public Object invokeStaticMethod(Object object, String methodName,
			Object[] arguments, InvocationCallback callBack) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("[DynamicMethods] Attempting invocation of dynamic static method ["+methodName+"] on target ["+object+"] with arguments ["+ArrayUtils.toString( arguments )+"]");
			//LOG.debug("[DynamicMethods] Registered dynamic static methods: ["+this.staticMethodInvocations+"]");
		}
		for (Iterator iter = this.staticMethodInvocations.iterator(); iter.hasNext();) {
			StaticMethodInvocation methodInvocation = (StaticMethodInvocation)iter.next();
			if (methodInvocation.isMethodMatch(methodName)) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("[DynamicMethods] Static method matched, attempting to invoke");			
				}				
				
				try {
					Object result =  methodInvocation.invoke(this.clazz, methodName, arguments);
					
					if(LOG.isDebugEnabled()) {
						LOG.debug("[DynamicMethods] Static method ["+methodName+"] invoked successfully with result ["+result+"]. Marking as invoked");			
					}						
					callBack.markInvoked();
					return result;
				} catch (MissingMethodException e) {
					if(LOG.isDebugEnabled()) {					
						LOG.debug("[DynamicMethods] Static method ["+methodName+"] threw MissingMethodException. Returning null and falling back to standard MetaClass",e);			
					}						
					return null;
				}
			}
		}
		return null;
	}

	public DynamicProperty getDynamicProperty(String propertyName) {
		return (DynamicProperty)this.dynamicProperties.get(propertyName);
	}
	
	public DynamicMethodInvocation getDynamicMethod(String method_signature) {
		for (Iterator iter = this.dynamicMethodInvocations.iterator(); iter.hasNext();) {
			DynamicMethodInvocation methodInvocation = (DynamicMethodInvocation)iter.next();
			if (methodInvocation.isMethodMatch(method_signature)) {

				return methodInvocation;
			}
		}		
		return null;
	}	
}
