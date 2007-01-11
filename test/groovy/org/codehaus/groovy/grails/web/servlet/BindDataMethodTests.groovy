/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

/**
 * Tests for the bindData method
 *
 * @author Marc Palmer
 *
 */
class BindDataMethodTests extends AbstractControllerTests {

	void testBindDataFromMap() {
	    def mockController = getMockController("BindController")

		def mockRequest = new MockHttpServletRequest()
		def mockResponse = new MockHttpServletResponse()

        def method = new BindDynamicMethod(mockRequest, mockResponse)

        def target = new CommandObject()
        def safeMeta = target.metaClass
        def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer' ]

        method.invoke( mockController, [target, src].toArray() )
        
		assertEquals "Marc Palmer", target.name
		assertEquals safeMeta, target.metaClass
	}

	void onSetUp() {
		gcl.parseClass(
'''
class BindController {
}
'''
        )
    }
}

class CommandObject {
    String name
}
