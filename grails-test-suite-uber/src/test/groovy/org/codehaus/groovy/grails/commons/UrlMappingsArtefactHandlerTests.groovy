package org.codehaus.groovy.grails.commons;

import org.springframework.core.io.*

/**
 * Tests for UrlMappingsArtefactHandler.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
class UrlMappingsArtefactHandlerTests extends GroovyTestCase {

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
}
'''

    void testUrlMappingsArtefactHandler() {
        def gcl = new GroovyClassLoader()
        Class mappings = gcl.parseClass(new ByteArrayResource(mappingScript.bytes).inputStream, "MyUrlMappings")
        def handler = new UrlMappingsArtefactHandler()

        assert handler.isArtefactClass(mappings)
        assert handler.newArtefactClass(mappings)
    }
}
