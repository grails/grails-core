/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.plugins.xml

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class UnmanagedControllerXmlSpec extends Specification implements ControllerUnitTest<UnmanagedXmlController> {

    void 'a controller Spring did not create responds in XML with the renderers of the application'() {
        given: 'a controller created with new, which Spring does not autowire'
        def controller = new UnmanagedXmlController()
        request.addHeader('Accept', 'application/xml')

        when:
        controller.respond(new UnmanagedXmlBody(title: 'Grails'))

        then:
        response.status == 200
        response.contentType.startsWith('application/xml')
        response.xml.title.text() == 'Grails'
    }
}

@Artefact('Controller')
class UnmanagedXmlController { }

class UnmanagedXmlBody {
    String title
}
