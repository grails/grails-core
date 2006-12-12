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
		
		assert sw.toString().startsWith("test.grep(~/regex/).each {")
		
	}
		
	void testWithStatus() {
		def sw = new StringWriter()
		
		def tag = new GroovyGrepTag()
		tag.init(out: new PrintWriter(sw))
		
		tag.setAttributes('"in"': 'test', '"filter"':'\${~/regex/}','"status"':"i",'"var"':"t")
		
		tag.doStartTag();
		
		assertEquals( "test.grep(~/regex/).eachWithIndex { t,i ->"+System.getProperty("line.separator"), sw.toString() )		
	}

}
