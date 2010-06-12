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
package org.codehaus.groovy.grails.commons.spring;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletContextAware;

/**
 * Subclasses DefaultRuntimeSpringConfiguration to provide construction of WebApplicationContext instances.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class WebRuntimeSpringConfiguration extends DefaultRuntimeSpringConfiguration implements ServletContextAware {

    public WebRuntimeSpringConfiguration(ApplicationContext parent) {
        super(parent);
    }

    public WebRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader cl) {
        super(parent, cl);
    }

    @Override
    protected GenericApplicationContext createApplicationContext(ApplicationContext parentCtx) {
        if (parentCtx != null && beanFactory != null) {
            if (beanFactory instanceof DefaultListableBeanFactory) {
                return new GrailsWebApplicationContext((DefaultListableBeanFactory) beanFactory,parentCtx);
            }

            throw new IllegalArgumentException(
                    "ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");
        }

        if (beanFactory != null) {
            if (beanFactory instanceof DefaultListableBeanFactory) {
                return new GrailsWebApplicationContext((DefaultListableBeanFactory) beanFactory);
            }

            throw new IllegalArgumentException(
                    "ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");
        }

        if (parentCtx != null) {
            return new GrailsWebApplicationContext(parentCtx);
        }

        return new GrailsWebApplicationContext();
    }

    public void setServletContext(ServletContext servletContext) {
        initialiseApplicationContext();

        if (context instanceof ConfigurableWebApplicationContext) {
            ((ConfigurableWebApplicationContext)context).setServletContext(servletContext);
        }
    }
}
