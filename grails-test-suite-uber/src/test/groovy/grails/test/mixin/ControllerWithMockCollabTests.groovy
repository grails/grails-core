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
import org.junit.Test
import spock.lang.Specification

/**
 * Tests a controller with a mock collaborator
 */
class ControllerWithMockCollabTests extends Specification implements ControllerUnitTest<ControllerWithCollabController> {

    void testFirstCall() {
        when:
        executeCallTest()

        then:
        noExceptionThrown()
    }

    void testSecondCall() {
        when:
        executeCallTest()

        then:
        noExceptionThrown()
    }

    private void executeCallTest() {
        //Prepare
        boolean called = false
        controller.myCallable = [callMe: { called = true }]

        //Call
        controller.index()

        //Check
        assert response.status == 200
        assert called == true
    }
}

@Artefact("Controller")
class ControllerWithCollabController {

    def myCallable

    def index() {
        myCallable.callMe()
    }
}
