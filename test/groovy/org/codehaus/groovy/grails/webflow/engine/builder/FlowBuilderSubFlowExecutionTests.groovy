package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*


class FlowBuilderSubFlowExecutionTests extends AbstractFlowExecutionTests{

    void setUp() {
        ExpandoMetaClass.enableGlobally()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }    

    def foo() {
        "bar"
    }

    void testClosureMetaClassModifications() {
     def callable = {
            foo()
         }
         def metaClass = GroovySystem.metaClassRegistry.getMetaClass(callable.class)
         metaClass.invokeMethod = {String name, args-> "foo" }
         assertEquals "foo", callable()
    }

    void testSubFlowDefinition() {
        def theFlow = getFlowDefinition()

         // test sub flow model
        def subflowState = theFlow.getState('displayResults')

        assert subflowState instanceof SubflowState

        def subflow = subflowState.subflow

        assertEquals 4, subflow.stateCount

        assert subflow.getState('moreResults') instanceof EndState
        assert subflow.getState('noResults') instanceof EndState
    }

    void testSubFlowExecution() {
        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "results", viewSelection.viewName
        //assertEquals( ["foo", "bar"],viewSelection.model.results)
        viewSelection = signalEvent("findMore")
        assertEquals "displayMoreResults", viewSelection.viewName
    }

    FlowDefinition getFlowDefinition() {
        def searchService = [executeSearch:{["foo", "bar"]}]
        def params = [q:"foo"]

        def displayResultsSubFlow = {
            results {
                on("findMore").to "searchMore"
                on("searchAgain").to "noResults"
            }
            searchMore {
                action {
                    [moreResults:["one", "two", "three"]]
                }
                on("success").to "moreResults"
                on("error").to "noResults"
            }
            moreResults()
            noResults()

        }
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
            }
            displayResults {
                subflow(displayResultsSubFlow)
                on("moreResults").to "displayMoreResults"
            }
            displayMoreResults()

        }
    }
}