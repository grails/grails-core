package org.grails.compiler.web.taglib

import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.web.taglib.TagLibraryTransformer
import spock.lang.Issue
import spock.lang.Specification

class TagLibraryTransformerSpec extends Specification {

    static myTagLibClass

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new TagLibraryTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer] as ClassInjector[]
        myTagLibClass = gcl.parseClass('''
        class MyTagLib {
            def closureTagWithNoExplicitArgs = { }
            def closureTagWithOneArg = { attrs -> }
            def closureTagWithTwoArgs = { attrs, body -> }
            def closureTagWithThreeArgs = { attrs, body, extra -> }
            def closureTagWithFourArgs = { attrs, body, extra, anotherExtra -> }
        }
        ''')
    }

    void 'Test tag methods are created for properties which are tags'() {
        expect:
             /*
              * Tag methods are overloaded with these argument combinations:
              *    tagName()
              *    tagName(Map)
              *    tagName(Closure)
              *    tagName(Map, Closure)
              *    tagName(Map, CharSequence)
              */
           5 == myTagLibClass.methods.findAll { methodName == it.name }.size()

         where:
             methodName << ['closureTagWithNoExplicitArgs', 'closureTagWithOneArg', 'closureTagWithTwoArgs']
    }

    void 'Test tag methods are not created for properties which are not tags'() {
        expect:
           0 == myTagLibClass.methods.findAll { methodName == it.name }.size()

         where:
             methodName << ['closureTagWithThreeArgs', 'closureTagWithFourArgs']
    }
    
    @Issue('GRAILS-11241')
    void 'Test that a tag libary can be marked with @CompileStatic without generating compile errors'() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def transformer = new TagLibraryTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer] as ClassInjector[]

        expect:
        gcl.parseClass('''
        @groovy.transform.CompileStatic
        class MyTagLib {
            def closureTagWithNoExplicitArgs = { }
            def closureTagWithOneArg = { attrs -> }
            def closureTagWithTwoArgs = { attrs, body -> }
        }
        ''')
    }
}
