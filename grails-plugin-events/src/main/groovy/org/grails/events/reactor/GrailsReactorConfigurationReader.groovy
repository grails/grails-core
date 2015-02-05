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
package org.grails.events.reactor

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import groovy.transform.CompileStatic
import org.grails.config.PrefixedConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import reactor.Environment
import reactor.core.config.ConfigurationReader
import reactor.core.config.DispatcherConfiguration
import reactor.core.config.DispatcherType
import reactor.core.config.ReactorConfiguration


/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsReactorConfigurationReader implements ConfigurationReader, GrailsConfigurationAware {

    public static final String DEFAULT_DISPATCHER = 'reactor.dispatchers.default'
    public static final int DEFAULT_BACKLOG = 2048
    public static final int DEFAULT_SIZE = 0
    public static final int DEFAULT_RINGBUFFER_BACKLOG = 8192

    Config configuration

    @Autowired
    @Qualifier("grailsConfigProperties")
    Properties configurationProperties


    @Override
    ReactorConfiguration read() {
        def config = configuration
        if(config) {
            def defaultDispatcher = config.getProperty(DEFAULT_DISPATCHER, String, Environment.THREAD_POOL)
            List<DispatcherConfiguration> dispatcherConfigurations = []

            def dispatcherConfigs = config.getProperty('reactor.dispatchers', Map, Collections.emptyMap())
            if(dispatcherConfigs) {
                for(dispatcherName in dispatcherConfigs.keySet()) {
                    def dispatcherType = config.getProperty("reactor.dispatchers.${dispatcherName}.type", String)
                    if(dispatcherType) {
                        dispatcherConfigurations << new DispatcherConfiguration(dispatcherName.toString(),
                                                                                DispatcherType.valueOf(dispatcherType),
                                                                                config.getProperty("reactor.dispatchers.${dispatcherName}.backlog", Integer, DEFAULT_BACKLOG),
                                                                                config.getProperty("reactor.dispatchers.${dispatcherName}.size", Integer, DEFAULT_SIZE))
                    }
                }
            }
            else {
                dispatcherConfigurations << new DispatcherConfiguration(Environment.THREAD_POOL, DispatcherType.THREAD_POOL_EXECUTOR, DEFAULT_BACKLOG, DEFAULT_SIZE)
                dispatcherConfigurations << new DispatcherConfiguration(Environment.DISPATCHER_GROUP, DispatcherType.DISPATCHER_GROUP, DEFAULT_BACKLOG, DEFAULT_SIZE)
                dispatcherConfigurations << new DispatcherConfiguration(Environment.SHARED, DispatcherType.RING_BUFFER, DEFAULT_RINGBUFFER_BACKLOG, DEFAULT_SIZE)
                dispatcherConfigurations << new DispatcherConfiguration(Environment.WORK_QUEUE, DispatcherType.WORK_QUEUE, DEFAULT_BACKLOG, DEFAULT_SIZE)
            }

            return new ReactorConfiguration(dispatcherConfigurations, defaultDispatcher, configurationProperties)
        }
    }
}
