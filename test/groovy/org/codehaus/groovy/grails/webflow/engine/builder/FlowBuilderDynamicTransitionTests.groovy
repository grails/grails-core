package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*

class FlowBuilderDynamicTransitionTests extends AbstractFlowExecutionTests{

    void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }

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
        def viewSelection = startFlow()
        assert viewSelection          
        assertEquals "stepOne", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "stepFour", viewSelection.viewName
    }

    FlowDefinition getFlowDefinition() {

        new FlowBuilder("myFlow").flow {
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
