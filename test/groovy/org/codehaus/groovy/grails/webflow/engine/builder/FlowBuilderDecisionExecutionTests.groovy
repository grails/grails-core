package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*
import org.springframework.webflow.execution.ViewSelection

class FlowBuilderDecisionExecutionTests extends AbstractFlowExecutionTests{

    def searchService = [executeSearch:{["foo", "bar"]}]
    def params = [q:"foo"]

    void testNoResultFlowExecution() {
        searchService = [executeSearch:{[]}]

        ViewSelection viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "noResults", viewSelection.viewName

    }
    void testSuccessFlowExecution() {
        searchService = [executeSearch:{["foo", "bar"]}]

        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "displayResults", viewSelection.viewName
        assertEquals( ["foo", "bar"],viewSelection.model.results)
    }



    FlowDefinition getFlowDefinition() {


        
        new FlowBuilder("myFlow").flow {
            displaySearchForm {
                on("submit").to "executeSearch"
            }
            executeSearch {
                action {
                    def r = searchService.executeSearch(params.q)
                    r ? success(results:r) : none()                   
                }
                on("success").to "displayResults"
                on("none").to "noResults"
                on("error").to "displaySearchForm"
            }
            noResults()
            displayResults()
        }
    }
}