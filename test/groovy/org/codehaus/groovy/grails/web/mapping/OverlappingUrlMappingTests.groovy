/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Aug 9, 2007
 * Time: 7:04:11 PM
 * 
 */

package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.ByteArrayResource
import grails.util.GrailsWebUtil

class OverlappingUrlMappingTests extends GroovyTestCase {

    def mappingScript = '''
mappings {
    "/$id?" {
        controller = "content"
        action = "view"
    }
    "/$id/$dir" {
        controller = "content"
        action = "view"
    }
}
'''

    void testEvaluateMappings() {
        GrailsWebUtil.bindMockWebRequest()
        
         def res = new ByteArrayResource(mappingScript.bytes)

         def evaluator = new DefaultUrlMappingEvaluator()
         def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(res))

         Map params = [id: "contact"]
        def reverse = holder.getReverseMapping("content", "view", params)

         assertEquals "/contact", reverse.createURL(params,"utf-8")

         params.dir = "fred"
         reverse = holder.getReverseMapping("content", "view", params)
         assertEquals "/contact/fred", reverse.createURL(params,"utf-8")

    }
}