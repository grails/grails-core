package org.grails.compiler.injection

import grails.plugins.GrailsPluginInfo
import groovy.xml.MarkupBuilder
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

/**
 * @author Michael Yan
 * @since 5.1.8
 */
class PluginAstReaderSpec extends Specification {

    void "Test read the plugin info from classNode"() {
        given:"A Grails Plugin class"
            ClassNode classNode = null
            CompilationUnit cu = new CompilationUnit(new GroovyClassLoader())
            cu.addSource("FooGrailsPlugin", '''
import grails.util.GrailsUtil

class FooGrailsPlugin {
    def version = '0.1'
    def dependsOn = [core: version]
}
''')
            cu.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
                @Override
                void call(SourceUnit source, GeneratorContext context, ClassNode cn) throws CompilationFailedException {
                    if(cn.name == "FooGrailsPlugin") {
                         classNode = cn
                    }
                }
            }, Phases.CANONICALIZATION)
            cu.compile(Phases.CANONICALIZATION)

        when:"read info of the plugin classNode"
            def reader = new PluginAstReader()
            GrailsPluginInfo info = reader.readPluginInfo(classNode)

        then:"The info and properties are correct"
            info.name == "foo"
            info.version == "0.1"
            info.properties.dependsOn == [core: "0.1"]
    }
}