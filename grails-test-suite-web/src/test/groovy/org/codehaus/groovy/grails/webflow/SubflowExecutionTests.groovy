package org.codehaus.groovy.grails.webflow

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class SubflowExecutionTests extends AbstractGrailsTagAwareFlowExecutionTests {
    def subberFlow = {
        subber {
            on("proceed").to("subberEnd")
        }
        subberEnd()
    }

    void testSubFlowExecution() {
        registerFlow('subflowExecutionTests/subber', subberFlow)
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
                subflow(subberFlow)
                on("subberEnd").to("start")
            }
        }
    }
}
