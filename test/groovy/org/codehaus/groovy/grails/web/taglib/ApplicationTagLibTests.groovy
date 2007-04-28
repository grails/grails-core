package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClass

class ApplicationTagLibTests extends AbstractGrailsTagTests {

	void testCreateLinkTo() {
        InvokerHelper.getInstance().getMetaRegistry().removeMetaClass(String.class)
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

	void testCreateLinkURLEncoding() {
        InvokerHelper.getInstance().getMetaRegistry().removeMetaClass(String.class)

		StringWriter sw = new StringWriter();

		withTag("createLink", sw) { tag ->

		    println grailsApplication.allClasses.inspect()
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [action:'testAction', controller: 'testController',
			    params:['name':'Marc Palmer']]
			tag.call( attrs )

			assertEquals '/testController/testAction?name=Marc+Palmer', sw.toString()
		}

	}

	void testCreateLinkURLEncodingWithHTMLChars() {
        InvokerHelper.getInstance().getMetaRegistry().removeMetaClass(String.class)

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
