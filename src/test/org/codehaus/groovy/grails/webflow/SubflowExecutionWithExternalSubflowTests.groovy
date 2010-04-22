package org.codehaus.groovy.grails.webflow;

import groovy.lang.Closure;

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests;

import junit.framework.TestCase;

class SubflowExecutionWithExternalSubflowTests extends AbstractGrailsTagAwareFlowExecutionTests{

    void testSubFlowExecution() {
        startFlow()

        assertCurrentStateEquals "start"
        signalEvent("next")

        assertCurrentStateEquals "subber"

        signalEvent("proceed")

        assertCurrentStateEquals "start"    
    }
    public Closure getFlowClosure() {
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
