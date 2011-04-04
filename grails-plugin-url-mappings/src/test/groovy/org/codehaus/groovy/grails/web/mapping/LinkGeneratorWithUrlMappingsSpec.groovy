package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification
import org.springframework.mock.web.MockServletContext

/**
 * More tests for {@link LinkGenerator }. See Also LinkGeneratorSpec.
 */
class LinkGeneratorWithUrlMappingsSpec extends Specification{


    void "Check that the correct relative link is generated for a controller and action"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", '/bar')
            def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
            def mappings = evaluator.evaluateMappings {
                "/welcome"(controller:'home', action:'index')
            }
            generator.urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)

        when: "A link is created with only the controller specified"
            def uri = generator.link(controller:'home', action:'index')

        then:
            uri == '/bar/welcome'

    }

    void "Check that the correct absolute link is generated for a controller and action"() {
        given:
         def generator = new DefaultLinkGenerator("http://myserver.com/foo", '/bar')
            def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
            def mappings = evaluator.evaluateMappings {
                "/welcome"(controller:'home', action:'index')
            }
            generator.urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)

        when: "An absolute link is created with the controller and action specified"
            def uri = generator.link(controller:'home', action:'index', absolute:true)

        then:
            uri == 'http://myserver.com/foo/welcome'
    }

    void "Check that an absolute link is generated for a relative link with no context path"() {
        given:
         def generator = new DefaultLinkGenerator("http://myserver.com/foo", null)
         def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
         def mappings = evaluator.evaluateMappings {
            "/welcome"(controller:'home', action:'index')
         }
         generator.urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)

        when:"A relative link is created with a controller, an action and no known context path"
            def uri = generator.link(controller:'home', action:'index')

        then: "Check that an absolute link is created"
            uri == 'http://myserver.com/foo/welcome'


    }
}
