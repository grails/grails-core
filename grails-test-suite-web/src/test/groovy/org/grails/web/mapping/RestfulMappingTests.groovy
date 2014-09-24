package org.grails.web.mapping

import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.core.io.*

class RestfulMappingTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
  "/books" {
      controller = "book"
      action = [GET:"list", DELETE:"delete", POST:"update", PUT:"save", PATCH:"patch"]
  }
}
'''

    def mappingScript2 = '''
mappings {
    "/"(view:"/index")

    "/signin"(controller: "authentication") {
            action = [GET: "loginForm", POST: "handleLogin"]
    }
}
'''


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

        webRequest.currentRequest.method = "PATCH"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "patch", info.actionName

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

        webRequest.currentRequest.method = "PATCH"

        info = holder.match("/books")
        assertEquals "book", info.controllerName
        assertEquals "patch", info.actionName
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
