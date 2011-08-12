package org.codehaus.groovy.grails.compiler.web.taglib

import java.net.URL

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader

import spock.lang.Specification

class TagLibraryTransformerSpec extends Specification {

    static myTagLibClass

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new TagLibraryTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        gcl.classInjectors = [transformer]as ClassInjector[]
        myTagLibClass = gcl.parseClass('''
        class MyTagLib {
            def closureTagWithNoExplicitArgs = { }
            def closureTagWithOneArg = { attrs -> }
            def closureTagWithTwoArgs = { attrs, body -> }
            def closureTagWithThreeArgs = { attrs, body, extra -> }
            def closureTagWithFourArgs = { attrs, body, extra, anotherExtr -> }
        }
        ''')
    }

    void 'Test tag method creation'() {
        expect:
           numberOfMethods == myTagLibClass.methods.findAll { methodName == it.name }.size()

         where:
             /*
              * Tag methods are overloaded with these argument combinations:
              *    tagName()
              *    tagName(Map)
              *    tagName(Closure)
              *    tagName(Map, Closure)
              *    tagName(Map, CharSequence)
              */
             numberOfMethods | methodName
             5 | 'closureTagWithNoExplicitArgs'
             5 | 'closureTagWithOneArg'
             5 | 'closureTagWithOneArg'
             5 | 'closureTagWithTwoArgs'
             0 | 'closureTagWithThreeArgs'
             0 | 'closureTagWithFourArgs'
    }
}