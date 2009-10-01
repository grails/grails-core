package org.codehaus.groovy.grails.web.pages

import groovy.util.GroovyTestCase;


class SitemeshPreprocessorTests extends GroovyTestCase {

	void testSimpleParse() {
		def gspBody='''
<html>
		<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>This is the title</title></head>
		<body onload="test();">
			body text
		</body>
</html>
'''
		def preprocessor=new SitemeshPreprocessor()
		def gspBodyExpected='''
<html>
		<g:captureHead>
		<g:captureMeta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<g:captureTitle>This is the title</g:captureTitle></g:captureHead>
		<g:captureBody onload="test();">
			body text
		</g:captureBody>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}

	void testContentParse() {
		def gspBody='''
<html>
		<head><title>This is the title</title></head>
		<body onload="test();">
			body text
		</body>
		<content tag="nav">
			content test
		</content>
</html>
'''
		def preprocessor=new SitemeshPreprocessor()
		def gspBodyExpected='''
<html>
		<g:captureHead><g:captureTitle>This is the title</g:captureTitle></g:captureHead>
		<g:captureBody onload="test();">
			body text
		</g:captureBody>
		<g:captureContent tag="nav">
			content test
		</g:captureContent>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}


	void testContentParse2() {
		def gspBody='''
<html>
		<head><title>This is the title</title></head>
		<body onload="test();">
			body text
		</body>
		<content tag="nav">
			content test
		</content>
		<content tag="nav">
			content test
		</content>
</html>
'''
		def preprocessor=new SitemeshPreprocessor()
		def gspBodyExpected='''
<html>
		<g:captureHead><g:captureTitle>This is the title</g:captureTitle></g:captureHead>
		<g:captureBody onload="test();">
			body text
		</g:captureBody>
		<g:captureContent tag="nav">
			content test
		</g:captureContent>
		<g:captureContent tag="nav">
			content test
		</g:captureContent>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}
	
	void testOtherParse() {
		def gspBody='''
<html>
		<head ><titlenot>This is not the title</titlenot></head>
		<body>
			body text
		</body>
</html>
'''
		def preprocessor=new SitemeshPreprocessor()
		def gspBodyExpected='''
<html>
		<g:captureHead ><titlenot>This is not the title</titlenot></g:captureHead>
		<g:captureBody>
			body text
		</g:captureBody>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}
}