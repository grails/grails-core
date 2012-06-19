package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * Tests that focus on ensuring the applicationContext, grailsApplication and servletContext objects are available to UrlMappings
 */
class UrlMappingsBindingSpec extends Specification{

    void "Test that common applications variables are available in UrlMappings"() {
        when:"Mappings that use application variables"
            def evaluator = getEvaluator()
            def urlMappings = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(mappings))

        then:"The url mappings are valid"
            urlMappings != null
    }

    protected DefaultUrlMappingEvaluator getEvaluator() {
        final servletContext = new MockServletContext()
        final ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)
        return new DefaultUrlMappingEvaluator(servletContext)
    }

    Closure getMappings() {
        return {
            "/foo" {
                assert applicationContext != null
                assert grailsApplication != null
                assert servletContext != null
            }
        }
    }
}
