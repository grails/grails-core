package org.codehaus.groovy.grails.web.mapping

import spock.lang.Issue

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
class GroupedUrlMappingSpec extends AbstractUrlMappingsSpec {

    @Issue('#9138')
    void "Test that group parameters are included in generated links"() {
        given: "A link generator with a dynamic URL mapping"
        def linkGenerator = getLinkGenerator {
            group "/events/$alias", {
                "/"(controller: 'test', action: 'index')
                "/orders/$id"(controller: 'test', action: 'show')
            }
        }

        expect:
        linkGenerator.link(controller: "test", action: 'index', params: [alias: 'foo']) == 'http://localhost/events/foo'
        linkGenerator.link(controller: "test", action: 'show', id: 1, params: [alias: 'foo']) == 'http://localhost/events/foo/orders/1'
    }

    @Issue('#9426')
    void "Test that constraints embedded within groups are properly respected"() {
        given: "A group with a child URL that contains a constraint"
        def urlMappingsHolder = getUrlMappingsHolder {
            group "/group", {
                "/$id/$page?" {
                    controller = "test"
                    action = "index"
                    constraints {
                        id(matches: /\d+/)
                        page(matches: /\d+/)
                    }
                }
            }
        }

        when: 'Attempting to match some urls to this group'
        def goodMatch_full = urlMappingsHolder.matchAll("/group/1/2")
        def goodMatch_optional = urlMappingsHolder.matchAll("/group/1")
        def badMatch = urlMappingsHolder.matchAll("/group/1/char")

        then: 'Should succeed'
        goodMatch_full
        goodMatch_optional
        !badMatch
    }
}