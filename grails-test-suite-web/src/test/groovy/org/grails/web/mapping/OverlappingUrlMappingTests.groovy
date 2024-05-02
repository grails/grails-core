/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import grails.util.GrailsWebMockUtil

import org.springframework.core.io.ByteArrayResource
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class OverlappingUrlMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {


    void testEvaluateMappings() {

        when:
        Map params = [id: "contact"]
        def reverse = urlMappingsHolder.getReverseMapping("content", "view", params)

        then:
        "/contact" == reverse.createURL(params, "utf-8")


        when:
        params.dir = "fred"
        reverse = urlMappingsHolder.getReverseMapping("content", "view", params)


        then:
        "/contact/fred" == reverse.createURL(params, "utf-8")
    }

    static class UrlMappings {
        static mappings = {
            "/$id?" {
                controller = "content"
                action = "view"
            }
            "/$id/$dir" {
                controller = "content"
                action = "view"
            }
        }
    }
}
