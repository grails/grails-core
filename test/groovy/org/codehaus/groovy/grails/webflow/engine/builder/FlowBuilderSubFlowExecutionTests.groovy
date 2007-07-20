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
        grails.util.GrailsWebUtil.bindMockWebRequest()
        def theFlow = getFlowDefinition()

         // test sub flow model
        def subflowState = theFlow.getState('extendedSearch')

        assert subflowState instanceof SubflowState

        def subflow = subflowState.subflow

        assertEquals 4, subflow.stateCount

        assert subflow.getState('moreResults') instanceof EndState
        assert subflow.getState('noResults') instanceof EndState
    }

    def searchMoreAction = { [moreResults:["one", "two", "three"]] }
    void testSubFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()

        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "displayResults", viewSelection.viewName

        viewSelection = signalEvent("searchDeeper")        
        //assertEquals( ["foo", "bar"],viewSelection.model.results)

        assertEquals "startExtendedSearch", viewSelection.viewName
        
        viewSelection = signalEvent("findMore")
        assertEquals "displayMoreResults", viewSelection.viewName
    }


    void testSubFlowExecution2() {
        searchMoreAction = { error() }
        def viewSelection = startFlow()
        assert viewSelection
        assertEquals "displaySearchForm", viewSelection.viewName
        viewSelection = signalEvent( "submit" )
        assert viewSelection
        assertEquals "displayResults", viewSelection.viewName
        viewSelection = signalEvent("searchDeeper")
        //assertEquals( ["foo", "bar"],viewSelection.model.results)

        assertEquals "startExtendedSearch", viewSelection.viewName
                      
        //assertEquals( ["foo", "bar"],viewSelection.model.results)
        viewSelection = signalEvent("findMore")
        assertEquals "displayNoMoreResults", viewSelection.viewName
    }

    FlowDefinition getFlowDefinition() {
        def searchService = [executeSearch:{["foo", "bar"]}]
        def params = [q:"foo"]

        def extendedSearchFlow = {
            startExtendedSearch {
                on("findMore").to "searchMore"
                on("searchAgain").to "noResults"
            }
            searchMore {
                action(searchMoreAction) /*{ ctx ->
                    def results = searchService.deepSearch(ctx.conversation.query)
                    if(!results)return error
                    ctx.conversation.extendedResults = results
                }*/
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
                on("searchDeeper").to "extendedSearch"
                on("searchAgain").to "displaySearchForm"
            }
            extendedSearch {
                subflow(extendedSearchFlow)
                on("moreResults").to "displayMoreResults"
                on("noResults").to "displayNoMoreResults"

            }
            displayMoreResults()
            displayNoMoreResults()
        }
    }
}