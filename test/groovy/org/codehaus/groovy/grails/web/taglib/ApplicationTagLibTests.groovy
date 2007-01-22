package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClass

class ApplicationTagLibTests extends AbstractGrailsTagTests {

	void testCreateLinkURLEncoding() {
    /*

		StringWriter sw = new StringWriter();

		withTag("createLink", sw) { tag ->

		    println grailsApplication.allClasses.inspect()
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [action:'testAction', controller: 'testController',
			    params:['name':'Marc Palmer']]
			tag.call( attrs )

			println "output: ${sw}"

			assertEquals '<a href="/testController/testAction?name=Marc%20Palmer"></a>"', sw.toString()
		}*/

	}

	void testCreateLinkURLEncodingWithHTMLChars() {
/*
        InvokerHelper.getInstance().getMetaRegistry().removeMetaClass(String.class)

		StringWriter sw = new StringWriter();

		withTag("createLink", sw) { tag ->

			// test URL encoding is done but HTML encoding isn't, only want the one here.
			def attrs = [action:'testAction', controller: 'testController',
			    params:['email':'<marc@anyware.co.uk>']]
			tag.call( attrs )

			println "output: ${sw}"

			assertEquals '<a href="/testController/testAction?email=%3Cmarc%40anyware%2Eco%2Euk%3E"></a>"', sw.toString()
		}

*/
	}

}
