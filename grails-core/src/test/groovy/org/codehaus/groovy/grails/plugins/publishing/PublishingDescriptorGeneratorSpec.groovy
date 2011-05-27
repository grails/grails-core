package org.codehaus.groovy.grails.plugins.publishing

import org.springframework.core.io.FileSystemResource
import spock.lang.Specification

class PublishingDescriptorGeneratorSpec extends Specification {

    def "Test that valid descriptor is generated for a plugin"() {
        given:
            def generator = new PluginDescriptorGenerator("foo", [
                  new FileSystemResource(new File("grails-app/controllers/FooController.groovy")),
                  new FileSystemResource(new File("grails-app/controllers/bar/BarController.groovy"))])

        when:
            def sw = new StringWriter()
            generator.generatePluginXml([version:1.0, dependsOn:[core:1.0], author:"Bob"], sw)
            def xml = new XmlSlurper().parseText(sw.toString())

        then:
            xml.@name == 'foo'
            xml.@version == '1.0'
            xml.author.text() == 'Bob'
            xml.dependencies.plugin[0].@name == 'core'
            xml.dependencies.plugin[0].@version == '1.0'
            xml.resources.resource[0].text() == 'FooController'
            xml.resources.resource[1].text() == 'bar.BarController'
    }
}
