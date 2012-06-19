package org.codehaus.groovy.grails.webflow

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class FlowTagInvokationTests extends AbstractGrailsTagAwareFlowExecutionTests {

    void testRegularTagInvokation() {
        request[GrailsApplicationAttributes.CONTROLLER] = ga.getControllerClass("TestController").newInstance()
        startFlow()
        signalEvent('two')

        def model = getFlowScope()

        assertEquals '<a href="/foo/bar?execution=1"></a>',model.theLink
    }

    void testNamespacedTagInvokation() {
        startFlow()
        signalEvent('three')

        def model = getFlowScope()
        assertEquals '<a href="/foo/bar?execution=1"></a>',model.theLink
    }

    void onInit() {
        def controllerClass = gcl.parseClass('''
class TestController {
    def index = {}
}
        ''')
        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
    }

    Closure getFlowClosure() {
        return {
            one {
                on('two') {
                    [theLink:link(controller:"foo", action:"bar")?.toString()]
                }.to 'two'
                on('three') {
                    [theLink:g.link(controller:"foo", action:"bar")?.toString()]
                }.to 'three'
            }
            two {
                on('success').to 'end'
            }
            three {
                on('success').to 'end'
            }
            end()
        }
    }
}
