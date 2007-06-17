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
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * Tests for the bindData method
 *
 * @author Marc Palmer
 *
 */
class BindDataMethodTests extends AbstractGrailsControllerTests {
    def mockController
    def method
    def target
    def safeMeta

	void testBindDataFromMap() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer' ]

            method.invoke( mockController, "bindData", [target, src].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
        }
	}

	void testBindDataWithExcluded() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer', 'email' : 'dontwantthis' ]
            def excludes = [exclude:['email']]

            method.invoke( mockController,"bindData", [target, src, excludes].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
            assertNull target.email
        }
	}

	void testBindDataWithIncluded() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer', 'email' : 'dontwantthis' ]
            def includes = [include:['name']]

            method.invoke( mockController,"bindData", [target, src, includes].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
            assertNull target.email
        }
	}

	void testBindDataWithNeitherIncludeOrExcludeIncludesAll() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer', 'email' : 'dowantthis' ]
            method.invoke( mockController,"bindData", [target, src, [:]].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
            assertEquals target.email, 'dowantthis'
        }
	}

	void testBindDataExcludedOverridesIncluded() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer', 'email' : 'dontwantthis' ]
            def includedAndExcluded = [include:['name','email'], exclude:['email']]

            method.invoke( mockController,"bindData", [target, src, includedAndExcluded].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
            assertNull target.email
        }
	}

	void testBindDataWithPrefixFilter() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'mark.name' : 'Marc Palmer', 'mark.email' : 'dontwantthis',
                        'lee.name': 'Lee Butts', 'lee.email': 'lee@mail.com']
            def filter = "lee"
            method.invoke( mockController,"bindData", [target, src, filter].toArray() )
            assertEquals "Lee Butts", target.name
            assertEquals "lee@mail.com", target.email
            assertEquals safeMeta, target.metaClass
        }
	}

    void testBindDataWithDisallowedWithGrailsParameterMap() {
        runTest() {
            mockController = ga.getControllerClass("BindController")
            def input = [ 'metaClass' : this.metaClass, 'name' : 'Marc Palmer', 'email' : 'dontwantthis',
              'address.country':'gbr' ]
            input.each() {
                webRequest.currentRequest.addParameter((String)it.key, (String)it.value)
            }
            def excludes = [exclude:['email']]
            def params = new GrailsParameterMap(webRequest.currentRequest)

            method.invoke( mockController,"bindData", [target, params, excludes].toArray() )

            assertEquals "Marc Palmer", target.name
            assertEquals safeMeta, target.metaClass
            assertEquals "gbr", target.address.country
            assertNull target.email
        }
    }

    void testBindDataWithPrefixFilterAndDisallowed() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'mark.name' : 'Marc Palmer', 'mark.email' : 'dontwantthis',
                        'lee.name': 'Lee Butts', 'lee.email': 'lee@mail.com']
            def filter = "lee"
            def disallowed = [exclude:["email"]]
            method.invoke( mockController,"bindData", [target, src, disallowed, filter].toArray() )
            assertEquals "Lee Butts", target.name
            assertNull target.email
            assertEquals safeMeta, target.metaClass
        }
	}

	 void testBindDataConvertsSingleStringInMapToList() {
	    runTest() {
            mockController = ga.getControllerClass("BindController")
            def src = [ 'metaClass' : this.metaClass, 'mark.name' : 'Marc Palmer', 'mark.email' : 'dontwantthis',
                        'lee.name': 'Lee Butts', 'lee.email': 'lee@mail.com']
            def filter = "lee"
            def disallowed = [exclude:"email"]
            method.invoke( mockController,"bindData", [target, src, disallowed, filter].toArray() )
            assertEquals "Lee Butts", target.name
            assertNull target.email
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
        method = new BindDynamicMethod()
        target = new CommandObject()
        safeMeta = target.metaClass
    }
}

class CommandObject {
    String name
    String email
    Address address = new Address()
}

class Address {
    String country
}