package grails.test.mixin

import grails.artefact.Artefact

import org.junit.Test

@TestFor(UrlMappings)
@Mock([BookController])

class UrlMappingsTestForTests {

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
