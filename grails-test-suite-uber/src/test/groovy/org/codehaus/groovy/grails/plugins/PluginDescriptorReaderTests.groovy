package org.codehaus.groovy.grails.plugins

import grails.util.BuildSettings
import grails.util.PluginBuildSettings

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource

class PluginDescriptorReaderTests extends GroovyTestCase {

    void testReadPluginInfoFromDescriptorAst() {
        def pluginReader = new AstPluginDescriptorReader()

        def plugin = pluginReader.readPluginInfo(new ByteArrayResource('''
import org.codehaus.groovy.grails.plugins.springsecurity.AuthorizeTools
class FooBarGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "1.3"
    def evicts = ['hibernate', 'domainClass']
    def dependsOn = ['hibernate':'1.3', 'domainClass':'1.2']
}
'''.bytes))

        assert "0.1" == plugin.version : "plugin version should have been 0.1"
        assert "foo-bar" == plugin.name : "plugin name should have been 'foo-bar'"
        assert "1.3" == plugin.grailsVersion : "grails version should have been '1.2'"
        assertEquals "The full plugin name should be 'foo-bar-0.1'","foo-bar-0.1" ,plugin.fullName

        assertEquals(['hibernate', 'domainClass'], plugin.evicts)
        assertEquals(['hibernate':'1.3', 'domainClass':'1.2'], plugin.dependsOn)

        assertEquals 5, plugin.properties.size()
    }

    void testReadPluginInfoFromDescriptorXml() {
        def xml = '''\
<plugin name='acegi' version='0.5.2'>
  <author>Tsuyoshi Yamamoto</author>
  <authorEmail>tyama@xmldo.jp</authorEmail>
  <title>Grails Spring Security 2.0 Plugin</title>
  <description>Plugin to use Grails domain class and secure your applications with Spring Security filters.</description>
  <documentation>http://grails.org/plugin/acegi</documentation>
</plugin>
            '''

        def pluginSettings = new PluginBuildSettings(new BuildSettings())
        def pluginDescriptorReader = new XmlPluginDescriptorReader(pluginSettings)

        def info = pluginDescriptorReader.readPluginInfo(new ByteArrayResource(xml.bytes) {
            Resource createRelative(String relativePath) { new ByteArrayResource(xml.bytes) }
        })

        assertEquals "acegi", info.name
        assertEquals "0.5.2", info.version
        assertEquals "Tsuyoshi Yamamoto", info.author
    }
}
