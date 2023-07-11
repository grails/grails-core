package org.grails.core.cfg

import org.grails.config.PropertySourcesConfig
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertySources
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import spock.lang.Specification

@SuppressWarnings("GrMethodMayBeStatic")
class GroovyConfigPropertySourceLoaderSpec extends Specification {

    void "test loading multiple configuration files"() {
        setup:
        Resource inputStreamWithDsl = new FileSystemResource(getClass().getClassLoader().getResource("test-application.groovy").getFile())
        Resource inputSteamBuiltInVars = new FileSystemResource(getClass().getClassLoader().getResource("builtin-config.groovy").getFile())

        GroovyConfigPropertySourceLoader groovyPropertySourceLoader = new GroovyConfigPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        PropertySources propertySources = new MutablePropertySources()
        propertySources.addFirst(groovyPropertySourceLoader.load("test-application.groovy", inputStreamWithDsl).first())
        propertySources.addFirst(groovyPropertySourceLoader.load("builtin-config.groovy", inputSteamBuiltInVars).first())
        def config = new PropertySourcesConfig(propertySources)

        then:
        config.size() == 7
        config.getProperty("my.local.var", String.class) == "test"
        config.getProperty("userHomeVar", String.class)

    }

}
