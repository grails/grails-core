package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*
import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests


class FlowBuilderSubFlowExecutionTests extends AbstractGrailsTagAwareFlowExecutionTests{

    def searchMoreAction = { [moreResults:["one", "two", "three"]] }
    
    public Closure getFlowClosure() {
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
        return {
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

        def subflow = subflowState.subflow.getValue(null)

        assertEquals 4, subflow.stateCount

        assert subflow.getState('moreResults') instanceof EndState
        assert subflow.getState('noResults') instanceof EndState
    }


    void testSubFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()

        startFlow()

        assertCurrentStateEquals "displaySearchForm"

        signalEvent( "submit" )

        assertCurrentStateEquals "displayResults"


        signalEvent("searchDeeper")


        assertCurrentStateEquals "startExtendedSearch"

        signalEvent("findMore")

        assertFlowExecutionEnded()
        assertFlowExecutionOutcomeEquals "displayMoreResults"
    }


    void testSubFlowExecution2() {
        searchMoreAction = { error() }
        startFlow()

        assertCurrentStateEquals "displaySearchForm"

        signalEvent( "submit" )

        assertCurrentStateEquals "displayResults"

        signalEvent("searchDeeper")

        assertCurrentStateEquals "startExtendedSearch"
                      

        signalEvent("findMore")

        assertFlowExecutionEnded()
        assertFlowExecutionOutcomeEquals "displayNoMoreResults"        
    }


}