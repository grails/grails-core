/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 7, 2008
 */
package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class UrlMappingWithCustomValidatorTests extends AbstractGrailsControllerTests {
   def topLevelMapping = '''
mappings {
    "/help/$path**"(controller : "wiki", action : "show", id : "1") {
        constraints {
            path(validator : { val, obj -> ! val.startsWith("js") })
        }
    }
}
'''
    def UrlMappingsHolder holder


    void setUp() {
        super.setUp()
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    void testMatchWithCustomValidator() {
        def info = holder.match("/help/foo.html")

        assert info

        info = holder.match("/help/js/foo.js")

        assert !info
    }
}