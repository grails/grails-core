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

package org.grails.web.servlet

import grails.persistence.Entity
import grails.test.mixin.TestFor
import grails.util.GrailsWebMockUtil

import org.junit.Test

/**
*  @author Graeme Rocher
*/
@TestFor(Book)
class FlashScopeWithErrorsTests  {

    @Test
    void testFlashScopeWithErrors() {
        GrailsWebMockUtil.bindMockWebRequest()

        def b = new Book()

        b.validate()
        assert b.hasErrors()

        def flash = new GrailsFlashScope()

        flash.book = b

        flash.next()

        assert flash.book
        assert flash.book.hasErrors()

        flash.next()

        assert !flash.book
    }
}

@Entity
class Book {
    String title
    URL site
}
