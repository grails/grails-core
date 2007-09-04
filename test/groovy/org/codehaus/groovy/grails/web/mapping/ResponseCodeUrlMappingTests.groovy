/**
 * @author mike
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

class ResponseCodeUrlMappingTests extends AbstractGrailsControllerTests {
    def topLevelMapping = '''
mappings {
    "404"{
        controller = "sustem"
        action = "notfound"
    }
}
'''
    def holder


    void setUp() {
/*
        super.setUp()
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
*/
    }


    void testSimple() {
//        assertNotNull holder
    }
}