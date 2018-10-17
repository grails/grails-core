package org.grails.config

import org.springframework.boot.env.RandomValuePropertySource
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import spock.lang.Issue
import spock.lang.Specification

class PropertyResolutionSpec extends Specification {

    @Issue('#10340')
    void 'test property evaluation'() {
        given:
        Resource resource1 = new ByteArrayResource('''
---
rabbitmq:
    something1: '${random.int}'
    something2: ${random.int}
    connections:
      - name: main
        host: ${random.int}
        host2: '${random.int}'
        username: guest
        password: guest
'''.bytes, 'test.yml')

        def propertySourceLoader = new YamlPropertySourceLoader()
        def yamlPropertiesSource1 = propertySourceLoader.load('test.yml', resource1)
        MutablePropertySources propertySources = new MutablePropertySources()
        propertySources.addFirst(yamlPropertiesSource1.first())
        propertySources.addFirst(new RandomValuePropertySource())

        def config = new PropertySourcesConfig(propertySources)

        expect:
        !config.getProperty('rabbitmq.something1').contains('random.int')
        config.getProperty('rabbitmq.something1').isNumber()
        !config.getProperty('rabbitmq.something2').contains('random.int')
        config.getProperty('rabbitmq.something2').isNumber()

        and:
        config.getProperty('rabbitmq.connections[0]', Map).get('name') == 'main'
        !config.getProperty('rabbitmq.connections[0]', Map).get('host').contains('random.int')
        config.getProperty('rabbitmq.connections[0]', Map).get('host').isNumber()
        !config.getProperty('rabbitmq.connections[0]', Map).get('host2').contains('random.int')
        config.getProperty('rabbitmq.connections[0]', Map).get('host2').isNumber()
    }
}
