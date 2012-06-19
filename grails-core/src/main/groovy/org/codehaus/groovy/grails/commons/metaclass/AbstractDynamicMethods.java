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

import groovy.lang.MissingMethodException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides the base implementation responsible for performing dynamic method invocation
 * such as the dynamic finders in GORM.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @since 0.1
 */
public abstract class AbstractDynamicMethods implements DynamicMethods {

    protected Collection<DynamicMethodInvocation> dynamicMethodInvocations = new ArrayList<DynamicMethodInvocation>();
    protected Collection<StaticMethodInvocation> staticMethodInvocations = new ArrayList<StaticMethodInvocation>();
    protected Collection<DynamicConstructor> dynamicConstructors = new ArrayList<DynamicConstructor>();
    protected Map<String, DynamicProperty> dynamicProperties = new HashMap<String, DynamicProperty>();
    protected Class<?> clazz;

    private static final Log LOG = LogFactory.getLog(AbstractDynamicMethods.class);

    /**
     * Creates and registers a DelegatingMetaClass instance in the registry that delegates to this class.
     *
     * @param theClass
     */
    public AbstractDynamicMethods(Class<?> theClass) {
        this(theClass, true);
    }

    /**
     * Creates and optionally registers a DelegatingMetaClass in the MetaClasRegistry that
     * delegates to this class.
     * @param theClass
     * @param inRegistry
     */
    public AbstractDynamicMethods(Class<?> theClass, @SuppressWarnings("unused") boolean inRegistry) {
        clazz = theClass;
    }

    /**
     * A non-registering constructor that simple creates an instance
     */
    public AbstractDynamicMethods() {
        // default
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.metaclass.DynamicMethods#addDynamicConstructor(org.codehaus.groovy.grails.commons.metaclass.DynamicConstructor)
     */
    public void addDynamicConstructor(DynamicConstructor constructor) {
        dynamicConstructors.add(constructor);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addDynamicMethodInvocation(org.codehaus.groovy.grails.metaclass.DynamicMethodInvocation)
     */
    public void addDynamicMethodInvocation(DynamicMethodInvocation methodInvocation) {
        dynamicMethodInvocations.add(methodInvocation);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addStaticMethodInvocation(org.codehaus.groovy.grails.metaclass.StaticMethodInvocation)
     */
    public void addStaticMethodInvocation(StaticMethodInvocation methodInvocation) {
        staticMethodInvocations.add(methodInvocation);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#addDynamicProperty(org.codehaus.groovy.grails.metaclass.DynamicProperty)
     */
    public void addDynamicProperty(DynamicProperty property) {
        dynamicProperties.put(property.getPropertyName(), property);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#getProperty(java.lang.Object, java.lang.String, org.codehaus.groovy.grails.metaclass.InvocationCallback)
     */
    public Object getProperty(Object object, String propertyName, InvocationCallback callback) {
        DynamicProperty getter = dynamicProperties.get(propertyName);
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
        DynamicProperty setter = dynamicProperties.get(propertyName);
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
        if (LOG.isTraceEnabled()) {
            LOG.debug("[DynamicMethods] Attempting invocation of dynamic method [" + methodName +
                    "] on target [" + object + "] with arguments [" + ArrayUtils.toString(arguments) + "]");
        }
        for (DynamicMethodInvocation methodInvocation : dynamicMethodInvocations) {
            if (methodInvocation.isMethodMatch(methodName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[DynamicMethods] Dynamic method [" + methodName + "] matched, attempting to invoke.");
                }

                try {
                    Object result = methodInvocation.invoke(object, methodName, arguments);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[DynamicMethods] Instance method [" + methodName +
                                "] invoked successfully with result [" + result + "]. Marking as invoked");
                    }
                    callback.setInvoker(methodInvocation);
                    callback.markInvoked();
                    return result;
                }
                catch (MissingMethodException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[DynamicMethods] Instance method [" + methodName +
                                "] threw MissingMethodException. Returning null and falling back to standard MetaClass",e);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("[DynamicMethods] Attempting invocation of dynamic constructor with arguments [" +
                    ArrayUtils.toString(arguments) + "]");
        }

        for (DynamicConstructor constructor : dynamicConstructors) {
            if (constructor.isArgumentsMatch(arguments)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[DynamicMethods] Dynamic constructor found, marked and invoking...");
                }
                callBack.markInvoked();
                return constructor.invoke(clazz,arguments);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.metaclass.DynamicMethods#invokeStaticMethod(java.lang.Object, java.lang.String, java.lang.Object[], org.codehaus.groovy.grails.metaclass.InvocationCallback)
     */
    public Object invokeStaticMethod(Object object, String methodName,
            Object[] arguments, InvocationCallback callBack) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[DynamicMethods] Attempting invocation of dynamic static method [" + methodName +
                    "] on target [" + object + "] with arguments [" + ArrayUtils.toString(arguments) + "]");
        }
        for (StaticMethodInvocation methodInvocation : staticMethodInvocations) {
            if (methodInvocation.isMethodMatch(methodName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[DynamicMethods] Static method matched, attempting to invoke");
                }

                try {
                    Object result = methodInvocation.invoke(clazz, methodName, arguments);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[DynamicMethods] Static method [" + methodName +
                                "] invoked successfully with result [" + result + "]. Marking as invoked");
                    }
                    callBack.markInvoked();
                    return result;
                } catch (MissingMethodException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[DynamicMethods] Static method [" + methodName +
                                "] threw MissingMethodException. Returning null and falling back to standard MetaClass",e);
                    }
                    return null;
                }
            }
        }
        return null;
    }

    public DynamicProperty getDynamicProperty(String propertyName) {
        return dynamicProperties.get(propertyName);
    }

    public DynamicMethodInvocation getDynamicMethod(String methodSignature) {
        for (DynamicMethodInvocation methodInvocation : dynamicMethodInvocations) {
            if (methodInvocation.isMethodMatch(methodSignature)) {
                return methodInvocation;
            }
        }
        return null;
    }
}
