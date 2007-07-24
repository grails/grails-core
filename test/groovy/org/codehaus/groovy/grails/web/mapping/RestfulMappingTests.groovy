package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*
import org.codehaus.groovy.grails.web.servlet.mvc.*


class RestfulMappingTests extends AbstractGrailsControllerTests {

    def mappingScript = '''
mappings {
  "/books" {
      controller = "book"
      action = [GET:"list", DELETE:"delete", POST:"update", PUT:"save"]
  }
}
'''


	void testRestfulMappings() {
           def res = new ByteArrayResource(mappingScript.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
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

}

