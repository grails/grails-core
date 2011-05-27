package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification
import org.springframework.mock.web.MockServletContext

/**
 * More tests for {@link LinkGenerator }. See Also LinkGeneratorSpec.
 */
class LinkGeneratorWithUrlMappingsSpec extends Specification{

    def baseUrl = "http://myserver.com/foo"
    def context = null
    def path = "welcome"
    def action = [controller:'home', action:'index']

    def mappings = {
        "/${this.path}"(this.action)
    }

    def link = null

    protected getGenerator() {
        def generator = new DefaultLinkGenerator(baseUrl, context)
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        generator.urlMappingsHolder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(mappings ?: {}))
        generator
    }

    protected getUri() {
        generator.link(link)
    }

    void "Check that the correct relative link is generated for a controller and action"() {
        given:
            context = "/bar"

        when: "A link is created with only the controller specified"
            link = action

        then:
            uri == "$context/$path"
    }

    void "Check that the correct absolute link is generated for a controller and action"() {
        given:
            context = "/bar"

        when: "An absolute link is created with the controller and action specified"
            link = action + [absolute: true]

        then:
            uri == "$baseUrl/$path"
    }

    void "Check that an absolute link is generated for a relative link with no context path"() {
        given:
            context = null

        when:"A relative link is created with a controller, an action and no known context path"
            link = action

        then: "Check that an absolute link is created"
            uri == "$baseUrl/$path"
    }
}
