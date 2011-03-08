package org.codehaus.groovy.grails.compiler.web

import grails.util.GrailsWebUtil
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

 /**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 07/03/2011
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
class ControllerTransformerSpec extends Specification{

    void "Test get artefact type"() {
        when:
            def transformer = new ControllerTransformer()

        then:
            transformer.artefactType == 'Controller'
    }

    void "Test that the API is injected via AST"() {

        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
            gcl.classInjectors = [transformer] as ClassInjector[]


        when:
            def cls = gcl.parseClass('''
class TestTransformedController {}
''')
            def controller = cls.newInstance()

        then:

            controller != null
            controller.instanceControllersApi != null

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

    void "Test annotated artefact"() {
         when:
            def gcl = new GrailsAwareClassLoader()
            def cls = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class AnnotatedControllerTransformerController {

}
''')
            def controller = cls.newInstance()

        then:

            controller != null
            controller.instanceControllersApi != null

    }

    def cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

