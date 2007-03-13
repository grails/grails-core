package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.*

class UrlMappingEvaluatorTests extends GroovyTestCase {

    def mappingScript = '''
mappings {
  "/$id/$year?/$month?/$day?" {  
        controller = "blog"
        action = "show"
        constraints {
            year(matches:/\\d{4}/)
            month(matches:/\\d{2}/)
        }
  }

  "/product/$name" {
        controller = "product"
        action = "show"
  }
  "/book/$author/$title/$test" {
      controller = "book"
  }
}
'''
    
    void testEvaluateMappings() {
         def res = new ByteArrayResource(mappingScript.bytes)

         def evaluator = new DefaultUrlMappingEvaluator()
         def mappings = evaluator.evaluateMappings(res)

         assert mappings
         assertEquals 3, mappings.size()

         def m1 = mappings[0]
         assertEquals "/(*)/(*)?/(*)?/(*)?", m1.urlData.urlPattern
         assertEquals 4, m1.urlData.tokens.size()


         def info = m1.match("/myentry/2007/04/28")

         assertEquals "myentry", info.id
         assertEquals "2007", info.parameters.year
         assertEquals "04", info.parameters.month
         assertEquals "28", info.parameters.day
         assertEquals "blog", info.controllerName
         assertEquals "show", info.actionName


         assert m1.match("/myentry/2007/04/28")
         assert m1.match("/myentry/2007/04")
         assert m1.match("/myentry/2007")
         assert m1.match("/myentry")


         def m2 = mappings[1]

         info = m2.match("/product/MacBook")
         assert info
         assertEquals "MacBook", info.parameters.name

         assert !m2.match("/product")
         assert !m2.match("/foo/bar")
         assert !m2.match("/product/MacBook/foo")         
    }


}

