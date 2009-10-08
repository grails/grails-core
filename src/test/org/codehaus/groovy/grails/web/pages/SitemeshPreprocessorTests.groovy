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
		<sitemesh:captureHead>
		<sitemesh:captureMeta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:captureHead>
		<sitemesh:captureBody onload="test();">
			body text
		</sitemesh:captureBody>
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
		<sitemesh:captureHead><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:captureHead>
		<sitemesh:captureBody onload="test();">
			body text
		</sitemesh:captureBody>
		<sitemesh:captureContent tag="nav">
			content test
		</sitemesh:captureContent>
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
		<sitemesh:captureHead><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:captureHead>
		<sitemesh:captureBody onload="test();">
			body text
		</sitemesh:captureBody>
		<sitemesh:captureContent tag="nav">
			content test
		</sitemesh:captureContent>
		<sitemesh:captureContent tag="nav">
			content test
		</sitemesh:captureContent>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}

    void testSitemeshParameterParse() {
		def gspBody='''
<html>
		<head><title>This is the title</title>
            <parameter name="foo" value="bar" />
        </head>
		<body>
			body text
		</body>
</html>
'''
		def preprocessor=new SitemeshPreprocessor()
		def gspBodyExpected='''
<html>
		<sitemesh:captureHead><sitemesh:captureTitle>This is the title</sitemesh:captureTitle>
            <sitemesh:parameter name="foo" value="bar" />
        </sitemesh:captureHead>
		<sitemesh:captureBody>
			body text
		</sitemesh:captureBody>
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
		<sitemesh:captureHead ><titlenot>This is not the title</titlenot></sitemesh:captureHead>
		<sitemesh:captureBody>
			body text
		</sitemesh:captureBody>
</html>
'''
		assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
	}
}