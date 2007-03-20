package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*

class RegexUrlMappingTests extends GroovyTestCase {

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
}
'''

    void testCreateUrlFromMapping() {
             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)


             def m = mappings[0]
             assert m

             assertEquals "/book/dierk/gina/foo", m.createURL(author:"dierk", title:"gina", test:"foo")

             m = mappings[1]
             assert m

             assertEquals "/blog/foo/2007/10/24", m.createURL(entry:"foo", year:2007, month:10, day:24)
             assertEquals "/blog/foo/2007/10", m.createURL(entry:"foo", year:2007, month:10)
             assertEquals "/blog/foo/2007", m.createURL(entry:"foo", year:2007)
             assertEquals "/blog/foo", m.createURL(entry:"foo")
             shouldFail { m.createURL([:]) }
    }

    void testComparable() {
        def parser = new DefaultUrlMappingParser()
        def m1 = new RegexUrlMapping(parser.parse("/foo"), "test", null)
        def m2 = new RegexUrlMapping(parser.parse("/foo/(*)"), "test", null)
        def m3 = new RegexUrlMapping(parser.parse("/foo/(*)/bar"), "test", null)
        def m4 = new RegexUrlMapping(parser.parse("/(*)/foo/bar"), "test", null)
        def m5 = new RegexUrlMapping(parser.parse("/foo/bar/(*)"), "test", null)
        def m6 = new RegexUrlMapping(parser.parse("/(*)/(*)/bar"), "test", null)
        def m7 = new RegexUrlMapping(parser.parse("/foo/(*)/(*)"), "test", null)
        def m8 = new RegexUrlMapping(parser.parse("/(*)/(*)/(*)"), "test", null)
        def m9 = new RegexUrlMapping(parser.parse("/"), "test", null)

        // root url
        assertEquals( -1, m9.compareTo(m2) )
        assertEquals( -1, m9.compareTo(m3))
        assertEquals( -1, m9.compareTo(m4))
        assertEquals( -1, m9.compareTo(m5))
        assertEquals( -1, m9.compareTo(m6))
        assertEquals( -1, m9.compareTo(m7))

        assertEquals( 1, m2.compareTo(m9) )
        assertEquals( 1, m3.compareTo(m9))
        assertEquals( 1, m4.compareTo(m9))
        assertEquals( 1, m5.compareTo(m9))
        assertEquals( 1, m6.compareTo(m9))
        assertEquals( 1, m7.compareTo(m9))

        def urls = [m9,m2,m5,m7,m3,m4,m8]
        Collections.sort(urls)
        Collections.reverse(urls)
        println urls.inspect()
        assertEquals m9, urls[-1]

        // url 1
        assertEquals( -1, m1.compareTo(m2) )
        assertEquals( -1, m1.compareTo(m3))
        assertEquals( -1, m1.compareTo(m4))
        assertEquals( -1, m1.compareTo(m5))
        assertEquals( -1, m1.compareTo(m6))
        assertEquals( -1, m1.compareTo(m6))

        // url 2
        assertEquals( 1, m2.compareTo(m1))
        assertEquals( -1, m2.compareTo(m3))
        assertEquals( -1, m2.compareTo(m4))
        assertEquals( -1, m2.compareTo(m5))
        assertEquals( -1, m2.compareTo(m6))
        assertEquals( -1, m2.compareTo(m7))
        assertEquals( -1, m2.compareTo(m8))


        // url 3
        assertEquals 1, m3.compareTo(m1)
        assertEquals 1, m3.compareTo(m2)
        assertEquals 1, m3.compareTo(m4)
        assertEquals 1, m3.compareTo(m6)
        assertEquals 1, m3.compareTo(m8)
        assertEquals(-1, m3.compareTo(m5))
        assertEquals(1, m3.compareTo(m7))
        

        // url 4
        assertEquals( 1, m4.compareTo(m1))
        assertEquals( 1, m4.compareTo(m2))
        assertEquals( -1, m4.compareTo(m3))
        assertEquals( -1, m4.compareTo(m5))
        assertEquals( 1, m4.compareTo(m6))
        assertEquals( -1, m4.compareTo(m7))
        assertEquals( 1, m4.compareTo(m8))
    }

    void testMatchUriWithConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "hello", String.class)
        cp.nullable = false

        // mapping would be "/foo/$hello/bar
        def parser = new DefaultUrlMappingParser()

        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", [cp] as ConstrainedProperty[])

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
        def m = new RegexUrlMapping(parser.parse('/foo/(*)/bar'), "test", "action", [cp] as ConstrainedProperty[])

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
        def m = new RegexUrlMapping(parser.parse("/(*)/hello"), "test", [] as ConstrainedProperty[])

    }

    void testMatchUriNoConstraints() {
        def parser = new DefaultUrlMappingParser()  
        def m = new RegexUrlMapping(parser.parse("/foo/(*)/bar"), "test", [] as ConstrainedProperty[])

        def info = m.match("/foo/test/bar")
        assert info
        assertEquals "test", info.controllerName

        info = m.match("/foo/bar/test")
        assertNull info
    }


}

