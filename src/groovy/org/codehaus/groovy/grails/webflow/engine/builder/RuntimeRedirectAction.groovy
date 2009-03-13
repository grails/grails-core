package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.metaclass.PropertyExpression
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.webflow.execution.RequestContext
import org.codehaus.groovy.grails.commons.metaclass.BeanBinding
import org.springframework.webflow.action.AbstractAction
import org.springframework.webflow.execution.Event


/**
 * A Class that takes the parameters formulated in the builder and produces an ExternalRedirect at runtime
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jul 22, 2008
 */
class RuntimeRedirectAction extends AbstractAction{

    def controller
    def action
    Map params
    UrlMappingsHolder urlMapper


    def resolveExpressionsInParams(ExpressionDelegate delegate, Map params) {
        for (entry in params) {
            if(entry.value instanceof Map) {
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
        if(!urlMapper) throw new IllegalStateException("Cannot redirect without an instance of [org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder] within the ApplicationContext")

        def delegate = new ExpressionDelegate(context)
        controller = resolveExpression(delegate, controller)
        action = resolveExpression(delegate, action)
        resolveExpressionsInParams(delegate, params)

        UrlCreator creator = urlMapper.getReverseMapping(controller, action, params)
        def url = creator.createRelativeURL(controller, action, params, 'utf-8')

        context.getExternalContext().requestExternalRedirect("contextRelative:$url");
        return success();
    }
}
/**
 * Used for evaluating PropertyExpression instances
 */
class ExpressionDelegate extends AbstractDelegate {

    public ExpressionDelegate(RequestContext context) {
        super(context);
    }

}