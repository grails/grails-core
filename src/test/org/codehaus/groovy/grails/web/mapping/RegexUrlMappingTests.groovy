package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*
import grails.util.GrailsWebUtil
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException

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
}
'''
    void testParseRequestArgument() {

        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def info = holder.match("/foo")

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
        GrailsWebUtil.bindMockWebRequest()
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
        assertEquals '/blog/My+%2410', m.createURL(entry: 'My $10', "utf-8")
        assertEquals '/blog/My+%24temp', m.createURL(entry: 'My $temp', "utf-8")
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
        GrailsWebUtil.bindMockWebRequest()

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

    void testComparable() {
        def parser = new DefaultUrlMappingParser()

        def m1 = new RegexUrlMapping(parser.parse("/foo/"), "test", null, null, null, servletContext)
        def m2 = new RegexUrlMapping(parser.parse("/"), "test", null, null, null, servletContext)
        def m3 = new RegexUrlMapping(parser.parse("/foo/bar/(*)"), "test", null, null, null, servletContext)
        def m4 = new RegexUrlMapping(parser.parse("/foo/(*)/bar"), "test", null, null, null, servletContext)
        def m5 = new RegexUrlMapping(parser.parse("/(*)/foo/bar"), "test", null, null, null, servletContext)
        def m6 = new RegexUrlMapping(parser.parse("/foo/(*)"), "test", null, null, null, servletContext)
        def m7 = new RegexUrlMapping(parser.parse("/(*)"), "test", null, null, null, servletContext)
        def m8 = new RegexUrlMapping(parser.parse("/foo/(*)/(*)"), "test", null, null, null, servletContext)
        def m9 = new RegexUrlMapping(parser.parse("/(*)/(*)/bar"), "test", null, null, null, servletContext)
        def m10 = new RegexUrlMapping(parser.parse("/(*)/(*)/(*)"), "test", null, null, null, servletContext)


        assertTrue m1.compareTo(m2) > 0
        assertTrue m1.compareTo(m3) > 0
        assertTrue m1.compareTo(m4) > 0
        assertTrue m1.compareTo(m5) > 0
        assertTrue m1.compareTo(m6) > 0
        assertTrue m1.compareTo(m7) > 0
        assertTrue m1.compareTo(m8) > 0
        assertTrue m1.compareTo(m9) > 0
        assertTrue m1.compareTo(m10) > 0

        assertTrue m2.compareTo(m1) < 0
        assertTrue m2.compareTo(m3) > 0
        assertTrue m2.compareTo(m4) > 0
        assertTrue m2.compareTo(m5) > 0
        assertTrue m2.compareTo(m6) > 0
        assertTrue m2.compareTo(m7) > 0
        assertTrue m2.compareTo(m8) > 0
        assertTrue m2.compareTo(m9) > 0
        assertTrue m2.compareTo(m10) > 0

        assertTrue m3.compareTo(m1) < 0
        assertTrue m3.compareTo(m2) < 0
        assertTrue m3.compareTo(m4) > 0
        assertTrue m3.compareTo(m5) > 0
        assertTrue m3.compareTo(m6) > 0
        assertTrue m3.compareTo(m7) > 0
        assertTrue m3.compareTo(m8) > 0
        assertTrue m3.compareTo(m9) > 0
        assertTrue m3.compareTo(m10) > 0

        assertTrue m4.compareTo(m1) < 0
        assertTrue m4.compareTo(m2) < 0
        assertTrue m4.compareTo(m3) < 0
        assertTrue m4.compareTo(m5) > 0
        assertTrue m4.compareTo(m6) > 0
        assertTrue m4.compareTo(m7) > 0
        assertTrue m4.compareTo(m8) > 0
        assertTrue m4.compareTo(m9) > 0
        assertTrue m4.compareTo(m10) > 0

        assertTrue m5.compareTo(m1) < 0
        assertTrue m5.compareTo(m2) < 0
        assertTrue m5.compareTo(m3) < 0
        assertTrue m5.compareTo(m4) < 0
        assertTrue m5.compareTo(m6) > 0
        assertTrue m5.compareTo(m7) > 0
        assertTrue m5.compareTo(m8) > 0
        assertTrue m5.compareTo(m9) > 0
        assertTrue m5.compareTo(m10) > 0

        assertTrue m6.compareTo(m1) < 0
        assertTrue m6.compareTo(m2) < 0
        assertTrue m6.compareTo(m3) < 0
        assertTrue m6.compareTo(m4) < 0
        assertTrue m6.compareTo(m5) < 0
        assertTrue m6.compareTo(m7) > 0
        assertTrue m6.compareTo(m8) > 0
        assertTrue m6.compareTo(m9) > 0
        assertTrue m6.compareTo(m10) > 0

        assertTrue m7.compareTo(m1) < 0
        assertTrue m7.compareTo(m2) < 0
        assertTrue m7.compareTo(m3) < 0
        assertTrue m7.compareTo(m4) < 0
        assertTrue m7.compareTo(m5) < 0
        assertTrue m7.compareTo(m6) < 0
        assertTrue m7.compareTo(m8) > 0
        assertTrue m7.compareTo(m9) > 0
        assertTrue m7.compareTo(m10) > 0

        assertTrue m8.compareTo(m1) < 0
        assertTrue m8.compareTo(m2) < 0
        assertTrue m8.compareTo(m3) < 0
        assertTrue m8.compareTo(m4) < 0
        assertTrue m8.compareTo(m5) < 0
        assertTrue m8.compareTo(m6) < 0
        assertTrue m8.compareTo(m7) < 0
        assertTrue m8.compareTo(m9) > 0
        assertTrue m8.compareTo(m10) > 0

        assertTrue m9.compareTo(m1) < 0
        assertTrue m9.compareTo(m2) < 0
        assertTrue m9.compareTo(m3) < 0
        assertTrue m9.compareTo(m4) < 0
        assertTrue m9.compareTo(m5) < 0
        assertTrue m9.compareTo(m6) < 0
        assertTrue m9.compareTo(m7) < 0
        assertTrue m9.compareTo(m8) < 0
        assertTrue m9.compareTo(m10) > 0

        assertTrue m10.compareTo(m1) < 0
        assertTrue m10.compareTo(m2) < 0
        assertTrue m10.compareTo(m3) < 0
        assertTrue m10.compareTo(m4) < 0
        assertTrue m10.compareTo(m5) < 0
        assertTrue m10.compareTo(m6) < 0
        assertTrue m10.compareTo(m7) < 0
        assertTrue m10.compareTo(m8) < 0
        assertTrue m10.compareTo(m9) < 0

        def correctOrder = [m1, m2, m3, m4, m5, m6, m7, m8, m9, m10]

        // urls in completely reverse order
        def urls = [m10, m9, m8, m7, m6, m5, m4, m3, m2, m1]
        Collections.sort(urls)
        Collections.reverse(urls)
        assertEquals(correctOrder, urls)

        // urls in random order
        urls = [m3, m9, m5, m8, m4, m10, m7, m2, m1, m6]
        Collections.sort(urls)
        Collections.reverse(urls)
        assertEquals(correctOrder, urls)

    }

    void testMatchUriWithConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "hello", String.class)
        cp.nullable = false

        // mapping would be "/foo/$hello/bar
        def parser = new DefaultUrlMappingParser()

        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", null, [cp] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/world/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        println info.parameters
        assertEquals "world", info.parameters.hello

    }

    void testMatchUriWithMatchesConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "year", String.class)
        cp.matches = /\d{4}/

        // mapping would be "/foo/$hello/bar
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", null, [cp] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/2007/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        println info.parameters
        assertEquals "2007", info.parameters.year

        info = m.match("/foo/blah/bar")
        assertNull info
    }


    void testInit() {
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse("/(*)/hello"), "test", null, null, [] as ConstrainedProperty[], servletContext)

    }

    void testMatchUriNoConstraints() {
        def parser = new DefaultUrlMappingParser()
        def m = new RegexUrlMapping(parser.parse("/foo/(*)/bar"), "test", null, null, [] as ConstrainedProperty[], servletContext)

        def info = m.match("/foo/test/bar")
        assert info
        assertEquals "test", info.controllerName

        info = m.match("/foo/bar/test")
        assertNull info
    }
}
