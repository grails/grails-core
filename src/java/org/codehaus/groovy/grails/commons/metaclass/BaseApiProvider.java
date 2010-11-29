/*
 * Copyright 2010 the original author or authors.
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

import groovy.lang.MetaMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.metaclass.ReflectionMetaMethod;

/**
 *
 * 
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public abstract class BaseApiProvider {

	protected List<MetaMethod> instanceMethods = new ArrayList<MetaMethod>();
	protected List<Method> staticMethods = new ArrayList<Method>();
	
	public void addApi(final Object apiInstance) {
		if(apiInstance != null) {
			Class<?> currentClass = apiInstance.getClass();
			while(currentClass != Object.class) {
				final Method[] declaredMethods = currentClass.getDeclaredMethods();
				
				for (Method method : declaredMethods) {
					final int modifiers = method.getModifiers();
					if(Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers)) {
						if(Modifier.isStatic(modifiers)) {
							staticMethods.add(method);
						}
						else {
							instanceMethods.add(new ReflectionMetaMethod(new CachedMethod(method)) {
								@Override
								public Object invoke(Object object,
										Object[] arguments) {
									return super.invoke(apiInstance, ArrayUtils.add(arguments, 0, object));
								}
								
								@Override
								public CachedClass[] getParameterTypes() {
									final CachedClass[] paramTypes = method.getParameterTypes();
									if(paramTypes.length>0)
										return (CachedClass[]) ArrayUtils.subarray(paramTypes, 1, paramTypes.length);
									else
									  return paramTypes;
								}
							});
						}
					}
				}
				currentClass = currentClass.getSuperclass();
			}
		}
	}
	

}
