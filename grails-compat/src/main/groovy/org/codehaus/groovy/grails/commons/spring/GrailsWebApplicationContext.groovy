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
package org.codehaus.groovy.grails.commons.spring

import groovy.transform.CompileStatic
import grails.core.GrailsApplication
import org.springframework.beans.BeansException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ApplicationContext

/**
 * A WebApplicationContext that extends StaticApplicationContext to allow for programmatic
 * configuration at runtime. The code is adapted from StaticWebApplicationContext.
 *
 * @author Graeme
 * @since 0.3
 * @deprecated Use {@link grails.web.servlet.context.GrailsWebApplicationContext} instead
 */
@CompileStatic
class GrailsWebApplicationContext extends grails.web.servlet.context.GrailsWebApplicationContext{

    GrailsWebApplicationContext() throws BeansException {
    }

    GrailsWebApplicationContext(GrailsApplication grailsApplication) {
        super(grailsApplication)
    }

    GrailsWebApplicationContext(ApplicationContext parent) throws BeansException {
        super(parent)
    }

    GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, GrailsApplication grailsApplication) {
        super(defaultListableBeanFactory, grailsApplication)
    }

    GrailsWebApplicationContext(ApplicationContext parent, GrailsApplication grailsApplication) throws BeansException {
        super(parent, grailsApplication)
    }

    GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory) {
        super(defaultListableBeanFactory)
    }

    GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, ApplicationContext parent) {
        super(defaultListableBeanFactory, parent)
    }

    GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, ApplicationContext parent, GrailsApplication grailsApplication) {
        super(defaultListableBeanFactory, parent, grailsApplication)
    }
}
