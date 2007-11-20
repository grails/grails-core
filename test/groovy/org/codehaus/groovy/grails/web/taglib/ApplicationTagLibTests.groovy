package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.ApplicationHolder

class ApplicationTagLibTests extends AbstractGrailsTagTests {

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

	void testIteration() {
        def template = '''<g:set var="counter" value="${1}" />
<g:each in="${[10,11,12]}" var="myVal"><g:set var="counter" value="${myVal+counter}" />${counter}</g:each>'''
        assertOutputEquals('112234', template)
    }
	
	void testMetaTag() {
        def template = '<g:meta name="app.version"/>'

        assertOutputEquals('0.9.9.1', template)
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
			assertEquals '/', sw.toString()
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

}
