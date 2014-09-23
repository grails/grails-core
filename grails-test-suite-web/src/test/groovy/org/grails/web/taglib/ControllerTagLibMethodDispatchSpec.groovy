package org.grails.web.taglib

import grails.artefact.Artefact
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 0.4
 */
@TestFor(TestController)
@Mock([MyTagLib, TwoTagLib])
class ControllerTagLibMethodDispatchSpec extends Specification {

    void testControllerTagLibMethodDispatch() {
        when:
        controller.foo()
        
        then:
        '<a href="/test/foo"></a>hello! bar' == response.contentAsString
    }
}

@Artefact('Controller')
class TestController {
    def foo = {
        // test invoke core tag
        response.writer << link(controller:'test',action:'foo')
        // test invoke namespaced tag
        response.writer << my.test2(foo:"bar")
    }
}

@Artefact('TagLib')
class MyTagLib {
    static namespace = "my"
    Closure test1 = { attrs, body ->
        out << body(foo:"bar", one:2)
    }

    Closure test2 = { attrs, body ->
        out << "hello! ${attrs.foo}"
    }
}

@Artefact('TagLib')
class TwoTagLib {
    static namespace = "two"

    Closure test1 = { attrs, body ->
        out << my.test2(foo:"bar3")
    }
}
