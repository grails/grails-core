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
package grails.web.mapping
/**
 * @author Graeme Rocher
 */
class RegisterUrlMappingsAtRuntimeSpec extends AbstractUrlMappingsSpec{

    void "Test registering new URL mappings at runtime"() {
        given:"A UrlMappings instance"
            UrlMappings urlMappings = getUrlMappingsHolder {
                "/foo"(controller:"foo")
            }

        when:"The mappings are obtained"
            def mappings = urlMappings.urlMappings

        then:"There is only a single mapping"
            mappings.size() == 1

        when:"A new mapping is registered"
            urlMappings.addMappings {
                "/bar"(controller: "bar")
            }
            mappings = urlMappings.urlMappings

        then:"A new mapping exists"
            mappings.size() == 2
            urlMappings.match('/bar')
            urlMappings.match('/foo')
    }
}
