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
import grails.config.ConfigProperties
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import reactor.Environment
import reactor.core.config.ConfigurationReader
import reactor.core.config.DispatcherConfiguration
import reactor.core.config.DispatcherType
import reactor.core.config.ReactorConfiguration


/**
 * Configures Reactor within a Grails application
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Commons
class GrailsReactorConfigurationReader implements ConfigurationReader {

    public static final String DEFAULT_DISPATCHER = 'reactor.dispatchers.default'
    public static final int DEFAULT_BACKLOG = 2048
    public static final int DEFAULT_SIZE = 0
    public static final int DEFAULT_RINGBUFFER_BACKLOG = 8192

    Config configuration
    Properties configurationProperties = new Properties()

    GrailsReactorConfigurationReader(Config configuration, Properties configurationProperties) {
        this.configuration = configuration
        this.configurationProperties = configurationProperties
    }

    @Override
    ReactorConfiguration read() {
        def config = configuration
        if(config) {
            def defaultDispatcher = config.getProperty(DEFAULT_DISPATCHER, String, Environment.THREAD_POOL)
            List<DispatcherConfiguration> dispatcherConfigurations = []

            def dispatcherConfigs = config.getProperty('reactor.dispatchers', Map, Collections.emptyMap())
            if(dispatcherConfigs) {
                for(dispatcherName in dispatcherConfigs.keySet()) {
                    if('default' == dispatcherName) continue
                    def dispatcherType = getType( config.getProperty("reactor.dispatchers.${dispatcherName}.type", String) )
                    if(dispatcherType) {
                        dispatcherConfigurations << new DispatcherConfiguration(dispatcherName.toString(),
                                                                                dispatcherType,
                                                                                config.getProperty("reactor.dispatchers.${dispatcherName}.backlog", Integer, DEFAULT_BACKLOG),
                                                                                config.getProperty("reactor.dispatchers.${dispatcherName}.size", Integer, DEFAULT_SIZE))
                    }
                }
            }
            else {
                populateDefaultDispatchers(dispatcherConfigurations)
            }

            return new ReactorConfiguration(dispatcherConfigurations, defaultDispatcher, configurationProperties)
        }
        else {
            List<DispatcherConfiguration> dispatcherConfigurations = []
            populateDefaultDispatchers(dispatcherConfigurations)

            return new ReactorConfiguration(dispatcherConfigurations, Environment.THREAD_POOL, configurationProperties )
        }
    }

    protected void populateDefaultDispatchers(List<DispatcherConfiguration> dispatcherConfigurations) {
        dispatcherConfigurations << new DispatcherConfiguration(Environment.THREAD_POOL, DispatcherType.THREAD_POOL_EXECUTOR, DEFAULT_BACKLOG, DEFAULT_SIZE)
        dispatcherConfigurations << new DispatcherConfiguration(Environment.DISPATCHER_GROUP, DispatcherType.DISPATCHER_GROUP, DEFAULT_BACKLOG, DEFAULT_SIZE)
        dispatcherConfigurations << new DispatcherConfiguration(Environment.SHARED, DispatcherType.RING_BUFFER, DEFAULT_RINGBUFFER_BACKLOG, DEFAULT_SIZE)
        dispatcherConfigurations << new DispatcherConfiguration(Environment.WORK_QUEUE, DispatcherType.WORK_QUEUE, DEFAULT_BACKLOG, DEFAULT_SIZE)
    }

    private DispatcherType getType(String type) {
        if("dispatcherGroup".equals(type)) {
            return DispatcherType.DISPATCHER_GROUP
        } else if("mpsc".equals(type)) {
            return DispatcherType.MPSC
        } else if("ringBuffer".equals(type)) {
            return DispatcherType.RING_BUFFER
        } else if("synchronous".equals(type)) {
            return DispatcherType.SYNCHRONOUS
        } else if("threadPoolExecutor".equals(type)) {
            return DispatcherType.THREAD_POOL_EXECUTOR
        } else if("workQueue".equals(type)) {
            return DispatcherType.WORK_QUEUE
        } else {
            log.warn("The type '$type' of Dispatcher is not recognized")
        }
    }
}
