/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.executor.support
/**
 * Tests for GrailsConventionsFlowExecutorArgumentHandler
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jul 3, 2007
 * Time: 9:13:14 AM
 * 
 */

import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder

class GrailsConventionsFlowExecutorArgumentsHandlerTests extends GroovyTestCase {

    def webRequest
    void setUp() {
        webRequest = GrailsWebUtil.bindMockWebRequest()
    }
    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void testFlowExecutorArgumentsHandler() {

        def argHandler = new GrailsConventionsFlowExecutorArgumentHandler(webRequest)
        assert !argHandler.isFlowIdPresent(null)

        webRequest.actionName = "foo"

        assert argHandler.isFlowIdPresent(null)
        assertEquals "foo", argHandler.extractFlowId(null)
    }

}