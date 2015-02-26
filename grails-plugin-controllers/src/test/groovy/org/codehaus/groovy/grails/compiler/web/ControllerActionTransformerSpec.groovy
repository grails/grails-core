package org.codehaus.groovy.grails.compiler.web

import grails.compiler.ast.ClassInjector
import grails.compiler.traits.ControllerTraitInjector
import grails.compiler.traits.TraitInjector
import grails.util.BuildSettings
import grails.util.GrailsWebMockUtil
import grails.web.Action
import grails.web.servlet.context.GrailsWebApplicationContext

import java.lang.reflect.Modifier

import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.web.ControllerActionTransformer
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ControllerActionTransformerSpec extends Specification {

    def gcl

    void setup() {
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'true'
        gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }

        gcl.classInjectors = [transformer] as ClassInjector[]
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        def appCtx = new GrailsWebApplicationContext()
        def servletContext = webRequest.servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }

    void "Test that a closure action has changed to method"() {

        when:
            def cls = gcl.parseClass('''
            class TestTransformedToController {

                def action = {
                }

                }
            ''')
            def controller = cls.newInstance()

        then:
          controller
          controller.getClass().getMethod("action", [] as Class[]) != null
    }

    void 'Test that user applied annotations are applied to generated action methods'() {
        given:
        def cls = gcl.parseClass('''
        class SomeController {
            @Deprecated
            def action1(){}
            @Deprecated
            def action2(String paramName){}
}
''')

        when:
        def action1NoArgMethod = cls.getMethod('action1')

        then:
        action1NoArgMethod.getAnnotation(Action)
        action1NoArgMethod.getAnnotation(Deprecated)

        when:
        def action2MethodWithStringArg = cls.getMethod('action2', [String] as Class[])

        then:
        !action2MethodWithStringArg.getAnnotation(Action)
        action2MethodWithStringArg.getAnnotation(Deprecated)

        when:
        def action2NoArgMethod = cls.getMethod('action2')

        then:
        action2NoArgMethod.getAnnotation(Action)
        action2NoArgMethod.getAnnotation(Deprecated)
    }
    
    void 'Test that a controller may have an abstract method - GRAILS-10509'() {
        given:
        def controllerClass = gcl.parseClass('''
            @grails.artefact.Artefact('Controller')
            abstract class SomeController {
                def someAction() {}
                abstract someAbstractMethod()
            }
''')
        when:
        def method = controllerClass.getMethod('someAbstractMethod')
        
        then:
        Modifier.isAbstract(method.modifiers)
        
        when:
        method = controllerClass.getMethod('someAction')
        
        then:
        !Modifier.isAbstract(method.modifiers)
    }

    void 'Test action overiding'() {
        given:
            def superControllerClass = gcl.parseClass('''
            @grails.artefact.Artefact('Controller')
            class SuperController {
                def methodAction() {
                    [ actionInvoked: 'SuperController.methodAction' ]
                }
                def methodActionWithParam(String s) {
                    [ paramValue: s ]
                }
            }
''')
            def superController = superControllerClass.newInstance()
            def subControllerClass = gcl.parseClass('''
            class SubController extends SuperController {
                def methodAction() {
                    [ actionInvoked: 'SubController.methodAction' ]
                }
                def methodActionWithParam(Integer i) {
                    [ paramValue: i ]
                }
            }
''')
            def subController = subControllerClass.newInstance()

        when:
            def model = superController.methodAction()

        then:
            'SuperController.methodAction' == model.actionInvoked

        when:
            superController.params.s = 'Super Controller Param'
            model = superController.methodActionWithParam()

        then:
            'Super Controller Param' == model.paramValue

        when:
            model = subController.methodAction()

        then:
            'SubController.methodAction' == model.actionInvoked

        when:
            subController.params.s = 'Super Controller Param'
            model = subController.methodActionWithParam()

        then:
            null == model.paramValue

        when:
            subController.params.i = 42
            model = subController.methodActionWithParam()

        then:
            42 == model.paramValue
    }

   /* void "Test annotated controllers"() {
        when:
        def cls = gcl.parseClass('''
            class AnnotatedControllerTransformer1Controller {
                def action = {
                }
            }
            ''')

        def controller = cls.newInstance()

        then:
        controller
        controller.getClass().getMethod("action", [] as Class[]) != null

    }
*/

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'false'
    }
}
