package org.codehaus.groovy.grails.webflow

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder


/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jul 22, 2008
 */
class FlowRedirectTests extends AbstractGrailsTagAwareFlowExecutionTests{

    void testRedirectToControllerAndAction() {
        startFlow()

        def context = signalEvent("test1")

        assertFlowExecutionEnded()

        assertEquals "contextRelative:/test/foo",context.getExternalRedirectUrl()

    }

    void testRedirectToControllerAndActionWithParamsObjectAccess() {

       webRequest.params.id = "1"
       startFlow()

       def context = signalEvent("test2")

       assertFlowExecutionEnded()

       assertEquals "contextRelative:/test/foo/1",context.getExternalRedirectUrl()
    }


    Map params = [id:10] // this should not be resolved

    public Closure getFlowClosure() {
        return {
            one {
                on("test1").to "test1"
                on("test2").to "test2"
            }
            test1 {
                redirect(controller:"test", action:"foo")
            }
            test2 {
                redirect(controller:"test", action:"foo", id:params.id)
            }
        }
    }



}