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
