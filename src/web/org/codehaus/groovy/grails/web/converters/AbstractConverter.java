/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.converters;

import org.apache.commons.lang.UnhandledException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.StringWriter;
import java.lang.reflect.Constructor;

/**
 * Abstract base implementation of the Converter interface that provides a default toString()
 * implementation
 *
 * @author Siegfried Puchbauer
 */
public abstract class AbstractConverter implements Converter {
    private static final String PERSISTENCE_BEAN_WRAPPER_CLASS = "org.codehaus.groovy.grails.orm.hibernate.support.HibernateBeanWrapper";

    public abstract void setTarget(Object target);

    /**
     * Renders the result to a String and returns it
     *
     * @return The converted object as a string
     */
    public String toString() {
        StringWriter writer = new StringWriter();
        try {
            render(writer);
        } catch (Exception e) {
            throw new UnhandledException(e);
        }
        return writer.toString();
    }


    protected BeanWrapper createBeanWrapper(Object o) {
        BeanWrapper beanWrapper;
        try {
            Class c = Class.forName(PERSISTENCE_BEAN_WRAPPER_CLASS);
            Constructor init = c.getConstructor(new Class[]{Object.class});
            beanWrapper = (BeanWrapper)init.newInstance(new Object[]{o});
        } catch (Exception e) {
            beanWrapper = new BeanWrapperImpl(o);
        }
        return beanWrapper;
    }
}
