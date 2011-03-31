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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.util.ReflectionUtils;

public class BeforeValidateHelper {
    public static final String BEFORE_VALIDATE = "beforeValidate";

    public void invokeBeforeValidate(final Object target, final List<?> validatedFieldsList) {
        Class<?> domainClass = target.getClass();
        Method method = null;
        if (validatedFieldsList == null) {
            // prefer the no-arg version of beforeValidate() if validatedFieldsList
            // is null...
            method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE);
            if (method == null) {
                method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE, List.class);
            }
        }
        else {
            // prefer the list-arg version of beforeValidate() if
            // validatedFieldsList is not null...
            method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE, List.class);
            if (method == null) {
                method = ReflectionUtils.findMethod(domainClass, BEFORE_VALIDATE);
            }
        }
        if (method != null) {
            ReflectionUtils.makeAccessible(method);
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {
                ReflectionUtils.invokeMethod(method, target, validatedFieldsList);
            }
            else {
                ReflectionUtils.invokeMethod(method, target);
            }
        }
    }
}
