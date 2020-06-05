package org.grails.core.cfg

import groovy.transform.CompileStatic
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.context.env.yaml.YamlPropertySourceLoader

import static org.grails.core.cfg.PluginGroovyPropertySourceLoader.DEFAULT_FILE_NAME

//TODO: Comment out {@code CompileStatic} Look into the Static type checking error
//@CompileStatic
class PluginYamlPropertySourceLoader extends YamlPropertySourceLoader {

    @Override
    Optional<PropertySource> load(Environment environment) {
        load(DEFAULT_FILE_NAME, environment)
    }
}
