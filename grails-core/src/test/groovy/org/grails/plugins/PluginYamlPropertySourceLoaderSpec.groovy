package org.grails.plugins

import grails.boot.GrailsApp
import io.micronaut.spring.context.env.MicronautEnvironment
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

class PluginYamlPropertySourceLoaderSpec extends Specification {

    void setup() {
        GrailsPluginConfigurationClass.GROOVY_EXISTS = false
        GrailsPluginConfigurationClass.YAML_EXISTS = true
    }

    void "test load plugin.yml configuration from the plugin to the Micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()

        expect:
        ((MicronautEnvironment) context.parent.getEnvironment()).getProperty("bar", String.class) == 'foo'
    }

    void "test load plugin.yml configuration from application overrides the one from plugin to the Micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()
        MicronautEnvironment environment = (MicronautEnvironment) context.parent.getEnvironment()

        expect:
        environment.getProperty("bar", String.class) == 'foo'
        environment.getProperty("foo", String.class) == 'foobar'
        environment.getProperty("abc", String.class) == 'xyz'
    }

    void "test load plugin.yml configuration to the Micronaut context base on the Grails plugin loadAfter"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()
        MicronautEnvironment environment = (MicronautEnvironment) context.parent.getEnvironment()

        expect:
        environment.getProperty("bar", String.class) == 'foo'
        environment.getProperty("abc", String.class) == 'xyz'
    }

    void "test that the configuration binding of plugin.yml using @ConfigurationProperties is working inside the micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class, ConfigBindingExampleConfiguration.class)
        ConfigurableApplicationContext context = app.run()
        ConfigBindingExampleProperties properties = context.parent.getBean(ConfigBindingExampleProperties.class)

        expect:
        properties.bar == 'foo'
    }
}
