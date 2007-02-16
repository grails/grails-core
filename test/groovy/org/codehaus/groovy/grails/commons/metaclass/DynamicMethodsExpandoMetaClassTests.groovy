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
package org.codehaus.groovy.grails.commons.metaclass;

import org.springframework.beans.BeanUtils

/**
 * @author Graeme Rocher
 */

class DynamicMethodsExpandoMetaClassTests extends GroovyTestCase {

	void testRegexMethodDefinition() {
       def metaClass = new DynamicMethodsExpandoMetaClass(Book.class)
       metaClass.initialize()

       metaClass./^findBy(\w+)$/ = { matcher, args ->
            assert matcher instanceof java.util.regex.Matcher
            assert args != null
            assert delegate instanceof Book

            return "foo${matcher[0][1]}"
       }

        def b = new Book()
        b.metaClass = metaClass

        assertEquals "fooBar", b.findByBar()
    }

    void testRegexStaticMethodDefinition() {
       def metaClass = new DynamicMethodsExpandoMetaClass(Book.class, true)
       metaClass.initialize()

       metaClass.'static'./^findBy(\w+)$/ = { matcher, args ->
            assert matcher instanceof java.util.regex.Matcher
            assert args != null
            println delegate.getName()
            println delegate.getClass()

            return "foo${matcher[0][1]}"
       }

        assertEquals "fooBar", Book.findByBar()

    }

    void testMixStandardAndRegixMethodDefinitions() {
       def metaClass = new DynamicMethodsExpandoMetaClass(Book.class, true)
       metaClass.initialize()

       metaClass.'static'./^findBy(\w+)$/ = { matcher, args ->
            assert matcher instanceof java.util.regex.Matcher
            assert args != null
            println delegate.getName()
            println delegate.getClass()

            return "foo${matcher[0][1]}"
       }

        metaClass.'static'.anotherFoo = {String obj-> "bar ${obj}" }
        metaClass.anotherBar = {-> "foo" }
        metaClass.getFoo = {-> "bar" }

        assertEquals "fooBar", Book.findByBar()
        assertEquals "bar foo", Book.anotherFoo("foo")

        def b = new Book()
        assertEquals "foo", b.anotherBar()
        assertEquals "bar", b.foo
        assertEquals "bar", b.getFoo()

    }

}
class Book {
    String title
}
