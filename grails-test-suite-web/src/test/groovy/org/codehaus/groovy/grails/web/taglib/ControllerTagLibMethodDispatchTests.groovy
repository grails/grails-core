package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllerTagLibMethodDispatchTests extends AbstractGrailsTagTests {

    void testControllerTagLibMethodDispatch() {
        def controller = ga.getControllerClass("TestController").newInstance()
        controller.foo()
        assertEquals '<a href="/test/foo"></a>hello! bar', response.contentAsString
    }

    protected void onInit() {
        def tagClass = gcl.parseClass('''
class MyTagLib {
    static namespace = "my"
    Closure test1 = { attrs, body ->
        out << body(foo:"bar", one:2)
    }

    Closure test2 = { attrs, body ->
        out << "hello! ${attrs.foo}"
    }
}
''')
       def tagClass2 = gcl.parseClass('''
class SecondTagLib {
   static namespace = "two"

   Closure test1 = { attrs, body ->
        out << my.test2(foo:"bar3")
   }

}
''')
        def controllerClass = gcl.parseClass('''
 class TestController {
    def foo = {
        // test invoke core tag
        response.writer << link(controller:'test',action:'foo')
        // test invoke namespaced tag
        response.writer << my.test2(foo:"bar")
    }
 }
 ''')

        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass2)
        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE,controllerClass)
    }
}
