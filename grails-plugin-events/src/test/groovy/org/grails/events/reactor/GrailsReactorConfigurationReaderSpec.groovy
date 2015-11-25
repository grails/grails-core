package org.grails.events.reactor

import grails.config.ConfigProperties
import org.grails.config.PropertySourcesConfig
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.boot.env.PropertiesPropertySourceLoader
import org.springframework.core.io.ByteArrayResource
import reactor.core.config.DispatcherType
import reactor.core.config.ReactorConfiguration
import spock.lang.Specification

/**
 * Created by graemerocher on 25/11/15.
 */
class GrailsReactorConfigurationReaderSpec extends Specification {
    void "Test default reactor configuration"() {
        given:"A reactor config"
        def config = new PropertySourcesConfig()

        when:
        def configReader = new GrailsReactorConfigurationReader(config, new ConfigProperties(config))
        def reactorConfiguration = configReader.read()

        then:
        reactorConfiguration.dispatcherConfigurations.size() == 4
        reactorConfiguration.defaultDispatcherName == 'threadPoolExecutor'
        reactorConfiguration.dispatcherConfigurations[0].name == 'threadPoolExecutor'
        reactorConfiguration.dispatcherConfigurations[0].type == DispatcherType.THREAD_POOL_EXECUTOR
        reactorConfiguration.dispatcherConfigurations[0].size == 0
        reactorConfiguration.dispatcherConfigurations[0].backlog == 2048
        reactorConfiguration.dispatcherConfigurations[1].name == 'dispatcherGroup'
        reactorConfiguration.dispatcherConfigurations[1].type == DispatcherType.DISPATCHER_GROUP
        reactorConfiguration.dispatcherConfigurations[1].size == 0
        reactorConfiguration.dispatcherConfigurations[1].backlog == 2048
        reactorConfiguration.dispatcherConfigurations[2].name == 'shared'
        reactorConfiguration.dispatcherConfigurations[2].type == DispatcherType.RING_BUFFER
        reactorConfiguration.dispatcherConfigurations[2].size == 0
        reactorConfiguration.dispatcherConfigurations[2].backlog == 8192
        reactorConfiguration.dispatcherConfigurations[3].name == 'workQueue'
        reactorConfiguration.dispatcherConfigurations[3].type == DispatcherType.WORK_QUEUE
        reactorConfiguration.dispatcherConfigurations[3].size == 0
        reactorConfiguration.dispatcherConfigurations[3].backlog == 2048
    }

    void "Test custom reactor configuration"() {
        given:"A reactor config"
        def propertySource = new YamlPropertySourceLoader().load("application", new ByteArrayResource('''
reactor:
    dispatchers:
        default: myExecutor
        myExecutor:
            type: threadPoolExecutor
            size: 10
            backlog: 1024
'''.getBytes("UTF-8")), null)

        def config = new PropertySourcesConfig(propertySource)

        when:
        def configReader = new GrailsReactorConfigurationReader(config, new ConfigProperties(config))
        def reactorConfiguration = configReader.read()

        then:
        reactorConfiguration.dispatcherConfigurations.size() == 1
        reactorConfiguration.defaultDispatcherName == 'myExecutor'
        reactorConfiguration.dispatcherConfigurations[0].name == 'myExecutor'
        reactorConfiguration.dispatcherConfigurations[0].type == DispatcherType.THREAD_POOL_EXECUTOR
        reactorConfiguration.dispatcherConfigurations[0].size == 10
        reactorConfiguration.dispatcherConfigurations[0].backlog == 1024
    }

    void "Test custom reactor configuration 2"() {
        given:"A reactor config"
        def propertySource = new YamlPropertySourceLoader().load("application", new ByteArrayResource('''
reactor:
  dispatchers:
    default: threadPool
    threadPool:
      type: threadPoolExecutor
      size: 5
      backlog: 1024
    dispatcherGroup:
      type: dispatcherGroup
      size: 0
      backlog: 1024
'''.getBytes("UTF-8")), null)

        def config = new PropertySourcesConfig(propertySource)

        when:
        def configReader = new GrailsReactorConfigurationReader(config, new ConfigProperties(config))
        def reactorConfiguration = configReader.read()

        then:
        reactorConfiguration.dispatcherConfigurations.size() == 2
        reactorConfiguration.defaultDispatcherName == 'threadPool'
        reactorConfiguration.dispatcherConfigurations[0].name == 'threadPool'
        reactorConfiguration.dispatcherConfigurations[0].type == DispatcherType.THREAD_POOL_EXECUTOR
        reactorConfiguration.dispatcherConfigurations[0].size == 5
        reactorConfiguration.dispatcherConfigurations[0].backlog == 1024
    }

    void "Test custom reactor configuration 3"() {
        given:"A reactor config"
        def propertySource = new PropertiesPropertySourceLoader().load("application", new ByteArrayResource('''
reactor.dispatchers.default=threadPool
reactor.dispatchers.threadPool.type=threadPoolExecutor
reactor.dispatchers.threadPool.size=5
reactor.dispatchers.threadPool.backlog=1024
'''.getBytes("UTF-8")), null)

        def config = new PropertySourcesConfig(propertySource)

        when:
        def configReader = new GrailsReactorConfigurationReader(config, new ConfigProperties(config))
        def reactorConfiguration = configReader.read()

        then:
        reactorConfiguration.dispatcherConfigurations.size() == 1
        reactorConfiguration.defaultDispatcherName == 'threadPool'
        reactorConfiguration.dispatcherConfigurations[0].name == 'threadPool'
        reactorConfiguration.dispatcherConfigurations[0].type == DispatcherType.THREAD_POOL_EXECUTOR
        reactorConfiguration.dispatcherConfigurations[0].size == 5
        reactorConfiguration.dispatcherConfigurations[0].backlog == 1024
    }
}
