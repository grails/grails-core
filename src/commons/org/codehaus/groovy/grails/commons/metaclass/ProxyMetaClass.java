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

import groovy.lang.*;

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
public class ProxyMetaClass extends MetaClassImpl implements AdaptingMetaClass {

    protected MetaClass adaptee = null;
    protected Interceptor interceptor = null;

    public MetaClass getAdaptee()
    {
        return adaptee;
    }



    /**
	 * @param adaptee the adaptee to set
	 */
	public void setAdaptee(MetaClass adaptee) {
		this.adaptee = adaptee;
	}

	/**
     * convenience factory method for the most usual case.
     * @return A new ProxyMetaClass instance
     * @param theClass The class to create a ProxyMetaClass for
     * @throws java.beans.IntrospectionException When the class canot be introspected
     */
    public static ProxyMetaClass getInstance(Class theClass) throws IntrospectionException {
        MetaClassRegistry metaRegistry = GroovySystem.getMetaClassRegistry();
        MetaClass meta = metaRegistry.getMetaClass(theClass);
        ProxyMetaClass pmc =  new ProxyMetaClass(metaRegistry, theClass, meta);
        pmc.initialize();
        return pmc;
    }
    /**
     * @param adaptee   the MetaClass to decorate with interceptability
     * @param registry The MetaClassRegistry instance
     *
     * @param theClass The class to apply this ProxyMetaClass to
     * @throws java.beans.IntrospectionException Thrown when the class cannot be introspected
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
      * @param object The GroovyObject to use this ProxyMetaClass with
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
    public Object invokeMethod(Class aClass, final Object object, final String methodName, final Object[] arguments, boolean b, boolean b1) {
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
    public Object getProperty(Class aClass, Object object, String property, boolean b, boolean b1) {
        if (null == interceptor) {
            return super.getProperty(aClass, object, property, b, b1);
        }
        if(interceptor instanceof PropertyAccessInterceptor) {
        	PropertyAccessInterceptor pae = (PropertyAccessInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        Object result = pae.beforeGet(object,property,callback);
	        if (!callback.isInvoked()) {
	            result = super.getProperty(aClass, object, property, b, b1);
	        }
	        return result;
        }
        return super.getProperty(aClass, object, property, b, b1);
    }

    /**
	 * Interceptors the call to a property setter if a PropertyAccessInterceptor
	 * is available
	 *
	 * @param object The object to invoke the setter on
	 * @param property The property name to set
	 * @param newValue The new value of the property
	 */
    public void setProperty(Class aClass, Object object, String property, Object newValue, boolean b, boolean b1) {
        if (null == interceptor) {
            super.setProperty(aClass,object, property,newValue,b,b1);
        }
        if(interceptor instanceof PropertyAccessInterceptor) {
        	PropertyAccessInterceptor pae = (PropertyAccessInterceptor)interceptor;
        	InvocationCallback callback = new InvocationCallback();
	        pae.beforeSet(object,property,newValue,callback);
	        if (!callback.isInvoked()) {
	        	super.setProperty(aClass,object, property,newValue,b,b1);
	        }
        }
        else {
            super.setProperty(aClass,object, property, newValue,b,b1);
        }
    }
}
