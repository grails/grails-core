package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*
import org.springframework.webflow.execution.ViewSelection

class FlowBuilderDecisionExecutionTests extends AbstractFlowExecutionTests{

    void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }

    def searchService = [executeSearch:{["foo", "bar"]}]
    def params = [q:"foo"]

    void testNoResultFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        searchService = [executeSearch:{[]}]

        ViewSelection viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "noResults", viewSelection.viewName

    }
    void testSuccessFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
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
                    def result = success(results:r)
                    r ? result : none()
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