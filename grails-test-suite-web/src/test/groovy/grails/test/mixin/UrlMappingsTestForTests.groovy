package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.UrlMappingsUnitTest
import org.junit.Test
import spock.lang.Specification

class UrlMappingsTestForTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    Class[] getControllersToMock() {
        [BookController]
    }

    @Test
    void testUrlMappings() {
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
