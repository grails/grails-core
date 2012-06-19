package org.codehaus.groovy.grails.compiler.validation

import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader

import spock.lang.Specification

class ValidateableTransformationSpec extends Specification {

    static gcl

    def setupSpec() {
        gcl = new GrailsAwareClassLoader()
    }

    void 'Test validate methods added to classes marked with grails.validation.Validateable'() {
        given:
            def clz = gcl.parseClass('''
            @grails.validation.Validateable
            class Widget {
            }
            ''')

        when:
            def validateMethodCount = clz.metaClass.methods.findAll { it.name == 'validate' }?.size()

        then:
            2 == validateMethodCount
    }

    void 'Test validate methods added to classes marked with org.codehaus.groovy.grails.validation.Validateable'() {
        given:
            def clz = gcl.parseClass('''
            @org.codehaus.groovy.grails.validation.Validateable
            class Widget {
            }
            ''')

        when:
            def validateMethodCount = clz.metaClass.methods.findAll { it.name == 'validate' }?.size()

        then:
            2 == validateMethodCount
    }

    void 'Test validate methods not added to classes which are not marked with Validateable'() {
        given:
            def clz = gcl.parseClass('''
            class Widget {
            }
            ''')

        when:
            def validateMethodCount = clz.metaClass.methods.findAll { it.name == 'validate' }?.size()

        then:
            0 == validateMethodCount
    }
}
