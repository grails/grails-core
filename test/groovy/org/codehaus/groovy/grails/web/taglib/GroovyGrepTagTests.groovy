package org.codehaus.groovy.grails.web.taglib;


class GroovyGrepTagTests extends GroovyTestCase {

	void testDoStartTag() {
		def sw = new StringWriter()
		
		def tag = new GroovyGrepTag()
		tag.init(out: new PrintWriter(sw))
		
		try {
			tag.doStartTag()
			fail("Should throw exception for required attributes")
		}
		catch(Exception e) {
			// expected
		}
		
		tag.setAttributes('"in"': 'test', '"filter"':'\${~/regex/}')
		
		tag.doStartTag();
		
		assert "test.grep(~/regex/).each {\n" == sw.toString()
		
	}

}
