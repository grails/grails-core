package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.metaclass.PropertyExpression
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.webflow.test.MockRequestContext
import org.springframework.webflow.test.MockExternalContext
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder

/**
 *
 */
class RuntimeRedirectActionTests extends GroovyTestCase{

    void testRedirectWithPropertyExpression() {
        GrailsWebUtil.bindMockWebRequest()

        try {
            def action   = new RuntimeRedirectAction()
            action.controller = "book"
            action.action = "show"
            action.params = [id: new PropertyExpression("flow.id")]
            action.urlMapper = new DefaultUrlMappingsHolder([])
            def ext = new MockExternalContext()
            def context = new MockRequestContext()
            context.setExternalContext(ext)
            context.getFlowScope().put("id", "1")
            action.execute(context)
            assert "contextRelative:/book/show/1" == ext.getExternalRedirectUrl()

            context.getFlowScope().put("id", "2")
            action.execute(context)
            assert "contextRelative:/book/show/2" == ext.getExternalRedirectUrl()


        }
        finally {
            RequestContextHolder.setRequestAttributes null
        }




    }
}
