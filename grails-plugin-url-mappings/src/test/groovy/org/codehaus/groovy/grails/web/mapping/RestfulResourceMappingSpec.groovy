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

    void "Test multiple resources produce the correct URL mappings"() {
        given:"A URL mappings definition with a single resources"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"book")
            }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are seven of them in total"
            urlMappings.size() == 7

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'list'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'
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

    void "Test a single resource produces the correct links for reverse mapping"() {
        given:"A link generator definition with a single resource"
            def linkGenerator = getLinkGenerator {
                "/book"(resource:"book")
            }

        expect:"The generated links to be correct"
            linkGenerator.link(controller:"book", action:"create") == "http://localhost/book/create"
            linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/book/create"
            linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"show", method:"GET") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"edit", method:"GET") == "http://localhost/book/edit"
            linkGenerator.link(controller:"book", action:"delete", method:"DELETE") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"update", method:"PUT") == "http://localhost/book"
    }

    void "Test a resource produces the correct links for reverse mapping"() {
        given:"A link generator definition with a single resource"
        def linkGenerator = getLinkGenerator {
            "/books"(resources:"book")
        }

        expect:"The generated links to be correct"
            linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/books/create"
            linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/books"
            linkGenerator.link(controller:"book", action:"show", id:1, method:"GET") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"edit", id:1, method:"GET") == "http://localhost/books/1/edit"
            linkGenerator.link(controller:"book", action:"delete", id:1, method:"DELETE") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"update", id:1, method:"PUT") == "http://localhost/books/1"
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
