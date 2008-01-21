package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*
import org.codehaus.groovy.grails.web.servlet.mvc.*


class UrlMappingsHolderTests extends AbstractGrailsControllerTests {

    def mappingScript = '''
mappings {
  "/blog/$entry/$year?/$month?/$day?" {
        controller = "blog"
        action = "show"
  }
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

   def mappingScript2 = '''
mappings  {
    "/specific/$action?" {
        controller = "someController"
    }

    "/admin/$controller/$action?/$id?" {
        constraints {
            id(matches:/\\d+/)
        }
    }
}
   '''

   def mappingWithNamedArgs = '''
mappings {
    "/author/$lastName/$firstName" (controller:'product', action:'show')
}
'''
   def mappingWithNamedArgsAndClosure = '''
mappings {
    "/author/$lastName/$firstName" (controller:'product') {
         action = 'show'
    }
}
'''

	void testGetReverseMappingWithNamedArgsAndClosure() {
           def res = new ByteArrayResource(mappingWithNamedArgsAndClosure.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)

           def params = [lastName:'Winter', firstName:'Johnny']
           def m = holder.getReverseMapping("product", "show", params)
           assertNotNull "getReverseMapping returned null", m
           
           assertEquals "/author/Winter/Johnny", m.createURL(params, "utf-8")
    }
            
	void testGetReverseMappingWithNamedArgs() { 
       runTest {
           def res = new ByteArrayResource(mappingWithNamedArgs.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)

           def params = [lastName:'Winter', firstName:'Johnny']
           def m = holder.getReverseMapping("product", "show", params)
           assertNotNull "getReverseMapping returned null", m
           
           assertEquals "/author/Winter/Johnny", m.createURL(params, "utf-8")
      }
                 
    }
     void testGetReverseMappingWithExcessArgs() {
         def res = new ByteArrayResource(mappingScript.bytes)

         def evaluator = new DefaultUrlMappingEvaluator()
         def mappings = evaluator.evaluateMappings(res)

         def holder = new DefaultUrlMappingsHolder(mappings)

         // test with exact argument match
         Map vars = [entry: "foo", year: 2007, month: 3, day: 17, some: "other"]
        def m = holder.getReverseMapping("blog", "show", vars)

         assert m
         println m.controllerName

         def url = m.createRelativeURL("blog", "show", vars, "utf-8")
         assertEquals("/blog/foo/2007/3/17?some=other", url)
    }

    void testGetReverseMappingWithVariables() {
             def res = new ByteArrayResource(mappingScript2.bytes)
             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)

             def holder = new DefaultUrlMappingsHolder(mappings)

            // test with fewer arguments
            def m = holder.getReverseMapping("test", "list",null)
            assert m

            assertEquals "/admin/test/list/1", m.createURL(controller:"test", action:"list",id:1, "utf-8")

            assertEquals "/admin/test/list/1?foo=bar", m.createURL(controller:"test", action:"list",id:1, foo:"bar", "utf-8")

            m = holder.getReverseMapping("someController", "test", null)
            assert m
            assertEquals "/specific/test", m.createURL(controller:"someController", action:"test", "utf-8")
    }

    void testGetReverseMappingWithFewerArgs() {
        runTest {
             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)

             def holder = new DefaultUrlMappingsHolder(mappings)

            // test with fewer arguments
            def m = holder.getReverseMapping("blog", "show", [entry:"foo", year:2007])

             assert m
             assertEquals "blog", m.controllerName
             assertEquals "show", m.actionName

        }
    }

    void testGetReverseMappingWithExactArgs() {
        runTest {
             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)

             def holder = new DefaultUrlMappingsHolder(mappings)

             // test with exact argument match
             def m = holder.getReverseMapping("blog", "show", [entry:"foo", year:2007, month:3, day:17])

             assert m
             assertEquals "blog", m.controllerName
             assertEquals "show", m.actionName
             assertEquals("/blog/foo/2007/3/17?test=test", m.createURL([controller:"blog",action:"show",entry:"foo",year:2007,month:3,day:17,test:'test'], "utf-8"))

            // test with fewer arguments
            m = holder.getReverseMapping("blog", "show", [entry:"foo", year:2007])

             assert m
             assertEquals "blog", m.controllerName
             assertEquals "show", m.actionName



            m = holder.getReverseMapping("book", null, [author:"dierk", title:"GINA", test:3])
            assert m
             assertEquals "book", m.controllerName
             


            m = holder.getReverseMapping(null, null, [:])
            assert m

        }

    }



}

