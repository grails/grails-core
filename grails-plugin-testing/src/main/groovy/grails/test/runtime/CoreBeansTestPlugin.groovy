/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime;

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.codehaus.groovy.grails.commons.cfg.GrailsPlaceholderConfigurer
import org.codehaus.groovy.grails.commons.cfg.MapBasedSmartPropertyOverrideConfigurer
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.grails.plugins.databinding.DataBindingGrailsPlugin
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.StaticMessageSource

/**
 * a TestPlugin for TestRuntime that adds some generic beans that are
 * required in Grails applications
 *
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
public class CoreBeansTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication']
    String[] providedFeatures = ['coreBeans']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerParentBeans(TestRuntime runtime, GrailsApplication grailsApplicationParam) {
        defineParentBeans(runtime) {
            grailsApplication(InstanceFactoryBean, grailsApplicationParam, GrailsApplication)
            pluginManager(NoOpGrailsPluginManager)
            conversionService(ConversionServiceFactoryBean)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplicationParam) {
        defineBeans(runtime, new DataBindingGrailsPlugin().doWithSpring)

        defineBeans(runtime) {
            xmlns context:"http://www.springframework.org/schema/context"
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            proxyHandler(DefaultProxyHandler)
            messageSource(StaticMessageSource)
            "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator)
            grailsApplicationPostProcessor(GrailsApplicationAwareBeanPostProcessor, grailsApplicationParam)
            grailsPlaceholderConfigurer(GrailsPlaceholderConfigurer, grailsApplicationParam)
            mapBasedSmartPropertyOverrideConfigurer(MapBasedSmartPropertyOverrideConfigurer, grailsApplicationParam)
        }
    }

    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }

    void defineParentBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineParentBeans", [closure: closure])
    }

    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'registerParentBeans':
                registerParentBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }

    public void close(TestRuntime runtime) {

    }
}
