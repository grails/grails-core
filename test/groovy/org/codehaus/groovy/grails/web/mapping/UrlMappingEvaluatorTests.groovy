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

  "/author/$lastName/$firstName" (controller:'author', action:'show') {
     constraints {
        lastName(maxSize:5)
        firstName(maxSize:5)
     }
  }

  "/music/$band/$album" (controller:'music', action:'show')
}
'''
    
    void testEvaluateMappings() {
         def res = new ByteArrayResource(mappingScript.bytes)

         def evaluator = new DefaultUrlMappingEvaluator()
         def mappings = evaluator.evaluateMappings(res)

         assert mappings
         assertEquals 5, mappings.size()

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
         
         def m4 = mappings[3]
         info = m4.match("/author/Brown/Jeff")
         assert info
         assertEquals "Brown", info.parameters.lastName
         assertEquals "Jeff", info.parameters.firstName
         assertEquals "show", info.actionName
         assertEquals "author", info.controllerName
         
         // first name too long
         assert !m4.match("/author/Lang/Johnny")
         
         // both names too long
         assert !m4.match("/author/Winter/Johnny")
         
         // last name too long
         assert !m4.match("/author/Winter/Edgar")
         
         def m5 = mappings[4]
         info = m5.match("/music/Rush/Hemispheres")
         assert info
         assertEquals "Rush", info.parameters.band
         assertEquals "Hemispheres", info.parameters.album
         assertEquals "show", info.actionName
         assertEquals "music", info.controllerName
    }
}

