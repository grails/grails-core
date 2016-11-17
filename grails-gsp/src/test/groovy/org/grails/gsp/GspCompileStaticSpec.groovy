/*
 * Copyright 2016 the original author or authors.
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

package org.grails.gsp

import spock.lang.Specification


class GspCompileStaticSpec extends Specification {
    GroovyPagesTemplateEngine gpte

    def setup() {
        gpte = new GroovyPagesTemplateEngine()
        gpte.afterPropertiesSet()
    }

    def "should support model fields in both compilation modes"() {
        given:
        def template = """<%@page model="Date date" compileStatic="$compileStatic"%>\${date.time}"""
        def date = new Date(123L)
        when:
        def rendered = renderTemplate(template, [date: date])
        then:
        rendered == '123'
        where:
        compileStatic << [true, false]
    }

    def "should support typed variables in both compilation modes"() {
        given:
        def template = """<%@page compileStatic="$compileStatic"%><g:def type="Date" var="date" value="\${new Date(123L)}"/>\${date.time}"""
        when:
        def rendered = renderTemplate(template, [:])
        then:
        rendered == '123'
        where:
        compileStatic << [true, false]
    }

    def renderTemplate(templateSource, model) {
        def t = gpte.createTemplate(templateSource, "template${templateSource.hashCode()}")
        def w = t.make(model)
        def sw = new StringWriter()
        def pw = new PrintWriter(sw, true)
        w.writeTo(pw)
        sw.toString()
    }
}
