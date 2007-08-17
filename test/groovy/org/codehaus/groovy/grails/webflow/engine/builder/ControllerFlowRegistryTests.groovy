package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.codehaus.groovy.grails.support.MockApplicationContext

/**
* Tests for the ControllerFlowRegistry class

* @author Graeme Rocher
* @since 0.6
 *
* Created: Jul 3, 2007
* Time: 9:22:23 AM
*
*/

class ControllerFlowRegistryTests extends GroovyTestCase {

    def gcl = new GroovyClassLoader()

    void setUp() {
        gcl.parseClass('''
class FooController {
    def searchService = [executeSearch: { ['book'] }]
	def shoppingCartFlow = {
		enterPersonalDetails {
			on("submit").to "enterShipping"
		}
		enterShipping  {
			on("back").to "enterPersonDetails"
			on("submit").to "enterPayment"
		}
		enterPayment  {
			on("back").to "enterShipping"
			on("submit").to "confirmPurchase"
		}
		confirmPurchase  {
			on("confirm").to "processPurchaseOrder"
		}
		processPurchaseOrder  {
			action {
				println "processing purchase order"
				[order:"done"]
			}
			on("error").to "confirmPurchase"
			on(Exception).to "confirmPurchase"
			on("success").to "displayInvoice"
		}
		displayInvoice()
	}

    def anotherAction = {

    }
}
        ''')
    }

    void testFlowRegsitry() {
        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        assertEquals 1, ga.controllerClasses.size()

        ControllerFlowRegistry factoryBean = new ControllerFlowRegistry()
        factoryBean.grailsApplication = ga
        factoryBean.beanFactory = new MockApplicationContext()
        factoryBean.afterPropertiesSet()

        FlowDefinitionRegistry registry = factoryBean.getObject()

        assert registry
        assertEquals 1,registry.getFlowDefinitionCount()
        def cartFlow = registry.getFlowDefinition("shoppingCart")
         assert cartFlow
        assertEquals 6,cartFlow.stateCount

    }
}