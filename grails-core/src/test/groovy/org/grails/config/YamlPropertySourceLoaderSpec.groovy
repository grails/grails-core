package org.grails.config

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.env.Environment
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import spock.lang.Specification

class YamlPropertySourceLoaderSpec extends Specification {

    def "ensure the config for environment is merged with single environment block"() {
        given: "A PropertySourcesConfig instance"
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource("foo-plugin-environments.yml").getFile())

        when:
        def yamlPropertiesSource = propertySource.load('foo-plugin-environments.yml', resource, null, true, Arrays.asList("dataSource", "hibernate"))
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        then: "The config to be accessible with the merged env values"
        config.one == 2
        config.two == 3
        config.three.four == 45
        !config.four.five
        config.getProperty('one', String) == '2'
        config.getProperty('three.four', String) == '45'
        config.getProperty('three', String) == null
        config.get('three.four') == 45
        config.getProperty('three.four') == '45'
        config.getProperty('three.four', Date) == null
        config.empty.value == 'development'
        !config.dataSource
        !config.getProperty('dataSource')
        !config.get('dataSource')
    }

    def "ensure the config for environment is merged with single environment block with parseFlatMap false"() {
        given: "A PropertySourcesConfig instance"
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource("foo-plugin-environments.yml").getFile())

        when:
        def yamlPropertiesSource = propertySource.load('foo-plugin-environments.yml', resource, null, false, Arrays.asList("dataSource", "hibernate"))
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        then: "These will not be navigable due to false parseFlatKeys"
        !config.three.four
        !config.empty.value

        and: "The config to be accessible with the merged env values"
        config.one == 2
        config.two == 3
        !config.four.five
        config.getProperty('one', String) == '2'
        config.getProperty('three.four', String) == '45'
        config.getProperty('three', String) == null
        config.get('three.four') == 45
        config.getProperty('three.four') == '45'
        config.getProperty('three.four', Date) == null
        !config.dataSource
        !config.getProperty('dataSource')
        !config.get('dataSource')
    }

    def "ensure the config for environment is merged with multiple environment block"() {
        given: "A PropertySourcesConfig instance"
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource("foo-plugin-multiple-environments.yml").getFile())

        when:
        def yamlPropertiesSource = propertySource.load('foo-plugin-multiple-environments.yml', resource, null, true, Arrays.asList("dataSource", "hibernate"))
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        then: "The config to be accessible with the merged env values"
        config.one == -2
        config.two == 3
        config.three.four == 45
        config.four.five == 45
        config.getProperty('one', String) == '-2'
        config.getProperty('three.four', String) == '45'
        config.getProperty('three', String) == null
        config.get('three.four') == 45
        config.get('four.five') == 45
        config.getProperty('three.four') == '45'
        config.getProperty('three.four', Date) == null
        config.getProperty('four.five') == '45'
        config.empty.value == 'development'
        !config.dataSource
        !config.getProperty('dataSource')
        !config.get('dataSource')
    }
}
