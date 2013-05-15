package org.codehaus.groovy.grails.web.mapping

import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class UrlMappingsWithHttpMethodSpec extends Specification{

    def mappings = {
         "/foo"( controller:"bar", action:"save", method:"POST" )
         "/foo"( controller:"bar", action:"update", method:"PUT" )

    }

    void "Test that the http method can be used as a prefix to URL mappings"() {
        given:"A URL mapping evaluator"
            def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())


        when:"The mappings are evaluated"
            def mappings = evaluator.evaluateMappings mappings

        then:"The mapping only accepts POST requests"
            mappings.size() == 2
            mappings[0].httpMethod == 'POST'
            mappings[1].httpMethod == 'PUT'

    }

    void "Test that URL mappings can be applied only to a certain HTTP method"() {
        when:"A URL is matched for a valid HTTP method"
            def results = urlMappingsHolder.matchAll('/foo', 'POST')
        then:"A match is found"
            results.size() == 1
            results[0].controllerName == 'bar'
            results[0].actionName == 'save'

        when:"A URL is matched with an invalid HTTP method"
             results = urlMappingsHolder.matchAll('/foo', 'GET')
        then:"No results are found"
            results.size() == 0

    }

    UrlMappingsHolder getUrlMappingsHolder() {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def mappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(mappings)
    }
}
