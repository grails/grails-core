/*
 * Copyright 2006-2007 Graeme Rocher
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
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Abstract base implementation of the Converter interface that provides a default toString()
 * implementation.
 *
 * @author Siegfried Puchbauer
 */
public abstract class AbstractConverter<W> implements Converter<W> {

    public abstract void setTarget(Object target);

    @Override
    public String toString() {
        FastStringWriter writer = new FastStringWriter();
        try {
            render(writer);
        }
        catch (Exception e) {
            throw new UnhandledException(e);
        }
        return writer.toString();
    }

    protected BeanWrapper createBeanWrapper(Object o) {
        return new BeanWrapperImpl(o);
    }
}
