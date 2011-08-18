package org.codehaus.groovy.grails.compiler.validation

import grails.spring.WebBeanBuilder
import grails.util.GrailsWebUtil
import grails.validation.DefaultASTValidateableHelper

import java.net.URL

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ValidateableTransformerSpec extends Specification {
    
    static gcl

    def setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new ValidateableTransformer()
        gcl.classInjectors = [transformer]as ClassInjector[]
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
