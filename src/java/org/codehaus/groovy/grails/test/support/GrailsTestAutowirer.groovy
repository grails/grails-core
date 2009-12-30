/*
 * Copyright 2009 the original author or authors.
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

package org.codehaus.groovy.grails.test.support

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

/**
 * Convenience class to autowire test classes
 */
class GrailsTestAutowirer {

    ApplicationContext applicationContext
    
    GrailsTestAutowirer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Autowires the bean by name, and set's the applicationContext if it implements ApplicationContextAware.
     */
    void autowire(bean) {
        applicationContext.autowireCapableBeanFactory.autowireBeanProperties(
            bean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false
        )

        if (bean instanceof ApplicationContextAware && applicationContext != null) {
            bean.setApplicationContext(applicationContext)
        }
    }
}