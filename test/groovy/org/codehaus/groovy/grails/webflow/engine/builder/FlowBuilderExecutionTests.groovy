package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*

class FlowBuilderExecutionTests extends AbstractFlowExecutionTests{

    void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }

    def searchService = [executeSearch:{["foo", "bar"]}]
    def params = [q:"foo"]

    void testFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "displayResults", viewSelection.viewName
        assertEquals( ["foo", "bar"],viewSelection.model.results)
        viewSelection = signalEvent("return")
        assertEquals "displaySearchForm", viewSelection.viewName
    }

    void testFlowExecutionException() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        searchService.executeSearch = { throw new FooException() }

        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "errorView", viewSelection.viewName

    }

    FlowDefinition getFlowDefinition() {

        new FlowBuilder("myFlow").flow {
            displaySearchForm {
                on("submit").to "executeSearch"
            }
            executeSearch {
                action {
                    [results:searchService.executeSearch(params.q)]
                }
                on("success").to "displayResults"
                on("error").to "displaySearchForm"
                on(FooException).to "errorView"
            }
            displayResults {
                on("return").to "displaySearchForm"
            }
            errorView()
        }
    }
}
class FooException extends Exception {}