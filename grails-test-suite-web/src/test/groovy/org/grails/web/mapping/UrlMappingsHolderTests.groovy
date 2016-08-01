package org.grails.web.mapping

import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder;
import org.springframework.core.io.*
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.junit.Test
import static org.junit.Assert.*

@TestMixin(ControllerUnitTestMixin)
class UrlMappingsHolderTests {

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
mappings {
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

    def mappingWithNamespace = '''
mappings {
    "/$namespace/$controller/$action?/$id?" ()
}
'''

    @Test
    void testGetReverseMappingWithNamespace() {
        def res = new ByteArrayResource(mappingWithNamespace.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def m = holder.getReverseMapping("product", "show", "foo", null, [:])
        assertNotNull "getReverseMapping returned null", m

        assertEquals "/foo/product/show", m.createURL("product", "show", "foo", null, [:], "utf-8")
    }


    @Test
    void testGetReverseMappingWithNamedArgsAndClosure() {
        def res = new ByteArrayResource(mappingWithNamedArgsAndClosure.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def params = [lastName:'Winter', firstName:'Johnny']
        def m = holder.getReverseMapping("product", "show", params)
        assertNotNull "getReverseMapping returned null", m

        assertEquals "/author/Winter/Johnny", m.createURL(params, "utf-8")
    }

    @Test
    void testGetReverseMappingWithNamedArgs() {
        def res = new ByteArrayResource(mappingWithNamedArgs.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def params = [lastName:'Winter', firstName:'Johnny']
        def m = holder.getReverseMapping("product", "show", params)
        assertNotNull "getReverseMapping returned null", m

        assertEquals "/author/Winter/Johnny", m.createURL(params, "utf-8")
    }

    @Test
    void testGetReverseMappingWithExcessArgs() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        // test with exact argument match
        Map vars = [entry: "foo", year: 2007, month: 3, day: 17, some: "other"]
        def m = holder.getReverseMapping("blog", "show", vars)

        assert m

        def url = m.createRelativeURL("blog", "show", vars, "utf-8")
        assertEquals("/blog/foo/2007/3/17?some=other", url)
    }

    @Test
    void testGetReverseMappingWithVariables() {
        def res = new ByteArrayResource(mappingScript2.bytes)
        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        // test with fewer arguments
        def m = holder.getReverseMapping("test", "list",null)
        assert m

        assertEquals "/admin/test/list/1", m.createURL(controller:"test", action:"list",id:1, "utf-8")

        // make sure caching works as expected
        assertEquals "/admin/test/list/1", m.createURL("test", "list", [id:1], "utf-8")
        assertEquals "/admin/test/list/1", m.createURL("test", "list", [id:1], "utf-8")

        assertEquals "/admin/test/list/1?foo=bar", m.createURL(controller:"test", action:"list",id:1, foo:"bar", "utf-8")

        m = holder.getReverseMapping("someController", "test", null)
        assert m
        assertEquals "/specific/test", m.createURL(controller:"someController", action:"test", "utf-8")
    }

    @Test
    void testGetReverseMappingWithFewerArgs() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        // use un-cached holder for testing
        def holder = new DefaultUrlMappingsHolder(mappings,null,true)
        holder.setUrlCreatorMaxWeightedCacheCapacity(0)
        holder.initialize()

        // test with fewer arguments
        def m = holder.getReverseMapping("blog", "show", [entry:"foo", year:2007])

        assert m
        assertEquals "blog", m.controllerName
        assertEquals "show", m.actionName
    }

    @Test
    void testGetReverseMappingWithExactArgs() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(mainContext)
        def mappings = evaluator.evaluateMappings(res)

        // use un-cached holder for testing
        def holder = new DefaultUrlMappingsHolder(mappings,null,true)
        holder.setUrlCreatorMaxWeightedCacheCapacity(0)
        holder.initialize()

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
