/*
 * Copyright 2004-2005 Graeme Rocher
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

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.util.Map;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Extends Groovy's Binding Object to allow a binding to a particular bean where the
 * properties of the Object become binding variables.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class BeanBinding extends Binding {

    private Object bean;

    public BeanBinding(Object bean) {
        this.bean = bean;
    }

    @Override
    public Object getVariable(String name) {
        MetaClass mc = getBeanMetaClass();
        return mc.getProperty(bean,name);
    }

    private MetaClass getBeanMetaClass() {
        if (bean instanceof GroovyObject) {
            return ((GroovyObject)bean).getMetaClass();
        }
        return GroovySystem.getMetaClassRegistry().getMetaClass(bean.getClass());
    }

    @Override
    public void setVariable(String name, Object value) {
        getBeanMetaClass().setProperty(bean, name,value);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getVariables() {
        return DefaultGroovyMethods.getProperties(bean);
    }
}
