package org.codehaus.groovy.grails.web.taglib;


class GroovyEachTagTests extends GroovyTestCase {

	void testSimpleEach() {
		def sw = new StringWriter()
		
		def tag = new GroovyEachTag()
		tag.init(out: new PrintWriter(sw))
		
		try {
			tag.doStartTag()
			fail("Should throw exception for required attributes")
		}
		catch(Exception e) {
			// expected
		}
		
		tag.setAttributes('"in"': 'test')
		
		tag.doStartTag();
		
		assert sw.toString() == "test.each { it ->"+System.getProperty("line.separator")
	
	}
		
	void testEachWithVar() {
		def sw = new StringWriter()
		
		def tag = new GroovyEachTag()
		tag.init(out: new PrintWriter(sw))		
		tag.setAttributes('"in"': 'test', '"var"':"i")
		
		tag.doStartTag();

		assert sw.toString() == "test.each { i ->"+System.getProperty("line.separator")		
		
	}
		
    void testEachWithStatus() {
		def sw = new StringWriter()
		
		def tag = new GroovyEachTag()
		tag.init(out: new PrintWriter(sw))		
		tag.setAttributes('"in"': 'test', '"status"':"i")
		
		tag.doStartTag();

		assert sw.toString() == "test.eachWithIndex { it,i ->"+System.getProperty("line.separator")		
    	
    }
		
	    void testEachWithStatusAndVar() {
			def sw = new StringWriter()
			
			def tag = new GroovyEachTag()
			tag.init(out: new PrintWriter(sw))		
			tag.setAttributes('"in"': 'test', '"status"':"i",'"var"':"i")
			
			try {
				tag.doStartTag();
				fail("exception expected as status cannot equal var")
			}
			catch(Exception e) {
				// expected
			}
			tag.setAttributes('"var"':'j')
			tag.doStartTag();

			println sw.toString()
			assert sw.toString() == "test.eachWithIndex { j,i ->"+System.getProperty("line.separator")		
	    	
	    }		

}
