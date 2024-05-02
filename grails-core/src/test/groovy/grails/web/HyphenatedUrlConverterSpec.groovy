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

class HyphenatedUrlConverterSpec extends Specification {

    @Unroll("converting #classOrActionName to url element #expectedUrlElement")
    def 'Test converting class and action names to url elements'() {
        given:
            def converter = new HyphenatedUrlConverter()

        expect:
            converter.toUrlElement(classOrActionName) == expectedUrlElement

        where:
            classOrActionName      | expectedUrlElement
            'Widget'               | 'widget'
            'widget'               | 'widget'
            'MyWidget'             | 'my-widget'
            'myWidget'             | 'my-widget'
            'A'                    | 'a'
            'a'                    | 'a'
            'MyMultiWordClassName' | 'my-multi-word-class-name'
            'myMultiWordClassName' | 'my-multi-word-class-name'
            'MyUrlHelper'          | 'my-url-helper'
            'myUrlHelper'          | 'my-url-helper'
            'MyURLHelper'          | 'my-u-r-l-helper'
            'myURLHelper'          | 'my-u-r-l-helper'
            'MYUrlHelper'          | 'm-y-url-helper'
            'myNamespace.v1'       | 'my-namespace.v1'
            'MyNamespace.v1'       | 'my-namespace.v1'
            'MyNamespace.V1'       | 'my-namespace.v1'
            ''                     | ''
            null                   | null
    }
}
