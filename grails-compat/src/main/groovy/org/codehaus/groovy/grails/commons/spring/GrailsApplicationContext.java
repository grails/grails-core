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
package org.codehaus.groovy.grails.commons.spring;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * An ApplicationContext that extends StaticApplicationContext and implements GroovyObject such that
 * beans can be retrieved with the dot de-reference syntax instead of using getBean('name').
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Use {@link org.grails.spring.GrailsApplicationContext}
 */
@Deprecated
public class GrailsApplicationContext extends org.grails.spring.GrailsApplicationContext {

    public GrailsApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory) {
        super(defaultListableBeanFactory);
    }

    public GrailsApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, ApplicationContext applicationContext) {
        super(defaultListableBeanFactory, applicationContext);
    }

    public GrailsApplicationContext(org.springframework.context.ApplicationContext parent) throws org.springframework.beans.BeansException {
        super(parent);
    }

    public GrailsApplicationContext() throws org.springframework.beans.BeansException {
        super();
    }
}
