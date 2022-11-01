package org.grails.web.servlet.mvc

import grails.web.servlet.mvc.GrailsParameterMap
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * Tests the behaviour of the GrailsParameterMap params object.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class ParamsObjectTests {

    /**
     * The grails params object exhibits multi dimensional behaviour. This test tests that.
     */
    @Test
    void testMultiDHashBehaviour() {
        def request = new MockHttpServletRequest()

        request.addParameter("test", "1")
        request.addParameter("firstName", "Fred")
        request.addParameter("lastName", "Flintstone")
        request.addParameter("book.title", "The Stand")
        request.addParameter("book.author.name", "Stephen King")
        request.addParameter("book.id", "10")
        request.addParameter("publisher.name", "Apress")
        request.addParameter("publisher.authors[0].name", "Fred")
        request.addParameter("publisher.authors[1].name", "Joe")
        request.addParameter("test..foo..bar", "Stuff")

        def params = new GrailsParameterMap(request)

        assertEquals "1", params.test
        assertEquals "Fred", params.firstName
        assertEquals "Flintstone", params.lastName
        assertEquals "The Stand", params.'book.title'
        assertEquals "Stephen King", params.'book.author.name'
        assertEquals "The Stand", params['book'].title
        assertEquals "Stephen King", params['book']['author.name']
        assertEquals "Stephen King", params['book']['author'].name
        assertEquals "Apress", params['publisher'].name
        assertEquals "Fred", params['publisher'].'authors[0].name'
    }
}
