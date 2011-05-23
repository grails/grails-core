package org.codehaus.groovy.grails.compiler.web

import grails.spring.WebBeanBuilder
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification
import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi

/**
 * Tests for ControllerDomainTransformer
 */
class ControllerDomainTransformerSpec extends Specification{

    void setup() {
        GrailsWebUtil.bindMockWebRequest(applicationContext)

    }

    void cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void "Test binding constructor adding via AST"() {
        given:
            def cls = getTestClass()

        when:
           def test = cls.newInstance(age:"10")

        then:
            test.age == 10
    }

    void "Test setProperties method added via AST"() {
        given:
            def cls = getTestClass()

        when:
           def test = cls.newInstance()
           test.properties = [age:"10"]

        then:
            test.age == 10

    }

    void "Test getProperties method added via AST"() {
        given:
            def cls = getTestClass()

        when:
           def test = cls.newInstance()
           test.properties['age', 'name'] = [age:"10"]

        then:
            test.age == 10
    }

    Class getTestClass() {
        def cls = classLoader.parseClass('''
        class Test {
            Long id
            Long version
            Integer age
        }
        ''')
        GrailsApplication application = GrailsWebRequest.lookup().applicationContext.grailsApplication
        application.initialise()
        application.addArtefact(DomainClassArtefactHandler.TYPE,cls)

        MetaClassEnhancer enhancer =  new MetaClassEnhancer()
        enhancer.addApi(new ControllersDomainBindingApi())
        enhancer.enhance(cls.metaClass)
        return cls
    }

    GroovyClassLoader getClassLoader() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerDomainTransformer() {
            @Override
            boolean shouldInject(URL url) {
                return true;
            }

        }
        gcl.classInjectors = [transformer] as ClassInjector[]

        return gcl
    }



    WebApplicationContext getApplicationContext() {
        def bb = new WebBeanBuilder()
        bb.beans {
            grailsApplication(DefaultGrailsApplication)
        }
        bb.createApplicationContext()
    }
}
