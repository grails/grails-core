/* Copyright 2004-2005 Graeme Rocher
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

package org.codehaus.groovy.grails.web.servlet

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.mock.web.MockHttpServletRequest

/**
*  @author Graeme Rocher
*/
class FlashScopeWithErrorsTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getDomainClasses() {
        [Book]
    }

    void testFlashScopeWithErrors() {
        def b = new Book()

        b.validate()
        def flash = new GrailsFlashScope()

        flash.book = b
        assert b.hasErrors()

        GrailsWebUtil.bindMockWebRequest()
        flash.next()

        assert flash.book
        assert flash.book.hasErrors()
    }
}

class Book {
    Long id
    Long version
    String title
    URL site
}
