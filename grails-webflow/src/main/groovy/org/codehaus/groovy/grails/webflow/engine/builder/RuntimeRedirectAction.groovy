/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.metaclass.BeanBinding
import org.codehaus.groovy.grails.commons.metaclass.PropertyExpression
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder

import org.springframework.webflow.action.AbstractAction
import org.springframework.webflow.execution.Event
import org.springframework.webflow.execution.RequestContext

/**
 * Takes the parameters formulated in the builder and produces an ExternalRedirect at runtime.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RuntimeRedirectAction extends AbstractAction {

    def controller
    def action
    Map params
    UrlMappingsHolder urlMapper

    def resolveExpressionsInParams(ExpressionDelegate delegate, Map params) {
        for (entry in params) {
            if (entry.value instanceof Map) {
                resolveExpressionsInParams(delegate, entry.value)
            }
            else {
                entry.value = resolveExpression(delegate, entry.value)
            }
        }
    }

    def resolveExpression(ExpressionDelegate delegate, expression) {
        if (expression instanceof PropertyExpression) {
            return new GroovyShell(new BeanBinding(delegate)).evaluate(expression.getValue())
        }
        return expression
    }

    protected Event doExecute(RequestContext context) {
        if (!urlMapper) throw new IllegalStateException("Cannot redirect without an instance of [org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder] within the ApplicationContext")

        def delegate = new ExpressionDelegate(context)
        def controller = resolveExpression(delegate, controller)
        def action = resolveExpression(delegate, action)
        Map params = this.params.clone() 
        resolveExpressionsInParams(delegate, params)

        UrlCreator creator = urlMapper.getReverseMapping(controller, action, params)
         def url = creator.createRelativeURL(controller, action, params, 'utf-8')

        context.getExternalContext().requestExternalRedirect("contextRelative:$url")
        return success()
    }
}

/**
 * Used for evaluating PropertyExpression instances
 */
class ExpressionDelegate extends AbstractDelegate {
    ExpressionDelegate(RequestContext context) {
        super(context)
    }
}
