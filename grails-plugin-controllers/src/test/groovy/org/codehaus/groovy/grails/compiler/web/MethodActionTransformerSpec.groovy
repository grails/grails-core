package org.codehaus.groovy.grails.compiler.web

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification
import grails.util.BuildSettings
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder


class MethodActionTransformerSpec extends Specification{


    void setup(){
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'true'
    }


    void "Test that a closure action changed to method"() {

        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new MethodActionTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
            gcl.classInjectors = [transformer] as ClassInjector[]


        when:
            def cls = gcl.parseClass('''
            class TestTransformedController {

                def action = {
                }

                }
            ''')
            def controller = cls.newInstance()

        then:
            controller
            controller.getClass().getMethod("action", [] as Class[]) != null
    }

    void "Test annotated controllers"() {
         when:
            def gcl = new GrailsAwareClassLoader()
            def cls = gcl.parseClass('''
            @grails.artefact.Artefact("Controller")
            class AnnotatedControllerTransformerController {
                def action = {
                }
            }
            ''')

            def controller = cls.newInstance()

        then:
            controller
            controller.getClass().getMethod("action", [] as Class[]) != null

    }

    void "Test command object actions"() {
         when:
            def gcl = new GrailsAwareClassLoader()
            def cls = gcl.parseClass('''
            @grails.artefact.Artefact("Controller")
            class AnnotatedControllerTransformerController {
                def commandObjectClosure
                def commandObjectMethod
                def action = { CommandObject cmd->
                    commandObjectClosure = cmd
                }
                def action2(CommandObject cmd){
                    commandObjectMethod = cmd
                }
            }
            class CommandObject{
                String prop
            }
            ''')

            GrailsWebUtil.bindMockWebRequest()

            def controller = cls.newInstance()
            controller.action()
            controller.action2()
        then:
            controller
            controller.commandObjectClosure
            controller.commandObjectMethod

    }

    def cleanup() {
        RequestContextHolder.setRequestAttributes(null)
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'false'
    }

}

