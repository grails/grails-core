package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.commons.ApplicationHolder
import javax.servlet.http.Cookie

class ApplicationTagLibTests extends AbstractGrailsTagTests {

    void testObtainCookieValue() {
        def cookie = new Cookie("foo", "bar")
        request.cookies = [cookie] as Cookie[]

        def template = '<g:cookie name="foo" />'

        assertOutputEquals "bar", template

        template = '${cookie(name:"foo")}'

        assertOutputEquals "bar", template

    }

    void testObtainHeaderValue() {
        request.addHeader "FOO", "BAR"
        def template = '<g:header name="FOO" />'

        assertOutputEquals "BAR", template

        template = '${header(name:"FOO")}'

        assertOutputEquals "BAR", template

    }


    void testClonedUrlFromVariable() {
        def template = '''<g:set var="urlMap" value="${[controller: 'test', action: 'justdoit']}"/>${urlMap}<g:link url="${urlMap}">test</g:link>${urlMap}'''

        assertOutputEquals('{controller=test, action=justdoit}<a href="/test/justdoit">test</a>{controller=test, action=justdoit}', template)
    }

    void testLinkWithMultipleParameters() {
        def template = '<g:link controller="foo" action="action" params="[test: \'test\', test2: \'test2\']">test</g:link>'

        assertOutputEquals('<a href="/foo/action?test=test&amp;test2=test2">test</a>', template)
    }

    void testLinkWithFragment() {
        def template = '<g:link controller="foo" action="bar" fragment="test">link</g:link>'

        assertOutputEquals('<a href="/foo/bar#test">link</a>', template)

    }

    void testSetTag() {
        def template = '<g:set var="one" value="two" />one: ${one}'

        assertOutputEquals('one: two', template)	
	}
	
	void testSetTagWithBody() {
        def template = '<g:set var="one">two</g:set>one: ${one}'

        assertOutputEquals('one: two', template)		
	}

	void testSetTagWithMap() {
        def template = '<g:set var="e" value="${c.a}"/>${e?.b}'

        assertOutputEquals('null', template, [c:[:]])
        assertOutputEquals('foo', template, [c:[a:[b:'foo']]])

    }

    
	void testIteration() {
        def template = '''<g:set var="counter" value="${1}" />
<g:each in="${[10,11,12]}" var="myVal"><g:set var="counter" value="${myVal+counter}" />${counter}</g:each>'''
        assertOutputEquals('112234', template)
    }
	
	void testMetaTag() {
        def template = '<g:meta name="app.version"/>'

        assertOutputEquals('0.9.9.1', template)
	}

    void testCreateLinkToWithDirAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithDirAndLeadingNoLeadingSlash() {
        def template = '<g:createLinkTo dir="images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithFileAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="/foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }



    void testCreateLinkTo() {
		StringWriter sw = new StringWriter();
		withTag("createLinkTo", sw) { tag ->
			def attrs = [dir:'test']
			tag.call( attrs )
			assertEquals '/test', sw.toString()

			sw.getBuffer().delete(0,sw.getBuffer().length());
			attrs = [dir:'test',file:'file']
			tag.call( attrs )
			assertEquals '/test/file', sw.toString()

			sw.getBuffer().delete(0,sw.getBuffer().length());
			attrs = [dir:'']
			tag.call( attrs )
			println sw.toString()
			assertEquals '', sw.toString()
		}
	}

    void testCreateLinkWithZeroId() {
        // test case for GRAILS-1123
        StringWriter sw = new StringWriter();
        withTag("createLink", sw) { tag ->
            def attrs = [action:'testAction', controller: 'testController', id:0]
            tag.call( attrs )
            assertEquals '/testController/testAction/0', sw.toString()
        }
    }

	void testCreateLinkURLEncoding() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [action:'testAction', controller: 'testController',
			    params:['name':'Marc Palmer']]
			tag.call( attrs )
			assertEquals '/testController/testAction?name=Marc+Palmer', sw.toString()
		}
	}

	void testCreateLinkURLEncodingWithHTMLChars() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding is done but HTML encoding isn't, only want the one here.
			def attrs = [action:'testAction', controller: 'testController',
			    params:['email':'<marc@anyware.co.uk>']]
			tag.call( attrs )
			assertEquals '/testController/testAction?email=%3Cmarc%40anyware.co.uk%3E', sw.toString()
		}
	}

	void testCreateLinkWithBase() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [base:"http://www128.myhost.com:3495", action:'testAction', controller: 'testController']
			tag.call( attrs )
			assertEquals 'http://www128.myhost.com:3495/testController/testAction', sw.toString()
		}
	}

    void testAbsoluteWithContextPath() {
        request.contextPath = "/foo"
        def template = '<g:createLink action="testAction" controller="testController" absolute="true" />'

        assertOutputEquals 'http://localhost:8080/foo/testController/testAction', template    
    }

	void testCreateLinkWithAbsolute() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [absolute:"true", action:'testAction', controller: 'testController']
			tag.call( attrs )
			assertEquals 'http://localhost:8080/testController/testAction', sw.toString()
		}
	}

}
