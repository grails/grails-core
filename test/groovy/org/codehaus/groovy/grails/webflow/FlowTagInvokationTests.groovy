/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Sep 7, 2007
 * Time: 8:33:22 AM
 * 
 */
package org.codehaus.groovy.grails.webflow

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

class FlowTagInvokationTests extends AbstractGrailsTagAwareFlowExecutionTests {

    void testRegularTagInvokation() {
        request[GrailsApplicationAttributes.CONTROLLER] = ga.getControllerClass("TestController").newInstance()
        def viewSelection = startFlow()
        viewSelection = signalEvent( "two" )

        assertEquals '<a href="/foo/bar"></a>',viewSelection.model.theLink
    }

    void testNamespacedTagInvokation() {

        def viewSelection = startFlow()
        viewSelection = signalEvent( "three" )

        assertEquals '<a href="/foo/bar"></a>',viewSelection.model.theLink
    }

    void onInit() {
        def controllerClass = gcl.parseClass('''
class TestController {
    def index = {}       
}
        ''')
        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
    }
    FlowDefinition getFlowDefinition() {
        new FlowBuilder("myFlow").flow {
            one {
                on("two").to "two"
                on("three").to "three"
            }
            two {
                action {
                    [theLink:link(controller:"foo", action:"bar")]
                }
                on("success").to "end"
            }
            three {
                action {
                    [theLink:g.link(controller:"foo", action:"bar")]
                }
                on("success").to "end"
            }
            end()
        }
    }
}