package org.grails.config

import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Iván López
 */
class SystemEnvironmentConfigSpec extends Specification {

    @Issue('#10670')
    void 'configuration properties defined in SystemEnvironment take precedence'() {
        given:
        Resource resource1 = new ByteArrayResource('''
property.with.period: from-yml
property_with_underscore: from-yml
'''.bytes, 'test.yml')

        def propertySourceLoader = new YamlPropertySourceLoader()
        def yamlPropertiesSource = propertySourceLoader.load('application.yml', resource1, null)

        def propertySources = new MutablePropertySources()
        propertySources.addFirst(yamlPropertiesSource)
        propertySources.addFirst(new SystemEnvironmentPropertySource('systemEnvironment', systemEnvConfig))
        def config = new PropertySourcesConfig(propertySources)

        expect:
        config.getProperty('property.with.period') == 'from-env'
        config.getProperty('property_with_underscore') == 'from-env'

        where:
        systemEnvConfig << [
            [property_with_period: 'from-env', property_with_underscore: 'from-env'],
            [PROPERTY_WITH_PERIOD: 'from-env', PROPERTY_WITH_UNDERSCORE: 'from-env'],
        ]
    }
}
