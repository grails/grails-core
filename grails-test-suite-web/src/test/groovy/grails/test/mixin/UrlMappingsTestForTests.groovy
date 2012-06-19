package grails.test.mixin

import org.junit.Test
import grails.artefact.Artefact

/**

 */
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
