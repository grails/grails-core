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
		
		assertEquals( "test.each { "+System.getProperty("line.separator"), sw.toString() )
	
	}
		
	void testEachWithVar() {
		def sw = new StringWriter()
		
		def tag = new GroovyEachTag()
		tag.init(out: new PrintWriter(sw))		
		tag.setAttributes('"in"': 'test', '"var"':"i")
		
		tag.doStartTag();

		assert sw.toString() == "test.each { i ->"+System.getProperty("line.separator")		
		
	}
		
    void testEachWithStatusOnly() {
		def sw = new StringWriter()
		
		def tag = new GroovyEachTag()
		tag.init(out: new PrintWriter(sw))		
		tag.setAttributes('"in"': 'test', '"status"':"i")
		try {
			tag.doStartTag();	
			fail("exception should have been thrown for status with no var")
		}
		catch(Exception ex) {
			// expected (can't have each with status and no var
		}		
    	
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
