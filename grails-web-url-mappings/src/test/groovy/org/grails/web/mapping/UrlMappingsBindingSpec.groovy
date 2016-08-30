package org.grails.web.mapping

import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.web.context.support.GenericWebApplicationContext
import spock.lang.Specification
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import grails.core.GrailsApplication
import grails.core.DefaultGrailsApplication

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
        final ctx = new GenericWebApplicationContext(servletContext)
        ctx.defaultListableBeanFactory.registerSingleton(GrailsApplication.APPLICATION_ID,new DefaultGrailsApplication())
        ctx.refresh()
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)
        return new DefaultUrlMappingEvaluator(ctx)
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
