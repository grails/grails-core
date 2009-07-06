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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;

import java.beans.IntrospectionException;

/**
 * This MetaClass extends ExpandoMetaClass and adds the ability to use regex to specified method matches that
 * then get passed to the invocable closure. Example:
 *
 * 			metaClass./^findBy(\w+)$/ = { matcher, args ->
 *
 *		    }
 *
 * The first argument to the closure is the Regex Matcher. The second is the arguments to the method. This MetaClass
 * allows you to implement thigns like dynamic finders in a trivial manner.
 *
 * The regular expression MUST start with a ^ and end with a $ otherwise it won't be regarded as a valid
 * regex expression and an error will be thrown. Otherwise the mechanism is similar to that provided
 * by ExpandoMetaClass
 *
 * WARNING: Unlike ExpandoMetaClass this MetaClass uses method proxying, hence there is a an overhead attached
 * to its use. This makes it less suitable for use on commonly used objects like java.lang.Object or java.lang.String
 * if performance is important to your application consider other options like the regular ExpandoMetaClass
 *
 * If usage can be isolated to a small set of use cases (such as dynamic finders in Grails) then there is
 * no problem as proxying is not occuring for every method call
 *
 * WARNING: This MetaClass does not support inheritance heirarchies. In other words a child class will not be able
 * to invoke a dynamically added method that exists on a super class 
 *
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 2, 2007
 *        Time: 6:31:31 PM
 */
public class DynamicMethodsExpandoMetaClass extends ExpandoMetaClass {
    
    private DynamicMethods dynamicMethods;
    private static final String REGEX_START = "^";
    private static final String REGEX_END = "$";

    /**
     * Constructs a new DynamicMethodsExpandoMetaClass given the current class. Note that this constructor will NOT
     * place this MetaClass in the MetaClassRegistry. It is up to you to either place it in the MetaClassRegistry or call setMetaClass
     * on GroovyObject
     *
     * @param aClass The class to create the MetaClass for
     *
     * @throws IntrospectionException Thrown if an error occurs introspecting the class
     */
    public DynamicMethodsExpandoMetaClass(Class aClass) throws IntrospectionException {
        super(aClass, false, true);

        this.dynamicMethods = new DefaultDynamicMethods(aClass);
    }

    /**
     * Constructs a new DynamicMethodsExpandoMetaClass given the current class and places it in the MetaClassRegistry
     *
     * @param aClass The class to create the MetaClass for
     *
     * @throws IntrospectionException Thrown if an error occurs introspecting the class
     */
    public DynamicMethodsExpandoMetaClass(Class aClass, boolean inReg) throws IntrospectionException {
        super(aClass, false, true);
        this.dynamicMethods = new DefaultDynamicMethods(aClass);
        if(inReg) {
            registry.setMetaClass(aClass, this);
        }        
    }

    /**
     * Either invokes a intercepted dyanmic static method or the adapted original MetaClass
     *
     * @param target The target object
     * @param methodName The method name
     * @param arguments The arguments to the method
     * @return The return value
     */
    public Object invokeStaticMethod(Object target, String methodName, Object[] arguments) {
		InvocationCallback callback = new InvocationCallback();
		Object returnValue = this.dynamicMethods.invokeStaticMethod(target, methodName, arguments, callback);
		if (callback.isInvoked()) {
			return returnValue;
		} else {
			return super.invokeStaticMethod(target, methodName, arguments);
		}
	}

    public void setProperty(Class aClass, Object object, String property, Object newValue, boolean b, boolean b1) {
        InvocationCallback callback = new InvocationCallback();
        this.dynamicMethods.setProperty(object,property,newValue,callback);
        if (!callback.isInvoked()) {
            super.setProperty(aClass,object, property, newValue, b ,b1);
        }
    }

    public Object getProperty(Class aClass, Object object, String property, boolean b, boolean b1) {
        InvocationCallback callback = new InvocationCallback();
        Object returnValue = this.dynamicMethods.getProperty(object,property,callback);
        if (callback.isInvoked()) {
            return returnValue;
        } else {
            return super.getProperty(aClass,object,property, b, b1);
        }

    }/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#invokeConstructor(java.lang.Object[])
	 */
	public Object invokeConstructor(Object[] arg0) {
		InvocationCallback callback = new InvocationCallback();
		Object instance = this.dynamicMethods.invokeConstructor(arg0,callback);
		if(callback.isInvoked()) {
			return instance;
		}
		else {
			return super.invokeConstructor(arg0);
		}
	}

    public Object invokeMethod(Class aClass, Object target, String methodName, Object[] arguments, boolean b, boolean b1) {
        InvocationCallback callback = new InvocationCallback();
        Object returnValue = this.dynamicMethods.invokeMethod(target, methodName, arguments, callback);
        if (callback.isInvoked()) {
            return returnValue;
        } else {
            return super.invokeMethod(aClass, target, methodName, arguments, b, b1);
        }

    }



    /**
     * Wraps an existing ExpandoMetaBeanProperty and interceptors methods registration to check if the specified
     * method addition is a regex method
     */
    class DynamicExpandoMetaProperty extends ExpandoMetaProperty {
        private DynamicExpandoMetaProperty(ExpandoMetaProperty wrapped) {
            super(wrapped.getPropertyName(), wrapped.isStatic());
        }
        public Object leftShift(Object arg) {
            if(isRegexMethod(propertyName, arg)) {
                registerDynamicMethodInvocation(propertyName,arg);
                return this;
            }
            else {
                return super.leftShift(arg);
            }
        }

        private void registerDynamicMethodInvocation(String name, Object newValue) {
            if(isStatic) {
                dynamicMethods.addStaticMethodInvocation( new ClosureInvokingDynamicMethod(name, (Closure)newValue) );
            }
            else {
                dynamicMethods.addDynamicMethodInvocation(new ClosureInvokingDynamicMethod(name, (Closure)newValue));
            }
        }

        public void setProperty(String property, Object newValue) {
            if(isRegexMethod(property,newValue)) {
                registerDynamicMethodInvocation(property, newValue);
            }
            else if(newValue instanceof Closure) {
                if(isStatic) {
                    registerStaticMethod(property, (Closure)newValue);
                }
                else {
                    registerInstanceMethod(property, (Closure)newValue);
                }
            }
        }
    }


    public Object getProperty(String name) {
        Object propertyValue = super.getProperty(name);
        if(propertyValue instanceof ExpandoMetaProperty) {
            return new DynamicExpandoMetaProperty((ExpandoMetaProperty)propertyValue);
        }
        else {
            return propertyValue;
        }
    }




    public void setProperty(String name, Object value) {
        if(isRegexMethod(name, value)) {
            this.dynamicMethods.addDynamicMethodInvocation(new ClosureInvokingDynamicMethod(name, (Closure)value));
        }
        else {
            super.setProperty(name,value);
        }
    }

    private boolean isRegexMethod(String name, Object value) {
        return name.startsWith(REGEX_START) && name.endsWith(REGEX_END) && value instanceof Closure;
    }

}
