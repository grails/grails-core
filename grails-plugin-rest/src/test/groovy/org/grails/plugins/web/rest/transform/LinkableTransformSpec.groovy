package org.grails.plugins.web.rest.transform

import grails.web.Action
import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

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

        when: "find all added methods"
            List<Method> addedLinkMethods = book.getClass().getMethods().findAll {it.name == 'link' || it.name == 'links'}
        then: "they are marked as Generated"
            addedLinkMethods.each {
                assert it.isAnnotationPresent(Generated)
            }
    }
}
