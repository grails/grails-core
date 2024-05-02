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
package grails.web

import spock.lang.Specification
import spock.lang.Unroll

class CamelCaseUrlConverterSpec extends Specification {

    @Unroll("converting #classOrActionName to url element #expectedUrlElement")
    def 'Test converting class and action names to url elements'() {
        given:
            def converter = new CamelCaseUrlConverter()

        expect:
            converter.toUrlElement(classOrActionName) == expectedUrlElement

        where:
            classOrActionName      | expectedUrlElement
            'Widget'               | 'widget'
            'widget'               | 'widget'
            'MyWidget'             | 'myWidget'
            'myWidget'             | 'myWidget'
            'A'                    | 'a'
            'a'                    | 'a'
            'MyMultiWordClassName' | 'myMultiWordClassName'
            'myMultiWordClassName' | 'myMultiWordClassName'
            'MyUrlHelper'          | 'myUrlHelper'
            'myUrlHelper'          | 'myUrlHelper'
            'MyURLHelper'          | 'myURLHelper'
            'myURLHelper'          | 'myURLHelper'
            'MYUrlHelper'          | 'MYUrlHelper'
            'myNamespace.v1'       | 'myNamespace.v1'
            'MyNamespace.v1'       | 'myNamespace.v1'
            'MyNamespace.V1'       | 'myNamespace.v1'
            ''                     | ''
            null                   | null
    }
}
