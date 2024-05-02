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
package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ControllerInheritanceTests extends Specification implements ControllerUnitTest<ControllerInheritanceFooController> {
    // test for GRAILS-6247
    void testCallSuperMethod() {
        when:
        controller.bar()

        then:
        noExceptionThrown()
    }
}

@Artefact('Controller')
class ControllerInheritanceFooBaseController {

    void bar() {
        println('bar in base class')
    }
}

@Artefact('Controller')
class ControllerInheritanceFooController extends ControllerInheritanceFooBaseController {}

