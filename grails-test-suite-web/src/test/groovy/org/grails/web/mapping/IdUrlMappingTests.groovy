package org.grails.web.mapping

import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.core.io.ByteArrayResource

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class IdUrlMappingTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
        "/emailConfirmation/$id?" {
            controller = "emailConfirmation"
            action = "index"
        }
        "/$id?" {
            controller = "content"
            action = "index"
        }
}
'''

    void onSetUp() {
        gcl.parseClass('''
@grails.artefact.Artefact('Controller')
class EmailConfirmationController {
    def index = {
        [result: "ID = " + params.id]
     }
}
class ContentController {
    def index = {}
}
        ''')
    }

    void testIdInURL() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest

        def infos = holder.matchAll("/emailConfirmation/foo")
        assert infos

        infos[0].configure(webRequest)

        def c = ga.getControllerClass("EmailConfirmationController").newInstance()

        assertEquals "foo", c.params.id
    }

    void testIdInParam() {

        def res = new ByteArrayResource(mappingScript.bytes)

        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest

        request.addParameter("id", "foo")
        def infos = holder.matchAll("/emailConfirmation")
        assert infos

        infos[0].configure(webRequest)

        def c = ga.getControllerClass("EmailConfirmationController").newInstance()

        assertEquals "foo", c.params.id
    }

    void testMappingWithUrlEncodedCharsInId() {
        def res = new ByteArrayResource(mappingScript.bytes)

        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)
        assert webRequest

        def infos = holder.matchAll("/emailConfirmation/my%20foo")
        assert infos

        infos[0].configure(webRequest)

        def c = ga.getControllerClass("EmailConfirmationController").newInstance()

        assertEquals "my foo", c.params.id

        infos = holder.matchAll("/emailConfirmation/my%2Ffoo")
        assert infos

        infos[0].configure(webRequest)

        c = ga.getControllerClass("EmailConfirmationController").newInstance()

        assertEquals "my/foo", c.params.id
    }
}
