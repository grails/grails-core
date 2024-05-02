/*
 * Copyright 2024 original authors
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
package org.grails.plugins

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import io.micronaut.spring.context.env.MicronautEnvironment
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

class PluginGroovyPropertySourceLoaderSpec extends Specification {

    void setup() {
        GrailsPluginConfigurationClass.GROOVY_EXISTS = true
        GrailsPluginConfigurationClass.YAML_EXISTS = false
    }

    void "test load groovy configuration from the plugin to the Micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()

        expect:
        ((MicronautEnvironment) context.parent.getEnvironment()).getProperty("bar", String.class) == 'foo'
    }

    void "test load groovy configuration from application overrides the one from plugin to the Micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()
        MicronautEnvironment environment =  (MicronautEnvironment) context.parent.getEnvironment()

        expect:
        environment.getProperty("bar", String.class) == 'foo'
        environment.getProperty("foo", String.class) == 'foobar'
        environment.getProperty("abc", String.class) == 'xyz'
    }

    void "test load groovy configuration to the Micronaut context base on the Grails plugin loadAfter"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class)
        ConfigurableApplicationContext context = app.run()
        MicronautEnvironment environment =  (MicronautEnvironment) context.parent.getEnvironment()

        expect:
        environment.getProperty("bar", String.class) == 'foo'
        environment.getProperty("abc", String.class) == 'xyz'
    }

    void "test that the configuration binding of plugin.groovy using @ConfigurationProperties is working inside the micronaut context"() {

        given:
        GrailsApp app = new GrailsApp(GrailsPluginConfigurationClass.class, GrailsAutoConfiguration.class, ConfigBindingExampleConfiguration.class)
        ConfigurableApplicationContext context = app.run()
        ConfigBindingExampleProperties properties = context.parent.getBean(ConfigBindingExampleProperties.class)

        expect:
        properties.bar == 'foo'
    }
}

