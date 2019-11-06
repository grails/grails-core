package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import grails.web.mapping.UrlCreator
import org.springframework.core.io.ByteArrayResource
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class AdditionalParamsMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    void testMapping() {
        when:
        UrlCreator creator = urlMappingsHolder.getReverseMapping("user", "profile",[id:"bob"])

        then:
        "/users/bob/profile" == creator.createRelativeURL("user", "profile",[id:"bob"], "utf-8")

        when:
        creator = urlMappingsHolder.getReverseMapping("user", "profile",[id:"bob", q:"test"])

        then:
        "/users/bob/profile?q=test" == creator.createRelativeURL("user", "profile",[id:"bob",q:"test"], "utf-8")
    }

    static class UrlMappings {
        static mappings = {
            "/$controller/$action?/$id?"{
                constraints {
                    // apply constraints here
                }
            }

            "/users/$id?/$action?" {
                controller = "user"
            }

        }
    }
}