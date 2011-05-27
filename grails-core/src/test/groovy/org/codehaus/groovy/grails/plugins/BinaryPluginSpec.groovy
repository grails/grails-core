package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import spock.lang.Specification

class BinaryPluginSpec extends Specification {

    def "Test creation of a binary plugin"() {
        given:
            def str = '''
    <plugin name='testBinary'>
      <class>org.codehaus.groovy.grails.plugins.TestBinaryGrailsPlugin</class>
      <resources>
             <resource>org.codehaus.groovy.grails.plugins.TestBinaryResource</resource>
      </resources>
    </plugin>
    '''

            def xml = new XmlSlurper().parseText(str)

        when:
            def descriptor = new BinaryGrailsPluginDescriptor(new ByteArrayResource(str.bytes), xml)
            def binaryPlugin = new BinaryGrailsPlugin(TestBinaryGrailsPlugin, descriptor, new DefaultGrailsApplication())

        then:
            binaryPlugin.version == "1.0"
            binaryPlugin.providedArtefacts.size() == 1
            binaryPlugin.providedArtefacts[0] == TestBinaryResource
            binaryPlugin.binaryDescriptor != null
    }

    def "Test loading properties from a binary plugin"() {
        given:
            def str = '''
    <plugin name='testBinary'>
      <class>org.codehaus.groovy.grails.plugins.TestBinaryGrailsPlugin</class>
      <resources>
             <resource>org.codehaus.groovy.grails.plugins.TestBinaryResource</resource>
      </resources>
    </plugin>
    '''

            def xml = new XmlSlurper().parseText(str)


        when:
            def resource = new MockBinaryPluginResource(str.bytes)
            def descriptor = new BinaryGrailsPluginDescriptor(resource, xml)
            resource.relativesResources['views.properties'] = new ByteArrayResource('''
/WEB-INF/grails-app/views/bar/list.gsp=org.codehaus.groovy.grails.plugins.MyView
'''.bytes)
            resource.relativesResources['grails-app/i18n'] = new ByteArrayResource(''.bytes)
            resource.relativesResources['grails-app/i18n/messages.properties'] = new ByteArrayResource('''
foo.bar=one
'''.bytes)
            def binaryPlugin = new BinaryGrailsPlugin(TestBinaryGrailsPlugin, descriptor, new DefaultGrailsApplication())
            def properties = binaryPlugin.getProperties(Locale.getDefault())

        then:
            properties.isEmpty() == false
            properties['foo.bar'] == 'one'

        when:
            def viewClass = binaryPlugin.resolveView("/WEB-INF/grails-app/views/bar/list.gsp")

        then:
            viewClass != null
            viewClass == MyView


    }
}
class TestBinaryGrailsPlugin {
    def version = 1.0
}
class TestBinaryResource {}

class MockBinaryPluginResource extends ByteArrayResource {

    Map<String, Resource> relativesResources = [:]

    MockBinaryPluginResource(byte[] byteArray) {
        super(byteArray)
    }

    @Override
    Resource createRelative(String relativePath) {
        return relativesResources[relativePath]
    }


}
class MyView extends Script {
    @Override
    Object run() {
        return "Good"
    }

}