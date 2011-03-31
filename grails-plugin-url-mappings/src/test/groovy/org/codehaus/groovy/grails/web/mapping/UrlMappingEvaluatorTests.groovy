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

  "/myFiles/something-$fname.$fext" {
	  controller = "files"
  }

  "/long/$path**"(controller: 'files')
}
'''
    
    void testEvaluateMappings() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(res)

        assert mappings
        assertEquals 7, mappings.size()

        def m = mappings[0]
        assertEquals "/(*)/(*)?/(*)?/(*)?", m.urlData.urlPattern
        assertEquals 4, m.urlData.tokens.size()


        def info = m.match("/myentry/2007/04/28")

        assertEquals "myentry", info.id
        assertEquals "2007", info.parameters.year
        assertEquals "04", info.parameters.month
        assertEquals "28", info.parameters.day
        assertEquals "blog", info.controllerName
        assertEquals "show", info.actionName


        assert m.match("/myentry/2007/04/28")
        assert m.match("/myentry/2007/04")
        assert m.match("/myentry/2007")
        assert m.match("/myentry")


        m = mappings[1]
        info = m.match("/product/MacBook")
        assert info
        assertEquals "MacBook", info.parameters.name

        assert !m.match("/product")
        assert !m.match("/foo/bar")
        assert !m.match("/product/MacBook/foo")

        m = mappings[3]
        info = m.match("/author/Brown/Jeff")
        assert info
        assertEquals "Brown", info.parameters.lastName
        assertEquals "Jeff", info.parameters.firstName
        assertEquals "show", info.actionName
        assertEquals "author", info.controllerName

        // first name too long
        assert !m.match("/author/Lang/Johnny")

        // both names too long
        assert !m.match("/author/Winter/Johnny")

        // last name too long
        assert !m.match("/author/Winter/Edgar")

        info = mappings[4].match("/music/Rush/Hemispheres")
        assert info
        assertEquals "Rush", info.parameters.band
        assertEquals "Hemispheres", info.parameters.album
        assertEquals "show", info.actionName
        assertEquals "music", info.controllerName

        info = mappings[5].match("/myFiles/something-hello.txt")
        assert info
        assertEquals "files", info.controllerName
        assertEquals "hello", info.parameters.fname
        assertEquals "txt", info.parameters.fext

        // Test the double-wildcard, "**".
        info = mappings[6].match("/long/path/to/some/file")
        assert info
        assertEquals "path/to/some/file", info.parameters.path
        assertEquals "files", info.controllerName
    }
}
