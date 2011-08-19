package grails.validation;

import static org.junit.Assert.*
import grails.spring.WebBeanBuilder
import grails.util.GrailsWebUtil

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

/**
 * Tests relevant to grails.validation.Validateable
 *
 */
class DefaultASTValidateableHelperSpec extends Specification {

    static widgetClass

    def setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ClassInjector() {
                    @Override
                    void performInjection(SourceUnit source, ClassNode classNode) {
                        performInject(source, null, classNode)
                    }
                    @Override
                    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
                        new DefaultASTValidateableHelper().injectValidateableCode(classNode)
                    }
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        gcl.classInjectors = [transformer]as ClassInjector[]
        widgetClass = gcl.parseClass('''
        class Widget {
            String name
            String category
            Integer count
            
            static constraints = {
                name matches: /[A-Z].*/
                category size: 3..50
                count range: 1..10
            }
        }
        ''')
        def bb = new WebBeanBuilder()
        bb.beans {
            grailsApplication(DefaultGrailsApplication)
            "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator) {
            }
        }
        def applicationContext = bb.createApplicationContext()

        GrailsWebUtil.bindMockWebRequest(applicationContext)
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void 'Test validate method on uninitialized object'() {
        given:
            def widget = widgetClass.newInstance()

        when:
            def isValid = widget.validate()

        then:
            !isValid
    }

    void 'Test validate method on invalid object'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate()
            def errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount
    }

    void 'Test clearErrors'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate()
            def errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount

        when:
            widget.clearErrors()
            errorCount = widget.errors.errorCount

        then:
            0 == errorCount

        when:
            isValid = widget.validate()
            errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount
    }

    void 'Test revalidating invalid object'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate()
            def errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount

        when:
            isValid = widget.validate()
            errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount
    }

    void 'Test revalidating object after fixing errors'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate()
            def errorCount = widget.errors.errorCount

        then:
            !isValid
            3 == errorCount

        when:
            widget.name = 'Joe'
            widget.count = 9
            widget.category = 'Playa'
            isValid = widget.validate()
            errorCount = widget.errors.errorCount

        then:
            isValid
            0 == errorCount
    }

    void 'Test validate method on valid object'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'Joe'
            widget.count = 9
            widget.category = 'Playa'

        when:
            def isValid = widget.validate()
            def errorCount = widget.errors.errorCount

        then:
            isValid
            0 == errorCount
    }

    void 'Test validate method with a List argument'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate(['count', 'category'])
            def errorCount = widget.errors.errorCount
            def countError = widget.errors.getFieldError('count')
            def nameError = widget.errors.getFieldError('name')
            def categoryError = widget.errors.getFieldError('category')

        then:
            !isValid
            2 == errorCount
            countError
            categoryError
            !nameError
    }

    void 'Test validate method with an empty List argument'() {
        given:
            def widget = widgetClass.newInstance()
            widget.name = 'joe'
            widget.count = 42

        when:
            def isValid = widget.validate([])
            def errorCount = widget.errors.errorCount
            
        then:
            isValid
            0 == errorCount
    }
}
