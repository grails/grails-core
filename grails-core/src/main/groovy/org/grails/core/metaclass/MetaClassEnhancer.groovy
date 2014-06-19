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
package org.grails.core.metaclass

import grails.util.GrailsMetaClassUtils

import java.lang.reflect.Method

import org.codehaus.groovy.runtime.metaclass.ReflectionMetaMethod
import org.springframework.beans.BeanUtils

/**
 * Enhances one or many MetaClasses with the given API methods provided by the super class BaseApiProvider.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class MetaClassEnhancer extends BaseApiProvider {

    void enhance(MetaClass mc) {
        def cls = mc.theClass
        def metaClass = GrailsMetaClassUtils.getExpandoMetaClass(cls)

        for (c in constructors) {
            def method = c
            def paramTypes = method.parameterTypes.length == 1 ? [] : method.parameterTypes[1..-1] as Class[]
            metaClass.constructor = new Closure(this) {
                @Override
                Class[] getParameterTypes() {
                    return paramTypes
                }

                @Override
                Object call(Object... args) {
                    def instance = BeanUtils.instantiate((Class)delegate)
                    method.invoke(method.getDeclaringClass(), instance, *args)
                    return instance
                }

            }
        }
        for (method in instanceMethods) {
            if (method instanceof ReflectionMetaMethod) {
                metaClass.registerInstanceMethod(method)
            }
        }

        for (Method method : staticMethods) {
            def methodName = method.name
            metaClass.static."${methodName}" = method.declaringClass.&"${methodName}"
        }
    }

    void enhanceAll(Iterable metaClasses) {
        for (MetaClass metaClass in metaClasses) {
            enhance metaClass
        }
    }
}
