package org.grails.core.cfg

import groovy.transform.CompileStatic
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource

//TODO: Comment out {@code CompileStatic} Look into the Static type checking error
//@CompileStatic
class PluginGroovyPropertySourceLoader extends MicronautGroovyPropertySourceLoader {

    protected static final String DEFAULT_FILE_NAME = 'plugin'

    @Override
    Optional<PropertySource> load(Environment environment) {
        load(DEFAULT_FILE_NAME, environment)
    }
}
