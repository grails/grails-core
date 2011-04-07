/* Copyright 2008 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.testing

import grails.converters.JSON
import groovy.util.slurpersupport.GPathResult
import org.springframework.mock.web.MockHttpServletResponse
import org.codehaus.groovy.grails.web.json.JSONElement

/**
 * Simple sub-class of Spring's MockHttpServletResponse that adds the
 * left-shift operator, "<<".
 */
class GrailsMockHttpServletResponse extends MockHttpServletResponse {
    /**
     * Appends the given content string to the response's output stream.
     */
    void leftShift(String content) {
        writer << content
    }

    /**
     * Get the response XML
     *
     * @return The response XML
     */
    GPathResult getXml() {
        new XmlSlurper().parseText(contentAsString)
    }

    /**
     * Get the response JSON
     *
     * @return  The JSON response
     */
    JSONElement getJson() {
        JSON.parse(contentAsString)
    }

    @Override
    void reset() {
        setCommitted(false)
        super.reset()
    }


}
