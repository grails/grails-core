package org.codehaus.groovy.grails.web.taglib;


class MessageTagTests extends AbstractTagLibTests {

	void testMessageTag() {
		StringWriter sw = new StringWriter();
		Closure tag = getTag("message",sw);
		
		// test when no message found it returns code
		def attrs = [code:"test.code"]	
		tag.call( attrs )		
		assertEquals "test.code", sw.toString()
		
		clearBuffer()
		
		// now test that when there is a message it finds it
		messageSource.addMessage("test.code", new Locale("en"), "hello world!")
		tag.call( attrs )		
		assertEquals "hello world!", sw.toString()
		
		// now test with arguments
		clearBuffer()
		messageSource.addMessage("test.args", new Locale("en"), "hello {0}!")
		attrs = [code:"test.args", args:["fred"]]
		                                 
		tag.call(attrs)
		
		assertEquals "hello fred!", sw.toString()
		
	}

}
