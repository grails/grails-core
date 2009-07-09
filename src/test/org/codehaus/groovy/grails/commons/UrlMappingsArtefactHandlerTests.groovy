package org.codehaus.groovy.grails.commons;

import org.springframework.core.io.*

/**
 * tests for UrlMappingsArtefactHandler
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 6:20:30 PM
 */
public class UrlMappingsArtefactHandlerTests extends GroovyTestCase {

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
