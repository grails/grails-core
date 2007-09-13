/**
 * Tests Grails data binding capabilities
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 12, 2007
 * Time: 11:50:39 AM
 * 
 */
package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class DataBindingTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass('''
class TestController {
    def index = {}
}
class Book {
    Long id
    Long version
    String title
    Author author
    URL site
}
class Author {
    Long id
    Long version
    String name
    String hairColour
}
        ''')
    }

    void testTypeConversionErrors() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("site", "not_a_valid_URL")


        def params = c.params

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params

        println b.errors
        assert b.hasErrors()

        def error = b.errors.getFieldError('site')
                      

    }

    void testAssociationAutoCreation() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("title", "The Stand")
        request.addParameter("author.name", "Stephen King")

        def params = c.params

        assertEquals "The Stand", params.title

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
    }

    void testMultiDBinding() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("author.name", "Stephen King")
        request.addParameter("author.hairColour", "Black")
        request.addParameter("title", "The Stand")
        def params = c.params

        def a = ga.getDomainClass("Author").newInstance()

        assertEquals "Stephen King",params['author'].name
        println params['author']
        a.properties = params['author']
        assertEquals "Stephen King", a.name
        assertEquals "Black", a.hairColour


        def b = ga.getDomainClass("Book").newInstance()
        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
                               
    }

}