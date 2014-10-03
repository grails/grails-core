package org.grails.web.mapping

import grails.util.GrailsWebMockUtil

import org.springframework.core.io.ByteArrayResource

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class OverlappingUrlMappingTests extends AbstractGrailsMappingTests {

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
        GrailsWebMockUtil.bindMockWebRequest()

        def res = new ByteArrayResource(mappingScript.bytes)
        def holder = new DefaultUrlMappingsHolder(evaluator.evaluateMappings(res))

        Map params = [id: "contact"]
        def reverse = holder.getReverseMapping("content", "view", params)

        assertEquals "/contact", reverse.createURL(params, "utf-8")

        params.dir = "fred"
        reverse = holder.getReverseMapping("content", "view", params)
        assertEquals "/contact/fred", reverse.createURL(params, "utf-8")
    }
}
