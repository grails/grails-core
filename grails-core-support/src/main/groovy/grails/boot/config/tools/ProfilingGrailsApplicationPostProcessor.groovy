/*
 * Copyright 2014 original authors
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
package grails.boot.config.tools

import grails.boot.config.GrailsApplicationPostProcessor
import grails.core.GrailsApplicationLifeCycle
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext


/**
 * Profiles bean creation outputting data to the console
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class ProfilingGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor implements BeanPostProcessor {


    long startTime

    ProfilingGrailsApplicationPostProcessor(GrailsApplicationLifeCycle lifeCycle, ApplicationContext applicationContext, Class... classes) {
        super(lifeCycle, applicationContext, classes)
        ((ConfigurableApplicationContext)applicationContext).beanFactory.addBeanPostProcessor(this)
    }

    @Override
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        startTime = System.currentTimeMillis()
        return bean
    }

    @Override
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        def totalTime = System.currentTimeMillis() - startTime
        if(totalTime > 10) {
            println "Creating bean $beanName of type ${bean.getClass()} took ${totalTime}ms"
        }
        return bean
    }
}
