package org.codehaus.groovy.grails.plugins

import grails.core.DefaultGrailsApplication
import org.grails.plugins.BinaryGrailsPlugin
import org.grails.plugins.BinaryGrailsPluginDescriptor
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
            def descriptor = new BinaryGrailsPluginDescriptor(new ByteArrayResource(str.getBytes('UTF-8')), ['org.codehaus.groovy.grails.plugins.TestBinaryResource'])
            def binaryPlugin = new BinaryGrailsPlugin(TestBinaryGrailsPlugin, descriptor, new DefaultGrailsApplication())

        then:
            binaryPlugin.version == "1.0"
            binaryPlugin.providedArtefacts.size() == 1
            binaryPlugin.providedArtefacts[0] == TestBinaryResource
            binaryPlugin.binaryDescriptor != null
    }


    def "Test load static resource from binary plugin"() {
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
            def resource = new MockBinaryPluginResource(str.getBytes('UTF-8'))
            def descriptor = new BinaryGrailsPluginDescriptor(resource, ['org.codehaus.groovy.grails.plugins.TestBinaryResource'])
            resource.relativesResources['static/css/main.css'] = new ByteArrayResource(''.bytes)
            def binaryPlugin = new BinaryGrailsPlugin(TestBinaryGrailsPlugin, descriptor, new DefaultGrailsApplication())
            def cssResource = binaryPlugin.getResource("/css/main.css")

        then:
            cssResource != null
        when:
            cssResource = binaryPlugin.resolveView("/css/foo.css")

        then:
            cssResource == null
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
