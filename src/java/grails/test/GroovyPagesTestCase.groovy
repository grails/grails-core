/* Copyright 2004-2005 the original author or authors.
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
package grails.test

import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.springframework.web.context.request.RequestContextHolder

/**
* A test harness that eases testing of GSP and tag libraries for Grails

* @author Graeme Rocher
*/
class GroovyPagesTestCase extends GroovyTestCase  {
    /**
     * The GroovyPagesTemplateEngine which gets wired into this GSP
     */
    GroovyPagesTemplateEngine groovyPagesTemplateEngine

    /**
     * Sets the controller name to use. Should be called to override the defaut "test" value
     */
    void setControllerName(String name) {
        RequestContextHolder.currentRequestAttributes().controllerName = name
    }

    /**
     * Asserts the output of a given template against the specified expected value
     *
     * @param expected The expected output
     * @param template A snippet of GSP
     * @param params An optional parameter that allows variables to be placed in the binding of the GSP
     * @param transform An optional parameter that allows the specification of a closure to transform the passed StringWriter
     */
    def assertOutputEquals(expected, template, params = [:], Closure transform = { it.toString() }) {
        def webRequest = RequestContextHolder.currentRequestAttributes()
        def engine = groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        assertEquals expected, transform(sw)
    }

    /**
     * Applies a GSP template and returns its output as a String
     *
     * @param template The GSP template
     * @param params An optional parameter that allows the specification of the binding
     */
	def applyTemplate(template, params = [:] ) {
        def webRequest = RequestContextHolder.currentRequestAttributes()
        def engine = groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        return sw.toString()
    }
}