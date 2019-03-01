package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class IdUrlMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {


    void testIdInURL() {

        when:
        def infos = urlMappingsHolder.matchAll("/emailConfirmation/foo")
        assert infos
        infos[0].configure(webRequest)

        def c = new EmailConfirmationController()

        then:
        "foo" == c.params.id
    }

    void testIdInParam() {
        when:
        assert webRequest

        def infos = urlMappingsHolder.matchAll("/emailConfirmation/foo")
        assert infos

        infos[0].configure(webRequest)

        def c = new EmailConfirmationController()

        then:
        "foo" == c.params.id
    }

    void testMappingWithUrlEncodedCharsInId() {
        when:
        assert webRequest

        def infos = urlMappingsHolder.matchAll("/emailConfirmation/my%20foo")
        assert infos

        infos[0].configure(webRequest)

        def c = new EmailConfirmationController()

        then:
        "my foo" == c.params.id

        when:
        infos = urlMappingsHolder.matchAll("/emailConfirmation/my%2Ffoo")
        assert infos
        infos[0].configure(webRequest)

        c = new EmailConfirmationController()

        then:
        "my/foo" == c.params.id
    }

    static class UrlMappings {
        static mappings = {
            "/emailConfirmation/$id?" {
                controller = "emailConfirmation"
                action = "index"
            }
            "/$id?" {
                controller = "content"
                action = "index"
            }
        }
    }
}
@grails.artefact.Artefact('Controller')
class EmailConfirmationController {
    def index() {
        [result: "ID = " + params.id]
    }
}
class ContentController {
    def index() {}
}