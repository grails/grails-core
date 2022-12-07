package org.grails.core.cfg

import grails.plugins.GrailsPlugin
import grails.util.Environment
import groovy.transform.CompileStatic
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.env.yaml.YamlPropertySourceLoader

/**
 * Load properties from a YAML file. This class extends the Micronaut default implementation {@link io.micronaut.context.env.yaml.YamlPropertySourceLoader} where it adds support for Micronaut beans receive configuration from environments block.
 *
 * @author Puneet Behl
 * @since 6.0.0
 */
@CompileStatic
class MicronautYamlPropertySourceLoader extends YamlPropertySourceLoader {

    @Override
    int getOrder() {
        return super.getOrder() + 1
    }

    @Override
    protected MapPropertySource createPropertySource(String name, Map<String, Object> map, int order) {
        return super.createPropertySource("grails.$name", map, order)
    }

    /**
     * Only process environment specific entries as the other entries are already processed by Micronaut default {@link io.micronaut.context.env.yaml.YamlPropertySourceLoader}
     *
     * @param finalMap The map with all the properties processed
     * @param map The map to process
     * @param prefix The prefix for the keys
     */
    @Override
    protected void processMap(Map<String, Object> finalMap, Map map, String prefix) {
        final String env = Environment.current.name
        if (env != null) {
            final String grailsEnvironmentPrefix = GrailsPlugin.ENVIRONMENTS + '.' + env + '.'
            ((Map<String, Object>) map).entrySet().stream()
                    .filter(e -> e.key instanceof String && (((String) e.key).startsWith(GrailsPlugin.ENVIRONMENTS) || prefix.startsWith(GrailsPlugin.ENVIRONMENTS)))
                    .forEach(e -> {
                        Map.Entry entry = (Map.Entry) e
                        String key = entry.getKey().toString()
                        Object value = entry.getValue()
                        if (value instanceof Map && !((Map) value).isEmpty()) {
                            processMap(finalMap, (Map) value, prefix + key + '.')
                        } else if ((prefix + key).startsWith(grailsEnvironmentPrefix)) {
                            finalMap.put((prefix + key).substring(grailsEnvironmentPrefix.length()), value)
                        }
                    })
        }
    }
}
