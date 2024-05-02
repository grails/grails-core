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
package grails.test.web

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Issue
import spock.lang.Specification

class FordedUrlSpec extends Specification implements ControllerUnitTest<DemoController> {

    @Issue('GRAILS-11673')
    void 'test forwardedUrl when forward is called'() {
        when:
        controller.firstAction()
        
        then:
        response.forwardedUrl == '/demo/secondAction'
    }

    @Issue('GRAILS-11673')
    void 'test forwardedUrl when forward is not called'() {
        when:
        controller.secondAction()
        
        then:
        response.forwardedUrl == null
    }
}


@Artefact('Controller')
class DemoController {
    def firstAction() {
        forward action: 'secondAction'
    }
    
    def secondAction() {}
}
