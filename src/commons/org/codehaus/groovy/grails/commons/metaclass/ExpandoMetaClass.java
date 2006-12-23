/*
 * Copyright 2004-2006 Graeme Rocher
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
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.DefaultMethodKey;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.MethodKey;

/**
 * A MetaClass that implements GroovyObject and behaves like an Expando, allowing the addition of new methods on the fly
 * 
 * <code><pre>
 * // defines or replaces instance method: 
 * metaClass.myMethod = { args -> }
 * 
 * // defines a new instance method
 * metaClass.myMethod << { args -> }
 * 
 * // creates multiple overloaded methods of the same name
 * metaClass.myMethod << { String s -> } << { Integer i -> } 
 * 
 * // defines or replaces a static method with the 'static' qualifier 
 * metaClass.'static'.myMethod = { args ->  }
 * 
 * // defines a new static method with the 'static' qualifier 
 * metaClass.'static'.myMethod << { args ->  }
 * 
 * // defines a new contructor
 * metaClass.ctor << { String arg -> }
 * 
 * // defines or replaces a constructor
 * metaClass.ctor = { String arg -> }
 * 
 * // defines a new property with an initial value of "blah"
 * metaClass.myProperty = "blah"
 * 
 * </code></pre>
 * 
 * By default methods are only allowed to be added before initialize() is called. In other words you create a new
 * ExpandoMetaClass, add some methods and then call initialize(). If you attempt to add new methods after initialize()
 * has been called an error will be thrown.
 * 
 * This is to ensure that the MetaClass can operate appropriately in multi threaded environments as it forces you
 * to do all method additions at the beginning, before using the MetaClass.
 * 
 * If you need more fine grained control of how a method is matched you can use DynamicMethodsMetaClass
 * 
 * WARNING: This MetaClass uses a thread-bound ThreadLocal instance to store and retrieve properties. 
 * In addition properties stored use soft references so they are both bound by the life of the Thread and by the soft
 * references. The implication here is you should NEVER use dynamic properties if you want their values to stick around
 * for long periods because as soon as the JVM is running low on memory or the thread dies they will be garbage collected.
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
public class ExpandoMetaClass extends MetaClassImpl implements GroovyObject {

	private static final String META_CLASS = "metaClass";
	private static final String CLASS = "class";
	private static final String META_METHODS = "metaMethods";
	private static final String METHODS = "methods";
	private static final String PROPERTIES = "properties";
	private static final String STATIC = "static";
	private static final Class[] ZERO_ARGUMENTS = new Class[0];
	private static final String CONSTRUCTOR = "ctor";
	private static final String GROOVY_CONSTRUCTOR = "<init>";
	
	private MetaClass myMetaClass;

	/**
	 * Constructs a new ExpandoMetaClass instance for the given class
	 * 
	 * @param theClass The class that the MetaClass applies to
	 */
	public ExpandoMetaClass(Class theClass) {
		super(InvokerHelper.getInstance().getMetaRegistry(), theClass);
		this.myMetaClass = InvokerHelper.getMetaClass(this);
		
	}
	
	/**
	 * Constructs a new ExpandoMetaClass instance for the given class optionally placing the MetaClass
	 * in the MetaClassRegistry automatically
	 * 
	 * @param theClass The class that the MetaClass applies to
	 * @param register True if the MetaClass should be registered inside the MetaClassRegistry
	 */
	public ExpandoMetaClass(Class theClass, boolean register) {
		this(theClass);
		if(register)
			super.registry.setMetaClass(theClass, this);
	}
	
	/**
	 * Instances of this class are returned when using the << left shift operator. 
	 * 
	 * Example:
	 * 
	 * metaClass.myMethod << { String args -> }
	 * 
	 * This allows callbacks to the ExpandoMetaClass for registering appending methods
	 * 
	 * @author Graeme Rocher
	 *
	 */
	private class ExpandoMetaProperty extends GroovyObjectSupport {
		
		String propertyName;
		boolean isStatic = false;
		private ExpandoMetaProperty(String name) {
			this(name, false);
		}
		private ExpandoMetaProperty(String name, boolean isStatic) {
			this.propertyName = name;
			this.isStatic = isStatic;
		}
		
		public Object leftShift(Object arg) {
			registerIfClosure(arg, false);
			return this;
		}
		private void registerIfClosure(Object arg, boolean replace) {
			if(arg instanceof Closure) {
				Closure callable = (Closure)arg;
				Class[] paramTypes = callable.getParameterTypes();
				if(paramTypes == null)paramTypes = ZERO_ARGUMENTS;
				if(!this.isStatic) {
					Method foundMethod = checkIfMethodExists(theClass, propertyName, paramTypes, false);
					
					if(foundMethod != null && !replace) throw new GroovyRuntimeException("Cannot add new method ["+propertyName+"] for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");
					
					registerInstanceMethod(propertyName, callable);
				}
				else {
					Method foundMethod = checkIfMethodExists(theClass, propertyName, paramTypes, true);
					if(foundMethod != null && !replace) throw new GroovyRuntimeException("Cannot add new static method ["+propertyName+"] for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");
					
					registerStaticMethod(propertyName, callable);
				}				
			}
		}
		private Method checkIfMethodExists(Class methodClass, String methodName, Class[] paramTypes, boolean staticMethod) {
			Method foundMethod = null;
			Method[] methods = methodClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if(methods[i].getName().equals(methodName) && Modifier.isStatic(methods[i].getModifiers()) == staticMethod) {
					if(MetaClassHelper.parametersAreCompatible( paramTypes, methods[i].getParameterTypes() )) {
						foundMethod = methods[i];
						break;
					}
				}
			}
			return foundMethod;
		}
		/* (non-Javadoc)
		 * @see groovy.lang.GroovyObjectSupport#getProperty(java.lang.String)
		 */
		public Object getProperty(String property) {
			this.propertyName = property;
			return this;
		}
		/* (non-Javadoc)
		 * @see groovy.lang.GroovyObjectSupport#setProperty(java.lang.String, java.lang.Object)
		 */
		public void setProperty(String property, Object newValue) {
			this.propertyName = property;
			registerIfClosure(newValue, true);
		}	
		
		
	}
	
	
	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#invokeConstructor(java.lang.Object[])
	 */
	public Object invokeConstructor(Object[] arguments) {
		
		// TODO This is the only area where this MetaClass needs to do some interception because Groovy's current
		// MetaClass uses hard coded references to the java.lang.reflect.Constructor class so you can't simply
		// inject Constructor like you can do properties, methods and fields. When Groovy's MetaClassImpl is
		// refactored we can fix this
		Class[] argClasses = MetaClassHelper.convertToTypeArray(arguments);
		MetaMethod method = retrieveMethod(GROOVY_CONSTRUCTOR, argClasses);
		if(method!=null && method.getParameterTypes().length == arguments.length) {
			return method.invoke(theClass, arguments);
		}
		return super.invokeConstructor(arguments);
	}

	/**
	 * Handles the ability to use the left shift operator to append new constructors
	 * 
	 * @author Graeme Rocher
	 *
	 */
	private class ExpandoMetaConstructor extends GroovyObjectSupport {
		

		public Object leftShift(Closure c) {
			if(c != null) {
				Class[] paramTypes = c.getParameterTypes();
				if(paramTypes == null)paramTypes = ZERO_ARGUMENTS;
				
				Constructor ctor = retrieveConstructor(paramTypes);
				if(ctor != null) throw new GroovyRuntimeException("Cannot add new constructor for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");
				
				registerInstanceMethod(GROOVY_CONSTRUCTOR, c);
			}
			
			return this;
		}
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#getMetaClass()
	 */
	public MetaClass getMetaClass() {
		return myMetaClass;
	}



	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) {
		if(isValidExpandoProperty(property)) {
			if(property.equals(STATIC)) {
				return new ExpandoMetaProperty(property, true);
			}
			else if(property.equals(CONSTRUCTOR)) {
				return new ExpandoMetaConstructor();
			}
			else {
				return new ExpandoMetaProperty(property);	
			}			
		}
		else {
			return myMetaClass.getProperty(this, property);
		}				
	}

	private boolean isValidExpandoProperty(String property) {
		if(!property.equals(META_CLASS) &&
		   !property.equals(CLASS) &&
		   !property.equals(META_METHODS) &&
		   !property.equals(METHODS) &&
		   !property.equals(PROPERTIES))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
	 */
	public Object invokeMethod(String name, Object args) {
		return myMetaClass.invokeMethod(this, name, args);
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
	 */
	public void setMetaClass(MetaClass metaClass) {
		this.myMetaClass = metaClass;
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String property, Object newValue) {
		if(newValue instanceof Closure) {
			if(property.equals(CONSTRUCTOR)) {
				property = GROOVY_CONSTRUCTOR;
			}
			Closure callable = (Closure)newValue;
			// here we don't care if the method exists or not we assume the 
			// developer is responsible and wants to override methods where necessary
			registerInstanceMethod(property, callable);
			
		}
		else {
			Class type = newValue == null ? Object.class : newValue.getClass();
			
			MetaBeanProperty mbp = new ThreadManagedMetaBeanProperty(theClass,property,type,newValue);
			
			addMetaMethod(mbp.getGetter());
			addMetaMethod(mbp.getSetter());
			
			addMetaBeanProperty(mbp);
			
			
		}			
	}

	/**
	 * Registers a new instance method for the given method name and closure on this MetaClass
	 * 
	 * @param methodName The method name
	 * @param callable The callable Closure
	 */
	protected void registerInstanceMethod(String methodName, Closure callable) {
		ClosureMetaMethod metaMethod = new ClosureMetaMethod(methodName, super.theClass,callable);
		MethodKey key = new DefaultMethodKey(super.theClass,methodName, metaMethod.getParameterTypes() );
		
		
		super.addMetaMethod(metaMethod);
		super.cacheInstanceMethod(key, metaMethod);
	}
	
	/**
	 * Registers a new static method for the given method name and closure on this MetaClass
	 * 
	 * @param methodName The method name
	 * @param callable The callable Closure
	 */
	protected void registerStaticMethod(String methodName, Closure callable) {
		ClosureStaticMetaMethod metaMethod = new ClosureStaticMetaMethod(methodName, super.theClass,callable);
		MethodKey key = new DefaultMethodKey(super.theClass,methodName, metaMethod.getParameterTypes() );
		
		super.addMetaMethod(metaMethod);
		super.cacheStaticMethod(key, metaMethod);
	}

}

