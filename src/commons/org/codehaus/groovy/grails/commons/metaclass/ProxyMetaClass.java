/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.commons.metaclass;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaClassRegistry;

import java.beans.IntrospectionException;

/**
 * As subclass of MetaClass, ProxyMetaClass manages calls from Groovy Objects to POJOs.
 * It enriches MetaClass with the feature of making method invokations interceptable by
 * an Interceptor. To this end, it acts as a decorator (decorator pattern) allowing
 * to add or withdraw this feature at runtime.
 * 
 * This is based on original code by Dierk Koenig, but uses a callback object for thread
 * safety and supports not only method interception, but property and constructor
 * interception too.
 * 
 * @author Dierk Koenig
 * @author Graeme Rocher
 * 
 */
public class ProxyMetaClass extends MetaClassImpl {

    protected MetaClass adaptee = null;
    protected Interceptor interceptor = null;

    public MetaClass getAdaptee()
    {
        return adaptee;
    }

    /**
     * convenience factory method for the most usual case.
     */
    public static ProxyMetaClass getInstance(Class theClass) throws IntrospectionException {
        MetaClassRegistry metaRegistry = InvokerHelper.getInstance().getMetaRegistry();
        MetaClass meta = metaRegistry.getMetaClass(theClass);
        ProxyMetaClass pmc =  new ProxyMetaClass(metaRegistry, theClass, meta);
        pmc.initialise();
        return pmc;
    }
    /**
     * @param adaptee   the MetaClass to decorate with interceptability
     */
    public ProxyMetaClass(MetaClassRegistry registry, Class theClass, MetaClass adaptee) throws IntrospectionException {
        super(registry, theClass);
        this.adaptee = adaptee;
        if (null == adaptee)
        {
            throw new IllegalArgumentException("adaptee must not be null");
        }
    }

    /**
     * Use the ProxyMetaClass for the given Closure.
     * Cares for balanced register/unregister.
     * @param closure piece of code to be executed with registered ProxyMetaClass
     */
    public void use(Closure closure){
        registry.setMetaClass(theClass, this);
        
        try {
            closure.call();
        } finally {
            registry.setMetaClass(theClass, adaptee);
        }
    }

     /**
     * Use the ProxyMetaClass for the given Closure.
     * Cares for balanced setting/unsetting ProxyMetaClass.
     * @param closure piece of code to be executed with ProxyMetaClass
     */
    public void use(GroovyObject object, Closure closure){
        object.setMetaClass(this);
        
        try {
            closure.call();
        } finally {
            object.setMetaClass(adaptee);
        }
    }

    /**
     * @return the interceptor in use or null if no interceptor is used
     */
    public Interceptor getInterceptor() {
        return interceptor;
    }

    /**
     * @param interceptor may be null to reset any interception
     */
    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Call invokeMethod on adaptee with logic like in MetaClass unless we have an Interceptor.
     * With Interceptor the call is nested in its beforeInvoke and afterInvoke methods.
     * The method call is suppressed if Interceptor.doInvoke() returns false.
     * See Interceptor for details.
     */
    public Object invokeMethod(final Object object, final String methodName, final Object[] arguments) {
        return doCall(object, methodName, arguments, new Callable(){
            public Object call() {
                return adaptee.invokeMethod(object, methodName, arguments);
            }
        });
    }
    /**
     * Call invokeStaticMethod on adaptee with logic like in MetaClass unless we have an Interceptor.
     * With Interceptor the call is nested in its beforeInvoke and afterInvoke methods.
     * The method call is suppressed if Interceptor.doInvoke() returns false.
     * See Interceptor for details.
     */
    public Object invokeStaticMethod(final Object object, final String methodName, final Object[] arguments) {
        return doCall(object, methodName, arguments, new Callable(){
            public Object call() {
                return adaptee.invokeStaticMethod(object, methodName, arguments);
            }
        });
    }

    /**
     * Call invokeConstructor on adaptee with logic like in MetaClass unless we have an Interceptor.
     * With Interceptor the call is nested in its beforeInvoke and afterInvoke methods.
     * The method call is suppressed if Interceptor.doInvoke() returns false.
     * See Interceptor for details.
     */
    public Object invokeConstructor(final Object[] arguments) {
        if (null == interceptor) {
            return super.invokeConstructor(arguments);
        }
        if(interceptor instanceof ConstructorInterceptor) {
        	ConstructorInterceptor ci = (ConstructorInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        Object result = ci.beforeConstructor(arguments,callback);
	        if (!callback.isInvoked()) {
	            result = super.invokeConstructor(arguments);
	        }
	        result = ci.afterConstructor(arguments,result);
	        return result;
        }
        return super.invokeConstructor(arguments);
    }


	public Object invokeConstructorAt(final Class at, final Object[] arguments) {
        if (null == interceptor) {
            return super.invokeConstructorAt(at,arguments);
        }
        if(interceptor instanceof ConstructorInterceptor) {
        	ConstructorInterceptor ci = (ConstructorInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        Object result = ci.beforeConstructor(arguments,callback);
	        if (!callback.isInvoked()) {
	            result = super.invokeConstructorAt(at,arguments);
	        }
	        result = ci.afterConstructor(arguments,result);
	        return result;
        }
        return super.invokeConstructorAt(at,arguments);		
    }

    // since Java has no Closures...
    private interface Callable{
        Object call();
    }
    private Object doCall(Object object, String methodName, Object[] arguments, Callable howToInvoke) {
        if (null == interceptor) {
            return howToInvoke.call();
        }
        InvocationCallback callback = new InvocationCallback();
        Object result = interceptor.beforeInvoke(object, methodName, arguments,callback);
        if (!callback.isInvoked()) {
            result = howToInvoke.call();
        }
        result = interceptor.afterInvoke(object, methodName, arguments, result);
        return result;
    }

    
    
    /**
     * Interceptors the call to getProperty if a PropertyAccessInterceptor is
     * available
     * 
     * @param object the object to invoke the getter on
     * @param property the property name
     * 
     * @return the value of the property
     */
	public Object getProperty(Object object, String property) {
        if (null == interceptor) {
            return super.getProperty(object, property);
        }
        if(interceptor instanceof PropertyAccessInterceptor) {
        	PropertyAccessInterceptor pae = (PropertyAccessInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        Object result = pae.beforeGet(object,property,callback);
	        if (!callback.isInvoked()) {
	            result = super.getProperty(object, property);
	        }
	        return result;
        }
        return super.getProperty(object, property); 
	}

	/**
	 * Interceptors the call to a property setter if a PropertyAccessInterceptor
	 * is available
	 * 
	 * @param object The object to invoke the setter on
	 * @param property The property name to set
	 * @param newValue The new value of the property
	 */
	public void setProperty(Object object, String property, Object newValue) {
        if (null == interceptor) {
            super.setProperty(object, property,newValue);
        }
        if(interceptor instanceof PropertyAccessInterceptor) {
        	PropertyAccessInterceptor pae = (PropertyAccessInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        pae.beforeSet(object,property,newValue,callback);
	        if (!callback.isInvoked()) {
	        	super.setProperty(object, property,newValue);
	        }
        }
        else {
            super.setProperty(object, property, newValue);
        }
    }    
}
