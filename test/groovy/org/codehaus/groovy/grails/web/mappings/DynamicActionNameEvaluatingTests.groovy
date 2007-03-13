package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.*
import org.codehaus.groovy.grails.web.servlet.mvc.*

class DynamicActionNameEvaluatingTests extends AbstractGrailsControllerTests {

    def mappingScript = '''
mappings {
  "/book/$author/$title/$test" {
      controller = "book"
      action = { "${params.test}" }
  }
  "/$controller/$action?/$id?" {
      controller = { params.controller }
      action = { params.action }
      id = { params.id }
  }    
}
'''
    void testNamedParameterAction() {
        runTest {
             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)


             def m = mappings[0]
             assert m

             def info = m.match("/book/graeme/grails/read")
             assert info
             assert info.controllerName
             assertEquals "read", info.actionName

        }
    }

    void testNamedParameterAction2() {
        runTest {
             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)


             def m = mappings[1]
             assert m

             def info = m.match("/book/show/1")
             assert info

             assertEquals "book", info.controllerName
             assertEquals "show", info.actionName
             assertEquals "1", info.id

        }

    }



}

