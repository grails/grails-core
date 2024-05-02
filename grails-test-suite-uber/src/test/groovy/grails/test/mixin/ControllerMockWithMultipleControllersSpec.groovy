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
package grails.test.mixin

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification

class ControllerMockWithMultipleControllersSpec extends Specification implements GrailsWebUnitTest {

    void "Test that both mocked controllers are valid"() {
        given:
        mockController(FirstController)
        mockController(SimpleController)

        when:"Two mock controllers are created"
            def c1 = new FirstController()
            def c2 = new SimpleController()

        then:"The request context is accessible from both"
            c1.request != null
            c2.request != null
    }
}
