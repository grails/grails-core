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

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Issue
import spock.lang.Specification

class TestForControllerWithNamePropertySpec extends Specification implements ControllerUnitTest<SomeController> {

    @Issue('grails/grails-core#10363')
    void "test referencing a controller with a 'name' property"() {
        when:
        controller

        then:
        notThrown ClassCastException
    }
}

@Artefact('Controller')
class SomeController {
    String name
}
