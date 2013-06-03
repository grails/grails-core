package org.grails.plugins.web.rest.transform

import grails.web.Action
import spock.lang.Specification

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
@Resource(responseFormats=['html','xml'])
class Book {
}
''')

        when:"The controller class is loaded"
            def domain = gcl.loadClass("Book")
            def ctrl = gcl.loadClass('BookController')

        then:"It exists"
            ctrl != null
            ctrl.getDeclaredMethod("index", Integer)
            ctrl.getDeclaredMethod("index", Integer).getAnnotation(Action)
            ctrl.getDeclaredMethod("index")
            ctrl.getDeclaredMethod("index").getAnnotation(Action)
            ctrl.getDeclaredMethod("show", domain)
            ctrl.getDeclaredMethod("edit", domain)
            ctrl.getDeclaredMethod("create")
            ctrl.getDeclaredMethod("save", domain)
            ctrl.getDeclaredMethod("save")

    }
}
