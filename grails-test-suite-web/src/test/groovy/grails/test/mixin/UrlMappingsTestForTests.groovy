package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.UrlMappingsUnitTest
import spock.lang.Specification

class UrlMappingsTestForTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    Class[] getControllersToMock() {
        [BookController]
    }

    void testUrlMappings() {

        expect:
        assertUrlMapping "/book/list", controller:"book", action:"list"
    }
}

@Artefact("Controller")
class BookController {
    def list() { }
}

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
    }
}
