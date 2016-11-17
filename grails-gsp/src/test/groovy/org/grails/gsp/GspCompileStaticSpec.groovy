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

import grails.core.GrailsTagLibClass
import org.grails.core.DefaultGrailsTagLibClass
import org.grails.taglib.TagLibraryLookup
import spock.lang.Specification


class GspCompileStaticSpec extends Specification {
    GroovyPagesTemplateEngine gpte

    def setup() {
        gpte = new GroovyPagesTemplateEngine()
        gpte.afterPropertiesSet()
    }

    def "should support model fields in both compilation modes"() {
        given:
        def template = """<%@ model="Date date" compileStatic="$compileStatic"%>\${date.time}"""
        def date = new Date(123L)
        when:
        def rendered = renderTemplate(template, [date: date], compileStatic)
        then:
        rendered == '123'
        where:
        compileStatic << [true, false]
    }

    def "specifying model implies compileStatic mode"() {
        given:
        def template = '<%@ model="Date date"%>${date.time}'
        def date = new Date(123L)
        when:
        def rendered = renderTemplate(template, [date: date], true)
        then:
        rendered == '123'
    }

    def "should support typed variables in both compilation modes"() {
        given:
        def template = """<%@ compileStatic="$compileStatic"%><g:def type="Date" var="date" value="\${new Date(123L)}"/>\${date.time}"""
        when:
        def rendered = renderTemplate(template, [:], compileStatic)
        then:
        rendered == '123'
        where:
        compileStatic << [true, false]
    }

    def "should support g:each in both compilation modes"() {
        given:
        def template = """<%@ model="List<Date> dates" compileStatic="$compileStatic"%><g:each var="date" in="\${dates}">\${date.time},</g:each>"""
        def model = [dates: [new Date(123L), new Date(456L), new Date(789L)]]
        when:
        def rendered = renderTemplate(template, model, compileStatic)
        then:
        rendered == '123,456,789,'
        where:
        compileStatic << [true, false]
    }

    def "should support message tag invocation"() {
        given:
        def tagLibraryLookup = new TagLibraryLookup() {
            @Override
            protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
                tags.put(name, taglib.newInstance())
            }
        }
        tagLibraryLookup.registerTagLib(new DefaultGrailsTagLibClass(SampleTagLib))
        gpte.tagLibraryLookup = tagLibraryLookup
        def template = '<%@ compileStatic="true"%>${' + (gDotPrefix ? 'g.' : '') + '''message(code:'World')}'''
        when:
        def rendered = renderTemplate(template, [:], true)
        then:
        rendered == 'Hello World'
        where:
        gDotPrefix << [false, true]
    }

    def "should support multi-line model declaration"() {
        given:
        def template = '''<%@ model="""
Date d1=new Date(123L)
Date d2=new Date(456L)
Date d3=new Date(789L)
Date d4=new Date(123L)
"""%>${d1.time}-${d2.time}-${d3.time}-${d4.time}'''
        when:
        def rendered = renderTemplate(template, [:], true, true)
        then:
        rendered == '123-456-789-123'
    }

    def "multiple model fields can be separated with semicolons"() {
        given:
        def template = '''<%@ model="Date d1=new Date(123L); Date d2=new Date(456L); Date d3=new Date(789L); Date d4=new Date(123L)"%>${d1.time}-${d2.time}-${d3.time}-${d4.time}'''
        when:
        def rendered = renderTemplate(template, [:], true, true)
        then:
        rendered == '123-456-789-123'
    }

    def "fields can be added by using alternative syntax"() {
        given:
        def template = '''@{ model="""Date d1=new Date(123L); Date d2=new Date(456L); Date d3=new Date(789L); Date d4=new Date(123L) """}${d1.time}-${d2.time}-${d3.time}-${d4.time}'''
        when:
        def rendered = renderTemplate(template, [:], true, true)
        then:
        rendered == '123-456-789-123'
    }

    def renderTemplate(templateSource, model, expectedCompileStaticMode, printSource = false) {
        def t = gpte.createTemplate(templateSource, "template${templateSource.hashCode()}")
        def w = t.make(model)
        if(printSource) {
            def sourceWriter = new StringWriter()
            w.writeGroovySourceToResponse(w.metaInfo, sourceWriter)
            println(sourceWriter.toString())
        }
        assert w.metaInfo.compileStaticMode == expectedCompileStaticMode
        def sw = new StringWriter()
        def pw = new PrintWriter(sw, true)
        w.writeTo(pw)
        sw.toString()
    }
}

class SampleTagLib {
    static returnObjectForTags = ['message']

    Closure message = { attrs ->
        "Hello ${attrs.code}"
    }
}

