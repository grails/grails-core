package org.grails.web.mapping

import grails.util.GrailsWebMockUtil
import grails.validation.ConstrainedProperty
import grails.web.mapping.UrlMapping
import grails.web.mapping.exceptions.UrlMappingException

import org.springframework.core.io.*

class RegexUrlMappingTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
  "/book/$author/$title/$test" {
      controller = "book"
      action = "show"
  }
  "/blog/$entry/$year?/$month?/$day?" {
     controller = "blog"
     action = "show"
  }
  "/surveys/$action?" {
      controller = "survey"
  }
  "/files/$path**?" {
      controller = "files"
  }
  "/filenameext/$fname$fext?" {
      controller = "download"
  }
  "/another/arbitrary/something-$prefix.$ext" {
      controller = "myFiles"
      action = "index"
  }
  "/foo"(controller:"foo", parseRequest:true)

  "/foo2"(controller: "foo") {
       parseRequest = true
   }

  "/foo3" {
       controller = "foo"
       parseRequest = true
   }

  name foo4: "/foo4" {
       controller = "foo"
       parseRequest = true
  }

  "/bar"(uri:"/x/y")

  "/surveys/view/$id" {
      controller = "survey"
      action = "viewById"
      constraints {
         id(matches:/\\d+/)
      }
  }
  "/surveys/view/$name" {
      controller = "survey"
      action = "viewByName"
  }
  "/reports/$foo" {
      controller = 'reporting'
      action = 'view'
  }
  "/$first-alpha-$second/$third-beta-$fourth-foo(.$format)?" {
      controller = 'hyphenTests'
      action = 'view'
  }
  "/plugins/grails-$plugin/tags/RELEASE_$version/$fullName(.$type)" {
      controller = 'website'
      action = 'displayPlugin'
  }
}
'''

    void testExtensionPrecededByTokenWhichMayContainDots() {
        def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes)))

        def info = holder.match("/plugins/grails-csv/tags/RELEASE_0.3.1/csv-0.3.1.pom")
        assertNotNull info
        assertEquals 'website', info.controllerName
        assertEquals 'displayPlugin', info.actionName
        assertEquals '0.3.1', info.params.version
        assertEquals 'csv', info.params.plugin
        assertEquals 'csv-0.3.1', info.params.fullName
        assertEquals 'pom', info.params.type
    }

    void testHyphenDelimiters() {
        def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes)))
    
        def info = holder.match("/one-alpha-two/three-beta-four-foo.json")
        assertNotNull info
        assertEquals 'hyphenTests', info.controllerName
        assertEquals 'view', info.actionName
        assertEquals 'one', info.params.first
        assertEquals 'two', info.params.second
        assertEquals 'three', info.params.third
        assertEquals 'four', info.params.fourth
        assertEquals 'json', info.params.format

 
        info = holder.match("/one-alpha-two/three-beta-four-foo")
        assertNotNull info
        assertEquals 'hyphenTests', info.controllerName
        assertEquals 'view', info.actionName
        assertEquals 'one', info.params.first
        assertEquals 'two', info.params.second
        assertEquals 'three', info.params.third
        assertEquals 'four', info.params.fourth
        assertNull info.params.format
    }

    void testMaptoURI() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def info = holder.match("/bar")
        assertEquals "/x/y", info.getURI()
    }

    void testParseRequestArgument() {

        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def info = holder.match("/foo")
        assertTrue "should have been a request parsing mapping", info.parsingRequest

        info = holder.match("/foo2")
        assertTrue "should have been a request parsing mapping", info.parsingRequest

        info = holder.match("/foo3")
        assertTrue "should have been a request parsing mapping", info.parsingRequest

        info = holder.match("/foo4")
        assertTrue "should have been a request parsing mapping", info.parsingRequest
    }

    void testNullableConstraintsInMapping() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def m = mappings[2]

        assert m.urlData.isOptional(0)

        assertEquals 1, m.constraints.length
        assertTrue m.constraints[0].nullable
    }

    void testCreateUrlFromMapping() {
        GrailsWebMockUtil.bindMockWebRequest()
        def res = new ByteArrayResource(mappingScript.bytes)

        def mappings = evaluator.evaluateMappings(res)

        def m = mappings[0]
        assert m

        assertEquals "/book/dierk/gina/foo", m.createURL(author: "dierk", title: "gina", test: "foo", "utf-8")

        m = mappings[1]
        assert m

        assertEquals "/blog/foo/2007/10/24", m.createURL(entry: "foo", year: 2007, month: 10, day: 24, "utf-8")
        assertEquals "/blog/foo/2007/10", m.createURL(entry: "foo", year: 2007, month: 10, "utf-8")
        assertEquals "/blog/foo/2007", m.createURL(entry: "foo", year: 2007, "utf-8")
        assertEquals "/blog/foo/2007", m.createURL(entry: "foo", year: 2007, null)
        assertEquals "/blog/foo", m.createURL(entry: "foo", "utf-8")
        assertEquals "/blog/foo", m.createURL(entry: "foo", null)
        assertEquals '/blog/My%20%2410', m.createURL(entry: 'My $10', "utf-8")
        assertEquals '/blog/My%20%24temp', m.createURL(entry: 'My $temp', "utf-8")
        assertEquals "/blog/foo?day=24", m.createURL(entry: "foo", day: 24, "utf-8")
        shouldFail { m.createURL([:], "utf-8") }

        m = mappings[3]
        assert m
        assertEquals "/files/path/to/my/file", m.createURL([path:"/path/to/my/file"], "utf-8")

        m = mappings[4] //filename+fileextension case
        assert m
        assertEquals "/filenameext/grails", m.createURL([fname:'grails'], "utf-8")
        assertEquals "/filenameext/grails.", m.createURL([fname:'grails.'], "utf-8")
        assertEquals "/filenameext/grails.jpg", m.createURL(fname:"grails",fext:".jpg", "utf-8")

        m = mappings[5]
        assert m
        assertEquals "/another/arbitrary/something-source.jar",
                m.createURL(controller:"myFiles",action:"index", prefix:"source", ext:"jar", "utf-8")

        // "ext" is a required property, so if it isn't specified an
        // exception should be thrown.
        shouldFail(UrlMappingException) {
            m.createURL(controller:"myFiles",action:"index", prefix:"source", "utf-8")
        }
    }

    void testCreateUrlWithFragment() {
        GrailsWebMockUtil.bindMockWebRequest()

        def res = new ByteArrayResource(mappingScript.bytes)

        def mappings = evaluator.evaluateMappings(res)

        def m = mappings[0]
        assert m

        assertEquals "/book/dierk/gina/foo#testfrag", m.createURL(author: "dierk", title: "gina", test: "foo", "utf-8", "testfrag")

        m = mappings[1]
        assert m

        assertEquals "/blog/foo/2007/10/24#testfrag2", m.createURL(entry: "foo", year: 2007, month: 10, day: 24, "utf-8", "testfrag2")

        // Test the behaviour of a null encoding.
        assertEquals "/blog/foo/2007/10/24#testfrag2", m.createURL(entry: "foo", year: 2007, month: 10, day: 24, null, "testfrag2")
    }



    void testMatchUriWithConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "hello", String.class)
        cp.nullable = false

        // mapping would be "/foo/$hello/bar
        def parser = new DefaultUrlMappingParser()

        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", null, null, null, null,UrlMapping.ANY_VERSION, [cp] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/world/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        assertEquals "world", info.parameters.hello
    }

    void testMatchUriWithMatchesConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "year", String.class)
        cp.matches = /\d{4}/

        // mapping would be "/foo/$hello/bar
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", null, null, null, null, UrlMapping.ANY_VERSION,[cp] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/2007/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        assertEquals "2007", info.parameters.year

        info = m.match("/foo/blah/bar")
        assertNull info
    }

    void testConstraintAsTiebreaker() {
        // test that two similar rules that only differ by # of constraints are evaluated correctly
        def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes)))

        def info = holder.match("/surveys/view/123")
        assertNotNull info
        assertEquals 'survey', info.controllerName
        assertEquals 'viewById', info.actionName

        info = holder.match("/surveys/view/foo")
        assertNotNull info
        assertEquals 'survey', info.controllerName
        assertEquals 'viewByName', info.actionName
    }
    
    void testParameterContainingADot() {
        def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(new ByteArrayResource(mappingScript.bytes)))
        
        def info = holder.match("/reports/my")
        assertNotNull info
        assertEquals 'reporting', info.controllerName
        assertEquals 'view', info.actionName
        assertEquals 'my', info.params.foo
        
        info = holder.match("/reports/my.id")
        assertNotNull info
        assertEquals 'reporting', info.controllerName
        assertEquals 'view', info.actionName
        assertEquals 'my.id', info.params.foo
    }
    
    void testInit() {
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse("/(*)/hello"), "test", null, null, null, null, null, UrlMapping.ANY_VERSION,[] as ConstrainedProperty[], servletContext)
    }

    void testMatchUriNoConstraints() {
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse("/foo/(*)/bar"), "test", null, null, null, null, null,UrlMapping.ANY_VERSION, [] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/test/bar")
        assert info
        assertEquals "test", info.controllerName

        info = m.match("/foo/bar/test")
        assertNull info
    }
}
