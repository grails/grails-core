package org.grails.compiler.injection

import groovy.xml.MarkupBuilder
import spock.lang.Specification

/**
 * Created by graemerocher on 19/09/14.
 */
class GlobalGrailsClassInjectorTransformationSpec extends Specification {

    void "Test that a correct plugin.xml file is generated when the plugin.xml doesn't exist"() {
        given:"A file that doesn't yet exist"
            File pluginXml = new File(System.getProperty("java.io.tmpdir"), "plugin-xml-gen-test.test.xml")
            pluginXml.delete()

        expect:"the file doesn't exist"
            !pluginXml.exists()

        when:"the transformation generates the plugin.xml"
            def transformation = new GlobalGrailsClassInjectorTransformation()
            transformation.generatePluginXml("FooGrailsPlugin", ['Foo'] as Set, pluginXml)

        then:"the file exists"
            pluginXml.exists()

        when:"the xml is parsed"
            def xml = new XmlSlurper().parse(pluginXml)

        then:"The generated plugin.xml is valid"
            xml.@name.text() == "foo"
            xml.type.text() == "FooGrailsPlugin"
            xml.resources.size() == 1
            xml.resources.resource.text() == "Foo"
    }

    void "Test that a correct plugin.xml file is updated when the plugin.xml does exist"() {
        given:"A file that doesn't yet exist"
            File pluginXml = File.createTempFile("plugin-xml-gen", "test.xml")
            pluginXml.withWriter { writer ->

                def mkp = new MarkupBuilder(writer)
                mkp.plugin(name:"foo") {
                    type "FooGrailsPlugin"
                    resources {
                        resource "Foo"
                        resource "Bar"
                    }
                }
            }
        expect:"the file does exist"
            pluginXml.exists()

        when:"the transformation generates the plugin.xml"
            def transformation = new GlobalGrailsClassInjectorTransformation()
            transformation.generatePluginXml("BarGrailsPlugin", ['Foo', "Bar"] as Set, pluginXml)

        then:"the file exists"
            pluginXml.exists()

        when:"the xml is parsed"
            def xml = new XmlSlurper().parse(pluginXml)

        then:"The generated plugin.xml is valid"
            xml.@name.text() == "bar"
            xml.type.text() == "BarGrailsPlugin"
            xml.resources.resource.size() == 2
            xml.resources.resource.text() == "FooBar"
    }
}
