/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.proxy;


import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.proxy.pojo.javassist.SerializableProxy;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.ReflectHelper;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Apr 21, 2009
 */
public class GroovyAwareJavassistLazyInitializer extends BasicLazyInitializer implements MethodHandler {
	private static final String WRITE_CLASSES_DIRECTORY = System.getProperty("javassist.writeDirectory");
	
    private static final Set GROOVY_METHODS = new HashSet() {{
        add("invokeMethod");
        add("getMetaClass");
        add("setMetaClass");
        add("metaClass");
        add("getProperty");
        add("setProperty");
        add("$getStaticMetaClass");
    }};
	private static final MethodFilter METHOD_FILTERS = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
            return m.getName().indexOf("super$")==-1 && !GROOVY_METHODS.contains(m.getName()) && !(m.getParameterTypes().length == 0 && (m.getName().equals("finalize")));
        }
	};

	private Class[] interfaces;
	private boolean constructed = false;

    private GroovyAwareJavassistLazyInitializer(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Serializable id,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final AbstractComponentType componentIdType,
	        final SessionImplementor session) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session );
		this.interfaces = interfaces;
	}

	public static HibernateProxy getProxy(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        AbstractComponentType componentIdType,
	        final Serializable id,
	        final SessionImplementor session) throws HibernateException {
		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final GroovyAwareJavassistLazyInitializer instance = new GroovyAwareJavassistLazyInitializer(
					entityName,
			        persistentClass,
			        interfaces,
			        id,
			        getIdentifierMethod,
			        setIdentifierMethod,
			        componentIdType,
			        session
			);
			ProxyFactory factory = createProxyFactory(persistentClass,
					interfaces);
			Class proxyClass = factory.createClass();
            HibernatePluginSupport.enhanceProxyClass(proxyClass);
            
			final HibernateProxy proxy = ( HibernateProxy ) proxyClass.newInstance();
			( ( ProxyObject ) proxy ).setHandler( instance );
			HibernatePluginSupport.enhanceProxy(proxy);
			instance.constructed = true;
			return proxy;
		}
		catch ( Throwable t ) {
			LoggerFactory.getLogger( BasicLazyInitializer.class ).error(
					"Javassist Enhancement failed: " + entityName, t
			);
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ entityName, t
			);
		}
	}

	public static HibernateProxy getProxy(
			final Class factory,
	        final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final AbstractComponentType componentIdType,
	        final Serializable id,
	        final SessionImplementor session) throws HibernateException {

		final GroovyAwareJavassistLazyInitializer instance = new GroovyAwareJavassistLazyInitializer(
				entityName,
		        persistentClass,
		        interfaces, id,
		        getIdentifierMethod,
		        setIdentifierMethod,
		        componentIdType,
		        session
		);

		final HibernateProxy proxy;
		try {
			proxy = ( HibernateProxy ) factory.newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), e
			);
		}
		( ( ProxyObject ) proxy ).setHandler( instance );
		instance.constructed = true;
		HibernatePluginSupport.enhanceProxy(proxy);
        HibernatePluginSupport.initializeDomain(persistentClass);
		return proxy;
	}

	public static Class getProxyFactory(
			Class persistentClass,
	        Class[] interfaces) throws HibernateException {
		// note: interfaces is assumed to already contain HibernateProxy.class

		try {
			ProxyFactory factory = createProxyFactory(persistentClass,
					interfaces);
			Class proxyClass=factory.createClass();
	        HibernatePluginSupport.enhanceProxyClass(proxyClass);
	        return proxyClass;
		}
		catch ( Throwable t ) {
			LoggerFactory.getLogger( BasicLazyInitializer.class ).error(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), t
			);
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), t
			);
		}
	}

	private static ProxyFactory createProxyFactory(Class persistentClass,
			Class[] interfaces) {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
		factory.setInterfaces( interfaces );
		factory.setFilter(METHOD_FILTERS);
		if(WRITE_CLASSES_DIRECTORY != null) {
			factory.writeDirectory = WRITE_CLASSES_DIRECTORY;
		}
		return factory;
	}


    public Object invoke(
			final Object proxy,
			final Method thisMethod,
			final Method proceed,
			final Object[] args) throws Throwable {
		if ( this.constructed ) {
			Object result;
			try {
				result = this.invoke( thisMethod, args, proxy );
			}
			catch ( Throwable t ) {
				throw new Exception( t.getCause() );
			}
			if ( result == INVOKE_IMPLEMENTATION ) {
				Object target = getImplementation();
				final Object returnValue;
				try {
					if ( ReflectHelper.isPublic( persistentClass, thisMethod ) ) {
						if ( !thisMethod.getDeclaringClass().isInstance( target ) ) {
							throw new ClassCastException( target.getClass().getName() );
						}
						returnValue = thisMethod.invoke( target, args );
					}
					else {
						if ( !thisMethod.isAccessible() ) {
							thisMethod.setAccessible( true );
						}
						returnValue = thisMethod.invoke( target, args );
					}
					return returnValue == target ? proxy : returnValue;
				}
				catch ( InvocationTargetException ite ) {
					throw ite.getTargetException();
				}
			}
			else {
				return result;
			}
		}
		else {
			// while constructor is running
			if ( thisMethod.getName().equals( "getHibernateLazyInitializer" ) ) {
				return this;
			}
			else {
				return proceed.invoke( proxy, args );
			}
		}
	}

	protected Object serializableProxy() {
		return new SerializableProxy(
				getEntityName(),
		        persistentClass,
		        interfaces,
		        getIdentifier(),
		        getIdentifierMethod,
		        setIdentifierMethod,
		        componentIdType
		);
	}
}
