package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*
import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests

class FlowBuilderDynamicTransitionTests extends AbstractGrailsTagAwareFlowExecutionTests{


    void testFlowDefinition() {

        def startState = flowDefinition.getStartState()
        assert startState
        assertEquals "stepOne", startState.id

        assertEquals 4, flowDefinition.stateCount

        def stepTwo = flowDefinition.getState("stepTwo")
        assertTrue stepTwo instanceof ActionState
        
    }
    void testFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        startFlow()

        assertCurrentStateEquals "stepOne"
        signalEvent( "submit" )

        assertFlowExecutionEnded()
        assertFlowExecutionOutcomeEquals "stepFour"
        
    }

    String getFlowId() { "myFlow" }

    Closure getFlowClosure() {
        return {
            stepOne {
                on("submit").to "stepTwo"
            }
            stepTwo {
                action {
                   flow.put('nextStep',"stepFour")
                }
                on("success").to { flow.get('nextStep') }
            }
           stepThree {
               on("success").to "stepFour"
           }
           stepFour()
        }
    }
}
