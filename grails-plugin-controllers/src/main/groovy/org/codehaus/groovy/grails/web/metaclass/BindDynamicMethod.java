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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.MissingMethodException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.web.binding.DataBindingUtils;
import org.springframework.util.Assert;

/**
 * A dynamic method present in controllers allowing data binding from a map to a target instance. Example:
 *
 * <code>
 *         def a = new Account()
 *         bindData(a, params)
 * </code>
 *
 * @author Graeme Rocher
 */
public class BindDynamicMethod extends AbstractDynamicMethodInvocation {

    public static final String METHOD_SIGNATURE = "bindData";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    private static final String INCLUDE_MAP_KEY = "include";
    private static final String EXCLUDE_MAP_KEY = "exclude";

    public BindDynamicMethod() {
        super(METHOD_PATTERN);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object invoke(Object target, String methodName, Object[] arguments) {

        if (arguments.length < 2 || arguments.length > 4) {
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
        }

        if (arguments[0] == null) {
            throw new IllegalArgumentException("Argument [target] is required by method [bindData] on class [" +
                    target.getClass().getName() + "]");
        }

        Object targetObject = arguments[0];
        Object bindParams = arguments[1];
        Map includeExclude = new HashMap();
        List include = null;
        List exclude = null;
        String filter = null;
        switch(arguments.length) {
            case 3:
                if (arguments[2] instanceof String) {
                    filter = (String) arguments[2];
                }
                else {
                    Assert.isInstanceOf(Map.class, arguments[2],
                         "The 3rd Argument for method bindData must represent included and excluded properties " +
                         "and implement the interface java.util.Map or be a String and represent a prefix to filter parameters with");
                    includeExclude = (Map) arguments[2];
                }
                break;
            case 4:
                Assert.isInstanceOf(Map.class, arguments[2],
                    "The 3rd Argument for method bindData must represent included and excluded properties " +
                    "and implement the interface java.util.Map or be a String and represent a prefix to filter parameters with");
                includeExclude = (Map) arguments[2];

                Assert.isInstanceOf(String.class, arguments[3],
                        "Argument [prefix] for method [bindData] must be a String");
                filter = (String) arguments[3];
                break;
        }

        if (includeExclude.containsKey(INCLUDE_MAP_KEY)) {
            include = convertToListIfString(includeExclude.get(INCLUDE_MAP_KEY));
        }

        if (includeExclude.containsKey(EXCLUDE_MAP_KEY)) {
            exclude = convertToListIfString(includeExclude.get(EXCLUDE_MAP_KEY));
        }

        DataBindingUtils.bindObjectToInstance(targetObject, bindParams, include, exclude, filter);
        return targetObject;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List convertToListIfString(Object o) {
        if (o instanceof String) {
            List list = new ArrayList();
            list.add(o);
            o = list;
        }
        return (List) o;
    }
}
