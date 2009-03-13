package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.test.execution.*
import org.springframework.webflow.definition.*
import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests

class FlowBuilderDecisionExecutionTests extends AbstractGrailsTagAwareFlowExecutionTests{

    public Closure getFlowClosure() {
        return {
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

    def searchService = [executeSearch:{["foo", "bar"]}]
    def params = [q:"foo"]

    void testNoResultFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        searchService = [executeSearch:{[]}]

        startFlow()

        assertCurrentStateEquals "displaySearchForm"

        signalEvent( "submit" )

        assertFlowExecutionEnded()
        assertFlowExecutionOutcomeEquals "noResults"
    }
    void testSuccessFlowExecution() {
        grails.util.GrailsWebUtil.bindMockWebRequest()
        searchService = [executeSearch:{["foo", "bar"]}]

        startFlow()

        assertCurrentStateEquals "displaySearchForm"

        signalEvent( "submit" )

        assertFlowExecutionEnded()
        assertFlowExecutionOutcomeEquals "displayResults"

    }




}