package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.springframework.mock.web.MockServletContext


class RestfulMappingTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
  "/books" {
      controller = "book"
      action = [GET:"list", DELETE:"delete", POST:"update", PUT:"save"]
  }

  "/authors"(resource:"author")
}
'''

    def mappingScript2 = '''
mappings  {
    "/"(view:"/index")

    "/signin"(controller: "authentication") {
            action = [GET: "loginForm", POST: "handleLogin"]
    }
}
'''

   void testRestfulMappingsWithResource() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest
        webRequest.currentRequest.method = "GET"

        def info = holder.match("/authors")

        assertTrue "should have been a request parsing mapping", info.parsingRequest

        assertEquals "author", info.controllerName
        assertEquals "show", info.actionName

        webRequest.currentRequest.method = "DELETE"

        info = holder.match("/authors")
        assertEquals "author", info.controllerName
        assertEquals "delete", info.actionName

        webRequest.currentRequest.method = "POST"

        info = holder.match("/authors")
        assertEquals "author", info.controllerName
        assertEquals "save", info.actionName

        webRequest.currentRequest.method = "PUT"

        info = holder.match("/authors")
        assertEquals "author", info.controllerName
        assertEquals "update", info.actionName
    }

    void testResultMappingsWithAbsolutePaths() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest
        webRequest.currentRequest.method = "GET"

        def info = holder.match("/books")

        assertEquals "book", info.controllerName
        assertEquals "list", info.actionName

        webRequest.currentRequest.method = "DELETE"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "delete", info.actionName

        webRequest.currentRequest.method = "POST"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "update", info.actionName

        webRequest.currentRequest.method = "PUT"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "save", info.actionName

    }

    void testRestfulMappings() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest
        webRequest.currentRequest.method = "GET"

        def info = holder.match("/books")

        assertEquals "book", info.controllerName
        assertEquals "list", info.actionName

        webRequest.currentRequest.method = "DELETE"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "delete", info.actionName

        webRequest.currentRequest.method = "POST"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "update", info.actionName

        webRequest.currentRequest.method = "PUT"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "save", info.actionName
    }

    void testRestfulMappings2() {
        def res = new ByteArrayResource(mappingScript2.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest
        webRequest.currentRequest.method = "GET"

        def info = holder.match("/signin")

        assertEquals "authentication", info.controllerName
        assertEquals "loginForm", info.actionName

        webRequest.currentRequest.method = "POST"

        info = holder.match("/signin")

        assertEquals "authentication", info.controllerName
        assertEquals "handleLogin", info.actionName

    }

}

