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

package grails.web.mapping
/**
 * @author graemerocher
 */
class UrlMappingSizeConstraintSpec extends AbstractUrlMappingsSpec {

    void "Test that a size constraint can be applied"() {

        given:
        def urlMappingsHolder = getUrlMappingsHolder {
            "/test/test/$size?" {
                controller = "test"
                action = "test"

                constraints {
                    size range: 100..1000
                }
            }
        }

        when:
        def infos = urlMappingsHolder.matchAll("/test/test/101")

        then:
        infos
        infos[0].parameters
    }
}
