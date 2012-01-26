package org.codehaus.groovy.grails.compiler.web

import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.FailsWith
import spock.lang.Specification

class ControllerActionTransformerCompilationErrorsSpec extends Specification {

    static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        def transformer2 = new ControllerTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        gcl.classInjectors = [transformer, transformer2]as ClassInjector[]
    }

    void 'Test overloaded method actions'() {
        when: 'A controller overloads a method action'
            gcl.parseClass('''
            class TestController {
                def methodAction(String s){}
                def methodAction(Integer i){}
            }
''')
        then:
            MultipleCompilationErrorsException e = thrown()
            e.message.contains 'Controller actions may not be overloaded.  The [methodAction] action has been overloaded in [TestController].'
    }

    void "Test default parameter values"() {
        when: 'A method action has default parameter values'
            gcl.parseClass('''
            class TestController {
                def methodAction(int i = 42){}
            }
            ''')

            then:
            MultipleCompilationErrorsException e = thrown()
            e.message.contains 'Parameter [i] to method [methodAction] has default value [42].  Default parameter values are not allowed in controller action methods.'
    }


    void "Test a non-validateable command object"() {
        when:
            gcl.parseClass('''
            class TestController {
                def methodAction(org.codehaus.groovy.grails.compiler.web.SomeClass sc){}
            }
            ''')

        then:
            MultipleCompilationErrorsException e = thrown()
            e.message.contains 'The [methodAction] action accepts a parameter of type [org.codehaus.groovy.grails.compiler.web.SomeClass] which does not appear to be a command object class.'

        when:
            gcl.parseClass('''
            class TestController {
                def closureAction = {org.codehaus.groovy.grails.compiler.web.SomeClass sc->}
            }
            ''')

        then:
            e = thrown()
            e.message.contains 'The [closureAction] action accepts a parameter of type [org.codehaus.groovy.grails.compiler.web.SomeClass] which does not appear to be a command object class.'
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

/**
 * This class needs to be defined out here and not loaded/compiled by the gcl.  The idea is that
 * this class is not to be made validateable at compile time.  This simulates a class defined in a plugin
 * and then used as a command object in the application.
 */
class SomeClass {
}

