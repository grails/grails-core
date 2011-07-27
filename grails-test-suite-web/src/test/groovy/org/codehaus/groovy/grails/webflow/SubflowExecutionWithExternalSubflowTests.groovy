package org.codehaus.groovy.grails.webflow

import junit.framework.TestCase

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests

class SubflowExecutionWithExternalSubflowTests extends AbstractGrailsTagAwareFlowExecutionTests {

    void testSubFlowExecution() {
        startFlow()
        assertCurrentStateEquals "start"

        signalEvent("next")
        assertCurrentStateEquals "subber"

        signalEvent("proceed")
        assertCurrentStateEquals "start"
    }

    Closure getFlowClosure() {
        return {
            start {
                on("next").to "subber"
            }
            subber {
                subflow(new OtherSubflowController().subberFlow)
                on("subberEnd").to("start")
            }
        }
    }
}

class OtherSubflowController {
    def subberFlow = {
        subber {
            on("proceed").to("subberEnd")
        }
        subberEnd()
    }
}
