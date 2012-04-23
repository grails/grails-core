package org.codehaus.groovy.grails.plugins.web.filters;

import groovy.lang.MetaMethod;

import org.codehaus.groovy.reflection.CachedClass;

/**
 * MetaMethod implementation that delegates to real MetaMethod implementation
 * 
 * This can be used to efficiently proxy a metamethod from another metaClass in methodMissing.
 * An example can be found in FilterConfig's methodMissing .
 * 
 * Without this class it's hard to implement efficient methodMissing "caching" supporting methods with multiple signatures (same method name, different set of arguments).
 * 
 * This class could be moved to org.codehaus.groovy.grails.commons.metaclass for reuse.
 * 
 * @author Lari Hotari
 *
 */
@SuppressWarnings("rawtypes")
class DelegateMetaMethod extends MetaMethod {
    static interface DelegateMetaMethodTargetStrategy {
        public Object getTargetInstance(Object instance);
    }
    
    private MetaMethod delegateMethod;
    private DelegateMetaMethodTargetStrategy targetStrategy;
    
    public DelegateMetaMethod(MetaMethod delegateMethod, DelegateMetaMethodTargetStrategy targetStrategy) {
        this.delegateMethod=delegateMethod;
        this.targetStrategy=targetStrategy;
    }

    public int getModifiers() {
        return delegateMethod.getModifiers();
    }

    public String getName() {
        return delegateMethod.getName();
    }

    public Class getReturnType() {
        return delegateMethod.getReturnType();
    }

    public Object invoke(Object object, Object[] arguments) {
        return delegateMethod.invoke(targetStrategy.getTargetInstance(object), arguments);
    }

    public void checkParameters(Class[] arguments) {
        delegateMethod.checkParameters(arguments);
    }

    public CachedClass[] getParameterTypes() {
        return delegateMethod.getParameterTypes();
    }

    public boolean isMethod(MetaMethod method) {
        return delegateMethod.isMethod(method);
    }

    public Class[] getNativeParameterTypes() {
        return delegateMethod.getNativeParameterTypes();
    }

    public boolean isVargsMethod(Object[] arguments) {
        return delegateMethod.isVargsMethod(arguments);
    }

    public String toString() {
        return delegateMethod.toString();
    }

    public boolean equals(Object obj) {
        return delegateMethod.equals(obj);
    }

    public Object clone() {
        return delegateMethod.clone();
    }

    public boolean isStatic() {
        return delegateMethod.isStatic();
    }

    public boolean isAbstract() {
        return delegateMethod.isAbstract();
    }

    public Object[] correctArguments(Object[] argumentArray) {
        return delegateMethod.correctArguments(argumentArray);
    }

    public boolean isCacheable() {
        return delegateMethod.isCacheable();
    }

    public String getDescriptor() {
        return delegateMethod.getDescriptor();
    }

    public String getSignature() {
        return delegateMethod.getSignature();
    }

    public String getMopName() {
        return delegateMethod.getMopName();
    }

    public Object doMethodInvoke(Object object, Object[] argumentArray) {
        return delegateMethod.doMethodInvoke(targetStrategy.getTargetInstance(object), argumentArray);
    }

    public boolean isValidMethod(Class[] arguments) {
        return delegateMethod.isValidMethod(arguments);
    }

    public boolean isValidExactMethod(Object[] args) {
        return delegateMethod.isValidExactMethod(args);
    }

    public boolean isValidExactMethod(Class[] args) {
        return delegateMethod.isValidExactMethod(args);
    }

    public boolean isValidMethod(Object[] arguments) {
        return delegateMethod.isValidMethod(arguments);
    }

    public CachedClass getDeclaringClass() {
        return delegateMethod.getDeclaringClass();
    }
}
