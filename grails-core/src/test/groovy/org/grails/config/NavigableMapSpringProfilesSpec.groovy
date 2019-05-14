package org.grails.config

import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import spock.lang.Specification

class NavigableMapSpringProfilesSpec extends Specification {

    void 'test spring profiles configuration for default config'() {
        given:
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource('application.yml').getFile())
        def yamlPropertiesSource = propertySource.load('application.yml', resource, null)
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        expect:
        config.getProperty('hello.message') == 'Default hello!'
    }

    void 'test spring profiles configuration for "sample" profile'() {
        given:
        System.setProperty('spring.profiles.active', 'sample')

        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource('application.yml').getFile())
        def yamlPropertiesSource = propertySource.load('application.yml', resource, 'sample')
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        expect:
        config.getProperty('hello.message') == 'Hello from SAMPLE profile!'
    }

    void 'test spring profiles configuration for "demo" profile'() {
        given:
        System.setProperty('spring.profiles.active', 'demo')

        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource('application.yml').getFile())
        def yamlPropertiesSource = propertySource.load('application.yml', resource, 'demo')
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        expect:
        config.getProperty('hello.message') == 'Hello from DEMO profile!'
    }

    void 'test spring profiles property resolution for default config'() {
        given:
        def propertySource = new YamlPropertySourceLoader()
        Resource resource = new FileSystemResource(getClass().getClassLoader().getResource('application.yml').getFile())
        def yamlPropertiesSource = propertySource.load('application.yml', resource, null)
        def config = new PropertySourcesConfig(yamlPropertiesSource)

        expect:
        config.getProperty('hello.evaluatedName') == 'Hello, my name is'
    }
}
