/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.web.servlet.mvc;

import grails.web.Action;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.collections.keyvalue.AbstractMapEntry;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Filter action getters
 *
 * @author Stephane Maldini
 * @since 2.0
 */
public class GrailsControllerBeanMap extends BeanMap {

    public GrailsControllerBeanMap(Object bean) {
        super(bean);
    }

    @Override
    public Object get(Object name) {
        Method method = getReadMethod(name);
        if (method.getAnnotation(Action.class) != null)
            return null;
        else
            return super.get(name);
    }

}
