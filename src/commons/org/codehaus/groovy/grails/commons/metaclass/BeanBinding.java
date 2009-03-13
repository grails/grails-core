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

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Map;

/**
 * Extends Groovy's Binding Object to allow a binding to a particular bean where the
 * properties of the Object become binding variables
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jul 22, 2008
 */
public class BeanBinding extends Binding {
    private Object bean;

    public BeanBinding(Object bean) {
        super();
        this.bean = bean;
    }

    public Object getVariable(String name) {
        MetaClass mc = getBeanMetaClass();
        return mc.getProperty(bean,name);

    }

    private MetaClass getBeanMetaClass() {
        if(bean instanceof GroovyObject) {
            return ((GroovyObject)bean).getMetaClass();
        }
        return GroovySystem.getMetaClassRegistry().getMetaClass(bean.getClass());
    }

    public void setVariable(String name, Object value) {
        MetaClass mc = getBeanMetaClass();
        mc.setProperty(bean, name,value);
    }

    public Map getVariables() {
        return DefaultGroovyMethods.getProperties(bean);
    }
}
