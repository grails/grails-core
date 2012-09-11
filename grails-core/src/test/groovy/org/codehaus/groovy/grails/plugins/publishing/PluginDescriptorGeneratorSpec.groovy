package org.codehaus.groovy.grails.plugins.publishing

import org.springframework.core.io.FileSystemResource
import spock.lang.Specification
import grails.util.BuildSettings

class PluginDescriptorGeneratorSpec extends Specification {

    def "Test that valid descriptor is generated for a plugin"() {
        given:
            def generator = new PluginDescriptorGenerator(new BuildSettings(),"foo", [
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
            xml.runtimePluginRequirements.plugin[0].@name == 'core'
            xml.runtimePluginRequirements.plugin[0].@version == '1.0'
            xml.resources.resource[0].text() == 'FooController'
            xml.resources.resource[1].text() == 'bar.BarController'
    }

    def "Test plugin excludes causes no problems when no resources"() {
        given:
            def generator = new PluginDescriptorGenerator(new BuildSettings(),"foo", [])
        when:
            def sw = new StringWriter()
            generator.generatePluginXml([version:1.0, dependsOn:[core:1.0], author:"Bob", pluginDir: new FileSystemResource(new File(".")), pluginExcludes: ["**/test/**"]], sw)
            def xml = new XmlSlurper().parseText(sw.toString())
        then:
            xml.@name == 'foo'
            xml.@version == '1.0'
            xml.author.text() == 'Bob'
            xml.runtimePluginRequirements.plugin[0].@name == 'core'
            xml.runtimePluginRequirements.plugin[0].@version == '1.0'
            xml.resources.resource.size() == 0
    }

    def "Test plugin/excludes is honoured for resources"() {
        given:
            def generator = new PluginDescriptorGenerator(new BuildSettings(),"foo",  [
                new FileSystemResource(new File("grails-app/controllers/FooController.groovy")),
                new FileSystemResource(new File("grails-app/controllers/test/BarController.groovy")),
                new FileSystemResource(new File("grails-app/services/test/MyService.groovy")),
                new FileSystemResource(new File("grails-app/services/MyService2.groovy"))
            ])
        when:
            def sw = new StringWriter()
            generator.generatePluginXml([version:1.0, dependsOn:[core:1.0], author:"Bob", pluginDir: new FileSystemResource(new File(".")), pluginExcludes: ["**/test/**"]], sw)
            def xml = new XmlSlurper().parseText(sw.toString())
        then:
            xml.@name == 'foo'
            xml.@version == '1.0'
            xml.author.text() == 'Bob'
            xml.runtimePluginRequirements.plugin[0].@name == 'core'
            xml.runtimePluginRequirements.plugin[0].@version == '1.0'
            xml.resources.resource.size() == 2
            xml.resources.resource[0].text() == 'FooController'
            xml.resources.resource[1].text() == 'MyService2'

    }

    void "Test that dependencies and repositories are correctly populated from BuildSettings"() {
        given:"A plugin descriptor generator with a BuildSettings instance that defines repositories and dependencies"
            PluginDescriptorGenerator generator = systemUnderTest()

        when:"The XML is generated"
            def sw = new StringWriter()
            generator.generatePluginXml([version:1.0], sw)
            def xml = new XmlSlurper().parseText(sw.toString())

        then:"The dependencies and repositories are correctly populated"
            xml.@name == "test"
            xml.@version == "1.0"
            xml.repositories.repository[0].@url == "http://myrepo.com/maven/"
            xml.dependencies.compile.dependency[0].@group == 'com.mysql'
            xml.dependencies.compile.dependency[0].@name == 'driver'
            xml.dependencies.compile.dependency[0].@version == '1.0'
    }

    PluginDescriptorGenerator systemUnderTest() {
        final settings = new BuildSettings()
        def config = new ConfigObject()
        config.grails.project.dependency.resolution = {
            repositories {
              mavenRepo "http://myrepo.com/maven"
            }
            dependencies {
                compile "com.mysql:driver:1.0"
            }
        }
        settings.loadConfig(config)
        return new PluginDescriptorGenerator(settings, "test", [])
    }
}
