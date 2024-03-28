package grails.config

import org.grails.config.NavigableMap
import org.grails.config.PropertySourcesConfig
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import spock.lang.Specification

/**
 *
 */
class PropertySourceConfigSpec extends Specification {

    void "specifying targetType as Map in getProperty method should not return NavigableMap"() {
        given:
        Map input = ['mongodb.username': 'test',
                     'mongodb.password': 'foo']

        when:
        PropertySourcesConfig config = new PropertySourcesConfig()
        config.merge(input)
        Map value = config.getProperty("mongodb", Map.class)

        then:
        !(value instanceof NavigableMap)
    }

    void "specifying targetType as Map in getProperty method should not return NavigableMap for List of Map value"() {
        given:
        Map input = ['grails.mongodb.connections[0].username': 'test',
                     'grails.mongodb.connections[0].password': 'foo']

        when:
        PropertySourcesConfig config = new PropertySourcesConfig()
        config.merge(input)

        then:
        !(config.getProperty("grails.mongodb.connections[0]", Map.class) instanceof NavigableMap)
        !(config.getProperty("grails.mongodb.connections", List.class).get(0) instanceof NavigableMap)
    }


    void "should merge sub-documents in yaml file to single config"() {

        given:
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource('config/application.yml').getFile())
        def yamlPropertiesSource = propertySource.load('application.yml', resource)
        PropertySourcesConfig config = new PropertySourcesConfig(yamlPropertiesSource.first())

        expect:
        config.a1 == [a2: 3, b2: 4, c2: [a3: 3, b2: 4, c3: 1], d2: 1, e2: 2]
        config.grails.profile == 'web'
        config.grails.containsKey('somekey') == true
    }

    void "Should support conversion from null to other objects"() {

        given:
        PropertySourcesConfig config = new PropertySourcesConfig()

        when:
        config.put('foo.bar', null)

        then:
        config.getProperty("foo.bar", Map.class) == null

    }

    void "should support merging maps"() {

        given:
        PropertySourcesConfig config = new PropertySourcesConfig()

        when:
        config.merge([a: 1])

        then:
        config.a == 1

        when:
        config.merge([b: 2])

        then:
        config.a == 1
        config.b == 2

        when:
        config.merge([a: [c: 1]])

        then:
        config.a == [c: 1]
        config.b == 2
        config.'a.c' == 1

        when:
        config.merge([a: [d: 1]])

        then:
        config.a == [c: 1, d: 1]
        config.b == 2
        config.'a.c' == 1
        config.'a.d' == 1

        when:
        config.merge([a: [c: 2]])

        then:
        config.a == [c: 2, d: 1]
        config.b == 2
        config.'a.c' == 2
        config.'a.d' == 1
    }

    void "should support casting to boolean"() {

        given:
        PropertySourcesConfig config = new PropertySourcesConfig()

        expect:
        config as boolean == false

        when:
        config.a = 1

        then:
        config as boolean == false
    }
}
