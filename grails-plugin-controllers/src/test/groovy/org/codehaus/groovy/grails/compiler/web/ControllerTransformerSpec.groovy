package org.codehaus.groovy.grails.compiler.web

import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder
import javax.servlet.http.HttpServletRequest

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 07/03/2011
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
class ControllerTransformerSpec extends Specification{

    void "Test that the API is injected via AST"() {

        given:
            def gcl = new GrailsAwareClassLoader()
            gcl.classInjectors = [new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }] as ClassInjector[]


        when:
            def cls = gcl.parseClass('''
class TestTransformedController {}
''')
            def controller = cls.newInstance()

        then:
            controller != null
            controller.controllersApi != null

        when:
            GrailsWebUtil.bindMockWebRequest()

        then:
            controller.getRequest() instanceof HttpServletRequest
            controller.request instanceof HttpServletRequest


        when:
            controller.render(view:"foo")

        then:
            controller.modelAndView != null

    }

    def cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }
}
