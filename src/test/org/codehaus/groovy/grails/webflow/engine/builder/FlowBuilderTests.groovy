package org.codehaus.groovy.grails.webflow.engine.builder;

import org.springframework.webflow.engine.*
import org.springframework.webflow.engine.support.ActionTransitionCriteria
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator
import org.springframework.binding.convert.service.DefaultConversionService
import org.springframework.webflow.expression.DefaultExpressionParserFactory
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext

class FlowBuilderTests extends GroovyTestCase{

    FlowBuilderServices flowBuilderServices

    void setUp() {
        ExpandoMetaClass.enableGlobally()
        flowBuilderServices = new FlowBuilderServices()
        MvcViewFactoryCreator viewCreator = new MvcViewFactoryCreator()
        viewCreator.applicationContext = new GrailsWebApplicationContext()
        flowBuilderServices.viewFactoryCreator = viewCreator
        flowBuilderServices.conversionService = new DefaultConversionService()
        flowBuilderServices.expressionParser = DefaultExpressionParserFactory.getExpressionParser()
    }

    void tearDown() {
        ExpandoMetaClass.disableGlobally()
    }

    void testCustomEntryAndExitActions() {
        def flow = new FlowBuilder("myFlow",flowBuilderServices, new FlowDefinitionRegistryImpl()).flow {
            displaySearchForm {
                onEntry {
                    "enterview"
                }
                onExit {
                    "exitview"
                }
                on("submit") {
                    error()
                }.to "displayResults"

            }
            displayResults {
                onEntry {
                    "end state entry"
                }
                redirect url:"blah"
            }
        }

        assert flow : "flow should not be null"


        def state = flow.getState('displaySearchForm')

        assertTrue state instanceof ViewState

        ViewState vs = state

        assert vs.entryActionList.size() == 1 : "should have 1 entry action!"
        assert vs.exitActionList.size() == 1 : "should have 1 exit action!"

        state = flow.getState('displayResults')

        assert state instanceof EndState : "should be an end state"

        EndState es = state
        assert es.entryActionList.size() == 1 : "should have 1 entry action!"

    }
    void testOnStartAndOnEndFlowEvents() {
        def flow = new FlowBuilder("myFlow",flowBuilderServices, new FlowDefinitionRegistryImpl()).flow {
            onStart {
                "started"
            }
            onEnd {
                "ended"
            }
            displaySearchForm {
                on("submit") {
                    error()
                }.to "displayResults"

            }
            displayResults()
        }

        assert flow : "should have created a flow"
        assert flow.startActionList.size() == 1 : "should have a start action"
        assert flow.endActionList.size() == 1 : "should have a end action"
    }

    void testOnRenderEventInViewFlow() {
        def flow = new FlowBuilder("myFlow",flowBuilderServices, new FlowDefinitionRegistryImpl()).flow {
            displaySearchForm {
                onRender {
                    "good"
                }
                on("submit") {
                    error()
                }.to "displayResults"

            }
            displayResults()
        }

        def state = flow.getState('displaySearchForm')

        assertTrue state instanceof ViewState

        ViewState vs = state

        assert vs.renderActionList.size() == 2 : "should have 2 render actions!"

        def lastAction = vs.renderActionList.toArray()[-1]
        assert lastAction.callable() == "good" : "should have returned 'good' from custom render action"

    }
    void testFlowWithTransitionCriteria() {
        def flow = new FlowBuilder("myFlow",flowBuilderServices, new FlowDefinitionRegistryImpl()).flow {
            displaySearchForm {
                on("submit") {
                    error()
                }.to "executeSearch"

            }
            executeSearch {
                action {
                    [results:searchService.executeSearch(params.q)]
                }
                on("success").to "displayResults"
                on(Exception).to "displaySearchForm"
            }
            displayResults()
        }

        assert flow
        assertEquals 3, flow.stateCount
        def state = flow.getState('displaySearchForm')

        assertTrue state instanceof ViewState
        assertEquals flow.startState, state
        assertEquals "displaySearchForm", state.id
        assertEquals 1, state.transitions.size()

        def t = state.transitions[0]
        assertTrue t.executionCriteria instanceof ActionTransitionCriteria
    }

    void testFlowDataModel() {


        def flow = new FlowBuilder("myFlow",getFlowBuilderServices(), new FlowDefinitionRegistryImpl()).flow {
            displaySearchForm {
                on("submit").to "executeSearch"
            }                   
            executeSearch {
                action {
                    [results:searchService.executeSearch(params.q)]
                }
                on("success").to "displayResults"
                on(Exception).to "displaySearchForm"
            }
            displayResults()
        }

        assert flow
        assertEquals 3, flow.stateCount
        def state = flow.getState('displaySearchForm')

        assertTrue state instanceof ViewState
        assertEquals flow.startState, state
        assertEquals "displaySearchForm", state.id
        assertEquals 1, state.transitions.size()

        def t = state.transitions[0]
        assertEquals "submit", t.id
        assertEquals "executeSearch", t.targetStateId

        state = flow.getState('executeSearch')

        assertTrue state instanceof ActionState
        assertEquals "executeSearch", state.id
        assertEquals 1, state.transitions.size()

        assertEquals 1, state.exceptionHandlerSet.size()

        state = flow.getState('displayResults')

        assertTrue state instanceof EndState
    }

    /*void testFlowBuilder() {
        def someOtherFlow = {

        }
        def bookStoreFlow = {
            displaySearchForm {
                on("submit").to "executeSearch"
            }
            executeSearch {
                action {
                    [results:searchService.executeSearch(params.q)]
                }
                on("success").to "displayResults"
                on(Exception).to "displaySearchForm"
            }
            
            displayResults {
                on("showItem").to "showItem"
                on("searchAgain").to "displaySearchForm"
            }
            showItem {
                action {
                    [item:Book.get(params.id)]
                }
                on("success").to "displayItem"
                on(Exception).to "displayResult"
            }
            displayItem {
                on("addToCart").to "addToCart"
                on("back").to "displayResults"
            }
            addToCart {
                action {
                    if(!flowScope['cartItems']) flowScope['cartItem'] = []
                    flowScope['cartItems'] << Book.get(params.id)
                }
                on('success').to "showCart"
                on(Exception).to 'displayItem'
            }
        }

    }  */
}
