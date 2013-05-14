package org.codehaus.groovy.grails.web.mapping

import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class UrlMappingsWithHttpMethodSpec extends Specification{

    def mappings = {
         "/foo"( controller:"bar", method:"POST" )

    }

    void "Test that the http method can be used as a prefix to URL mappings"() {
        given:"A URL mapping evaluator"
            def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())


        when:"The mappings are evaluated"
            def mappings = evaluator.evaluateMappings mappings

        then:"The mapping only accepts POST requests"
            mappings.size() == 1
            mappings[0].httpMethod == 'POST'

    }
}
