package org.grails.plugins.web.rest.transform

import grails.web.Action
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class LinkableTransformSpec extends Specification {

    void "Test that the resource transform creates a controller class"() {
        given:"A parsed class with a @Resource annotation"
            def gcl = new GroovyClassLoader()
            gcl.parseClass('''
    import grails.rest.*
    import grails.persistence.*

    @Linkable
    class Book {
    }
    ''')

        when:"A link is added"
            def domain = gcl.loadClass("Book")
            def book = domain.newInstance()
            book.link(rel:'foos', href:"/foo")
            def links = book.links()

        then:"The link is added to the available links"
            links[0].href == '/foo'

    }
}
