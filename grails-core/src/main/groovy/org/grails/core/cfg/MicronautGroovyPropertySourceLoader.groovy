package org.grails.core.cfg

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import io.micronaut.context.env.AbstractPropertySourceLoader
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.ResourceLoader
import org.grails.config.NavigableMap

import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

/**
 * Loads properties from a Groovy script.
 *
 * @author Puneet Behl
 * @since 4.0.3
 */
@CompileStatic
class MicronautGroovyPropertySourceLoader extends AbstractPropertySourceLoader {

    AtomicBoolean loaded = new AtomicBoolean(false)

    @Override
    int getOrder() {
        return DEFAULT_POSITION
    }

    @Override
    protected void processInput(String name, InputStream input, Map<String, Object> finalMap) throws IOException {
        if (loaded.compareAndSet(false, true)) {
            def env = Environment.current.name
            if (input.available()) {
                ConfigSlurper configSlurper = env ? new ConfigSlurper(env) : new ConfigSlurper()
                configSlurper.setBinding(
                        userHome: System.getProperty('user.home'),
                        grailsHome: BuildSettings.GRAILS_HOME?.absolutePath,
                        appName: Metadata.getCurrent().getApplicationName(),
                        appVersion: Metadata.getCurrent().getApplicationVersion())
                try {
                    def configObject = configSlurper.parse(input.getText("UTF-8"))
                    convertKeysToKebab(configObject)
                    def propertySource = new NavigableMap()
                    propertySource.merge(configObject, false)
                    finalMap.putAll(propertySource)
                } catch (Throwable e) {
                    throw new ConfigurationException("Exception occurred reading configuration [" + name + "]: " + e.getMessage(), e)
                }
            }

        }
    }

    @Override
    protected Optional<InputStream> readInput(ResourceLoader resourceLoader, String fileName) {
        Stream<URL> urls = resourceLoader.getResources(fileName)
        Stream<URL> urlStream = urls.filter({url -> !url.getPath().contains("src/main/groovy")})
        Optional<URL> config = urlStream.findFirst()
        if (config.isPresent()) {
            return config.flatMap( {url ->
                try {
                    return Optional.of(url.openStream())
                } catch (IOException e) {
                    throw new ConfigurationException("Exception occurred reading configuration [" + fileName + "]: " + e.getMessage(), e)
                }
            })
        }
        return Optional.empty()
    }

    @Override
    Set<String> getExtensions() {
        return Collections.singleton("groovy")
    }

    private void convertKeysToKebab(ConfigObject configObject) {
        def camelToKebabCase = {String text ->
            text.replaceAll(/([A-Z])/, /-$1/).toLowerCase().replaceAll(/^-/, '')
        }
        List<String> keys = []
        for (key in configObject.keySet()) {
            if (key.getClass() == String && configObject[key]) {
                if (configObject[key].getClass() == ConfigObject) {
                    convertKeysToKebab(configObject[key] as ConfigObject)
                }
                if (camelToKebabCase(key as String) != key) {
                    keys.add(key as String)
                }
            }
        }
        for (key in keys) {
            if (!configObject[camelToKebabCase(key)]) {
                configObject[camelToKebabCase(key)] = configObject[key]
            } else {
                throw new ConfigurationException("Key [" + key + "] defined as both camel and kebab case.")
            }
        }
    }

}
