/*
 * Copyright 2012 the original author or authors.
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

import org.junit.Test
import org.junit.Before

/**
 */
@TestFor(FirstController)
class SetupTeardownInvokeTests {

    void setUp() {
        controller.value = 'World!'
    }

    void tearDown() {
        controller.counter++
    }

    @Test
    void testThatSetupWasInvoked() {
        assert controller.value == 'World!'
    }

    @Test
    void testThatSetupWasInvoked2() {
        assert controller.counter == 1
    }
}
class FirstController {
    String value
    static int counter = 0
}
