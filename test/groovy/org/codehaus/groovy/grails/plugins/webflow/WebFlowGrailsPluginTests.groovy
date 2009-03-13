/* Copyright 2006-2007 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.webflow

import org.codehaus.groovy.grails.plugins.web.AbstractGrailsPluginTests
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator

/**
 * Tests the webflow Grails plugin intializes correctly
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 1, 2008
 */
class WebFlowGrailsPluginTests extends AbstractGrailsPluginTests {

    public void onSetUp() {

        gcl.parseClass('''
class WebFlowGrailsPluginTestController {
    def shoppingCartFlow = {
        displaySearchForm {
            on("submit").to "executeSearch"
        }
        executeSearch {
            action {
                success()
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
''')
        super.onSetUp();
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
	    pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.webflow.WebFlowGrailsPlugin")

    }


    void testWebFlowGrailsPluginBeans() {
         assert appCtx.containsBean("viewFactoryCreator")

         assertEquals MvcViewFactoryCreator, appCtx.viewFactoryCreator.getClass()

        assertTrue appCtx.containsBean("flowBuilderServices")
        assertTrue appCtx.containsBean("flowScopeRegistrar")
        assertTrue appCtx.containsBean("conversationManager")
    }
}