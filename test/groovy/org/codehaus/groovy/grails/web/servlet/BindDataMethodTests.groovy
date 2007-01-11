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

import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * Tests for the bindData method
 *
 * @author Marc Palmer
 *
 */
class BindDataMethodTests extends AbstractGrailsControllerTests {

	void testBindDataFromMap() {
	    runTest() {
            def mockController = ga.getController("BindController")

            def method = new BindDynamicMethod()

            def target = new CommandObject()
            def safeMeta = target.metaClass
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer' ]

            method.invoke( mockController, [target, src].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
        }
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
