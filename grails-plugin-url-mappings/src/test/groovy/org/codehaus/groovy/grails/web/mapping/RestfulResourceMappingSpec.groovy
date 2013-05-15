package org.codehaus.groovy.grails.web.mapping

import grails.web.CamelCaseUrlConverter
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class RestfulResourceMappingSpec extends Specification{

    def mappings = {
        "/book"(resource:"book")
        "/books"(resources:"book")
        "/authors"(resources: "author") {
            "/books"(resource:"book")
        }
    }

    void "Test a single resource produces the correct URL mappings"() {
        given:"A URL mappings definition with a single resource"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/book"(resource:"book")
            }

        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are six of them in total"
            urlMappings.size() == 6

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            urlMappingsHolder.matchAll('/book/create', 'GET')
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].httpMethod == 'GET'

            !urlMappingsHolder.matchAll('/book/create', 'POST')
            !urlMappingsHolder.matchAll('/book/create', 'PUT')
            !urlMappingsHolder.matchAll('/book/create', 'DELETE')

            urlMappingsHolder.matchAll('/book/edit', 'GET')
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/book/edit', 'POST')
            !urlMappingsHolder.matchAll('/book/edit', 'PUT')
            !urlMappingsHolder.matchAll('/book/edit', 'DELETE')

            urlMappingsHolder.matchAll('/book', 'POST')
            urlMappingsHolder.matchAll('/book', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/book', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/book', 'PUT')
            urlMappingsHolder.matchAll('/book', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/book', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/book', 'DELETE')
            urlMappingsHolder.matchAll('/book', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/book', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/book', 'GET')
            urlMappingsHolder.matchAll('/book', 'GET')[0].actionName == 'show'
            urlMappingsHolder.matchAll('/book', 'GET')[0].httpMethod == 'GET'
    }


    LinkGenerator getLinkGenerator(Closure mappings) {
        def generator = new DefaultLinkGenerator("http://localhost", null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = getUrlMappingsHolder mappings
        return generator;
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}
