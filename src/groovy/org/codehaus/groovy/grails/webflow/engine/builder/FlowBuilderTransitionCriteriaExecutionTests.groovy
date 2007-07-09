package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*

class FlowBuilderTransitionCriteriaTests extends AbstractFlowExecutionTests{

    void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }


    void testFlowExecution() {
        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "enterPersonalDetails", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "enterPersonalDetails", viewSelection.viewName

        viewSelection = signalEvent("another")
        assertEquals "enterShipping", viewSelection.viewName
    }


    FlowDefinition getFlowDefinition() {

        new FlowBuilder("myFlow").flow {
            enterPersonalDetails {
                on("submit") { ctx ->
                    error()
                }.to "enterShipping"
                on("another") { ctx ->
                    ctx.flowScope.put("hello", "world")
                }.to "enterShipping"
            }
            enterShipping  {
                on("back").to "enterPersonalDetails"
                on("submit").to "displayInvoice"
            }
            displayInvoice()
        }
    }
}
