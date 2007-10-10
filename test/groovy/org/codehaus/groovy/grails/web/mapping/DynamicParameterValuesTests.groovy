/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 10, 2007
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

class DynamicParameterValuesTests extends AbstractGrailsControllerTests {
    def mappingScript = '''
mappings {
  "/book/the_stand" {
      controller = "book"
      action = "show"
      id = "The Stand"
      price = 10.5
  }
}
'''

    void testImplicitNamedAction() {

             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)


             def m = mappings[0]
             assert m

             def info = m.match("/book/the_stand")
             assert info
             info.configure(webRequest)

             assertEquals "book", info.controllerName
             assertEquals "show", info.actionName
             assertEquals "The Stand", info.id
             assertEquals "The Stand", webRequest.params.id
             assertEquals 10.5, webRequest.params.price

    }
}