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

package org.grails.plugins.events

import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.events.reactor.GrailsReactorConfigurationReader
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import reactor.Environment
import reactor.bus.EventBus
import reactor.spring.context.config.ConsumerBeanAutoConfiguration


/**
 * A plugin that integrates Reactor into Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@Commons
class EventBusGrailsPlugin extends Plugin {

    def version = GrailsUtil.grailsVersion

    @Override
    Closure doWithSpring() {
        {->
            reactorConfigurationReader(GrailsReactorConfigurationReader, grailsApplication.config, ref("grailsConfigProperties"))
            reactorEnv(Environment, ref("reactorConfigurationReader"))

            eventBus(MethodInvokingFactoryBean) { bean ->
                targetClass = EventBus
                targetMethod = "create"
                arguments = [reactorEnv]
            }
            consumerBeanAutoConfiguration(ConsumerBeanAutoConfiguration)
        }
    }

    @Override
    @CompileStatic
    void doWithApplicationContext() {
        if( !Environment.alive() ) {
            Environment.assign applicationContext.getBean('reactorEnv', Environment)
        }
    }

    @Override
    @CompileStatic
    void onShutdown(Map<String, Object> event) {
        try {
            if( Environment.alive() ) {
                Environment.terminate()
            }
        } catch (Throwable e) {
            log.warn("Error shutting down Reactor: ${e.message}", e)
        }
    }
}
