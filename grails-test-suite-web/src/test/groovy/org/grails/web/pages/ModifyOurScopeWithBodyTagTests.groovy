package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests

class ModifyOurScopeWithBodyTagTests extends AbstractGrailsTagTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
import grails.gsp.*

@TagLib
class OutScopeTagLib {
  Closure threeTimes = { attrs, body ->
    3.times {
        if (attrs.var)
            out << body((attrs.var):it)
        else
            out << body()
    }
  }
    Closure local = { attrs, body ->
        out << body(attrs.vars)
    }

    Closure ittest = { attrs, body ->
        out << body('hello')
    }

    Closure nestedouter = { attrs, body ->
        out << body(test1:1)
    }

    Closure nestedinner = { attrs, body ->
        out << body(test2:2)
    }

    Closure bodytag = { attrs, body ->
        if (attrs.model)
            out << body(attrs.model)
        else
            out << body()
    }
}
'''
    }

    // test for GRAILS-5847
    void testModifyOuterScopeInTag() {

        // test with no body arguments
        def template = '<g:set var="counter" value="${1}"/><g:threeTimes>${counter++}</g:threeTimes>'

        assertOutputEquals '123', template

        // test with body arguments
        template = '<g:set var="counter" value="${1}"/><g:threeTimes var="x">${counter++}</g:threeTimes>'
        assertOutputEquals '123', template
    }

    // test for GRAILS-2675
    void testRestoreOuterVariableNamesWithBodyArguments() {
        def template = '<g:set var="counter" value="${9}"/><g:threeTimes var="counter">${counter++}</g:threeTimes>${counter}'
        assertOutputEquals '0129', template

        template = '<g:set var="counter" value="${1}"/><g:threeTimes var="counter">${counter}</g:threeTimes>${counter}'
        assertOutputEquals '0121', template
    }

    // test for GRAILS-7306
    void testRestoreOuterVariableNamesWithBodyArgumentsEvenIfOuterValueIsNull() {
        def template = '''<g:set var="foo" value="parentFooVal"/><g:set var="bar" value="${null}"/><g:local vars="[foo:'innerFooVal', bar:'nonNullVal']" someValue="nonNull" var="counter">inner foo: ${foo}, inner bar: ${bar}</g:local> outer foo: ${foo}, outer bar: ${bar}'''
        assertOutputEquals 'inner foo: innerFooVal, inner bar: nonNullVal outer foo: parentFooVal, outer bar: ', template
    }

    void testBodyIt() {
        def template = '''<g:set var="it" value=" world"/><g:ittest>${it}</g:ittest>${it}'''
        assertOutputEquals 'hello world', template
    }

    // test for GRAILS-8554
    void testNestedScope() {
        def template = '''<g:nestedouter><g:nestedinner>${test1} ${test2}</g:nestedinner></g:nestedouter>'''
        assertOutputEquals '1 2', template
    }

    // test for GRAILS-8569
    void testGSetInBody() {
        def template = '''<g:bodytag><g:set var="a" value="1"/></g:bodytag><g:bodytag model="[c:3]"><g:set var="b" value="2"/></g:bodytag>${a} ${b} ${c}'''
        assertOutputEquals '1 2 ', template
    }
}
