package org.grails.compiler.injection

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

/**
 * Created by graemerocher on 19/09/14.
 */
class GlobalGrailsClassInjectorTransformationSpec extends Specification {

    void "Test that a correct plugin dot xml file is generated when the plugin dot xml doesn't exist"() {
        given:"A file that doesn't yet exist"
            File pluginXml = new File(System.getProperty("java.io.tmpdir"), "plugin-xml-gen-test.test.xml")
            pluginXml.delete()
            ClassNode classNode = null
            CompilationUnit cu = new CompilationUnit(new GroovyClassLoader())
            cu.addSource("FooGrailsPlugin", '''
class FooGrailsPlugin {
}
''')
            cu.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
                @Override
                void call(SourceUnit source, GeneratorContext context, ClassNode cn) throws CompilationFailedException {
                    if(cn.name.endsWith("GrailsPlugin")) {
                         classNode = cn
                    }
                }
            },Phases.CONVERSION)
            cu.compile(Phases.CONVERSION)


        expect:"the file doesn't exist"
            !pluginXml.exists()

        when:"the transformation generates the plugin.xml"
            def transformation = new GlobalGrailsClassInjectorTransformation()
            transformation.generatePluginXml(classNode,"1.0", ['Foo'] as Set, pluginXml)

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

    void "Test that a correct plugin dot xml file is updated when the plugin dot xml does exist"() {
        given:"A file that doesn't yet exist"
            File pluginXml = File.createTempFile("plugin-xml-gen", "test.xml")
            ClassNode classNode = null
            CompilationUnit cu = new CompilationUnit(new GroovyClassLoader())
            cu.addSource("BarGrailsPlugin", '''
    class BarGrailsPlugin {
    }
    ''')
            cu.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
                @Override
                void call(SourceUnit source, GeneratorContext context, ClassNode cn) throws CompilationFailedException {
                    if(cn.name.endsWith("GrailsPlugin")) {
                        classNode = cn
                    }
                }
            },Phases.CONVERSION)
            cu.compile(Phases.CONVERSION)
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
            transformation.generatePluginXml(classNode, "1.0", ['Foo', "Bar"] as Set, pluginXml)

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
