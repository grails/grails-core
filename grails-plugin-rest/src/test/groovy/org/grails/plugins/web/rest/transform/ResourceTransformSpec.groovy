package org.grails.plugins.web.rest.transform

import grails.web.Action
import spock.lang.Issue
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Graeme Rocher
 */
class ResourceTransformSpec extends Specification {

    void "Test that the resource transform creates a controller class"() {
         given:"A parsed class with a @Resource annotation"
            def gcl = new GroovyClassLoader()
            gcl.parseClass('''
import grails.rest.*
import grails.persistence.*

@Entity
@Resource(formats=['html','xml'])
class Book {
}
''')

        when:"The controller class is loaded"
            def domain = gcl.loadClass("Book")
            def ctrl = gcl.loadClass('BookController')

        then:"It exists"
            ctrl != null
            getMethod(ctrl, "index", Integer)
            getMethod(ctrl, "index", Integer).getAnnotation(Action)
            getMethod(ctrl, "index")
            getMethod(ctrl, "index").getAnnotation(Action)
            getMethod(ctrl, "show", domain)
            getMethod(ctrl, "edit", domain)
            getMethod(ctrl, "create")
            getMethod(ctrl, "save", domain)
            getMethod(ctrl, "save")
            getMethod(ctrl, "update", domain)
            getMethod(ctrl, "update")
            getMethod(ctrl, "delete", domain)
            getMethod(ctrl, "delete")

            ctrl.scope == "singleton"

        when:"A link is added"
            def book = domain.newInstance()
            book.link(rel:'foos', href:"/foo")
            def links = book.links()

        then:"The link is added to the available links"
            links[0].href == '/foo'

    }

    @Issue("GRAILS-10741")
    void "Test that the resource transform creates a read only controller class"() {
        given:"A parsed class with a @Resource annotation"
        def gcl = new GroovyClassLoader()
        gcl.parseClass('''
import grails.rest.*
import grails.persistence.*

@Entity
@Resource(formats=['html','xml'], readOnly=true)
class Book {
}
''')

        when:"The controller class is loaded"
            def domain = gcl.loadClass("Book")
            def ctrl = gcl.loadClass('BookController')

        then:"It exists"
            ctrl != null
            getMethod(ctrl, "index", Integer)
            getMethod(ctrl, "index", Integer).getAnnotation(Action)
            getMethod(ctrl, "index")
            getMethod(ctrl, "index").getAnnotation(Action)
            getMethod(ctrl, "show", domain)
            ctrl.scope == "singleton"

        and:"It doesn't exist"
            !getMethod(ctrl, "edit", domain)
            !getMethod(ctrl, "create")
            !getMethod(ctrl, "save", domain)
            !getMethod(ctrl, "save")
            !getMethod(ctrl, "update", domain)
            !getMethod(ctrl, "update")
            !getMethod(ctrl, "delete", domain)
            !getMethod(ctrl, "delete")

        when:"A link is added"
            def book = domain.newInstance()
            book.link(rel:'foos', href:"/foo")
            def links = book.links()

        then:"The link is added to the available links"
            links[0].href == '/foo'
    }

    private Method getMethod(Class clazz, String methodName, Class[] paramTypes) {
        try {
            clazz.getDeclaredMethod(methodName, paramTypes)
        } catch (NoSuchMethodException e) {
            return null
        }
    }
}
