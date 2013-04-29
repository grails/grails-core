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
package grails.spring;

import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * Extended version of the BeanBuilder class that provides support for constructing WebApplicationContext instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class WebBeanBuilder extends BeanBuilder {

    public WebBeanBuilder() {
        super();
    }

    public WebBeanBuilder(ClassLoader classLoader) {
        super(classLoader);
    }

    public WebBeanBuilder(ApplicationContext parent) {
        super(parent);
    }

    public WebBeanBuilder(ApplicationContext parent, ClassLoader classLoader) {
        super(parent, classLoader);
    }

    @Override
    protected RuntimeSpringConfiguration createRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader classLoader) {
        return new WebRuntimeSpringConfiguration(parent, classLoader);
    }
}
