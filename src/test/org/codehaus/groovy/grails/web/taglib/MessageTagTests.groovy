package org.codehaus.groovy.grails.web.taglib;


class MessageTagTests extends AbstractGrailsTagTests {

	void testMessageTag() {
		StringWriter sw = new StringWriter();
		
		withTag("message", sw) { tag ->
		
			// test when no message found it returns code
			def attrs = [code:"test.code"]	
			tag.call( attrs )		
			assertEquals "test.code", sw.toString()
			
			sw.buffer.delete(0,sw.buffer.size())
			
			// now test that when there is a message it finds it
			messageSource.addMessage("test.code", new Locale("en"), "hello world!")
			tag.call( attrs )		
			assertEquals "hello world!", sw.toString()
			
			// now test with arguments
			sw.buffer.delete(0,sw.buffer.size())
			
			messageSource.addMessage("test.args", new Locale("en"), "hello {0}!")
			attrs = [code:"test.args", args:["fred"]]
			                                 
			tag.call(attrs)
			
			assertEquals "hello fred!", sw.toString()
		}
		
	}

	void testMessageTagWithCodec() {
		StringWriter sw = new StringWriter();

		withTag("message", sw) { tag ->

			def attrs = [code:"test.code", encodeAs:'HTML']
			messageSource.addMessage("test.code", new Locale("en"), ">>&&")
			tag.call( attrs )
			assertEquals "&gt;&gt;&amp;&amp;", sw.toString()
		}

	}

}
