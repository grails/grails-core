/*
 * Copyright 2011 SpringSource
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
package grails.test.mixin.support

import grails.spring.BeanBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
import org.springframework.context.support.StaticMessageSource
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler

/**
 * A base unit testing mixin that watches for MetaClass changes and unbinds them on tear down
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class GrailsUnitTestMixin {

    static GrailsWebApplicationContext applicationContext
    static GrailsApplication grailsApplication
    static ConfigObject config
    static StaticMessageSource messageSource

    static emcEvents = []

    static void defineBeans(Closure callable) {
        def bb = new BeanBuilder()
        def beans = bb.beans(callable)
        beans.registerBeans(applicationContext)
    }

    @BeforeClass
    static void initGrailsApplication() {
        applicationContext = new GrailsWebApplicationContext()
        final autowiringPostProcessor = new AutowiredAnnotationBeanPostProcessor()
        autowiringPostProcessor.beanFactory = applicationContext.autowireCapableBeanFactory
        applicationContext.beanFactory.addBeanPostProcessor(autowiringPostProcessor)


        defineBeans {
            grailsProxyHandler(DefaultProxyHandler)
            grailsApplication(DefaultGrailsApplication)
            messageSource(StaticMessageSource)
        }
        applicationContext.refresh()
        grailsApplication = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication)
        applicationContext.beanFactory.addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication))
        messageSource = applicationContext.getBean("messageSource")
        grailsApplication.initialise()

        grailsApplication.applicationContext = applicationContext
        config = grailsApplication.config
    }

    @Before
    void registerMetaClassRegistryWatcher() {
        def listener = { MetaClassRegistryChangeEvent event ->
            GrailsUnitTestMixin.emcEvents << event
        } as MetaClassRegistryChangeEventListener

        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener listener
    }

    @After
    void cleanupModifiedMetaClasses() {
        emcEvents*.clazz.each { GroovySystem.metaClassRegistry.removeMetaClass(it)}
    }

    @AfterClass
    static void shutdownApplicationContext() {
        if(applicationContext.isActive()) {
            applicationContext.close()
        }
        applicationContext = null
        grailsApplication = null
    }
}
