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
package org.grails.compiler.web

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class WithFormatSpec extends Specification implements ControllerUnitTest<MimeTypesCompiledController> {

    void "Test withFormat method injected at compile time"() {
        when:
        response.format = 'html'
        def format = controller.index()

        then:
        format == "html"
        
        when:
        response.format = 'xml'
        format = controller.index()
        
        then:
        format == 'xml'
    }
}

@Artefact('Controller')
class MimeTypesCompiledController {
    def index() {
        withFormat {
            html { "html" }
            xml { "xml" }
        }
    }
}

