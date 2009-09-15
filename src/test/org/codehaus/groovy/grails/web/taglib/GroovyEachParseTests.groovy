package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.*;

class GroovyEachParseTests extends ParseTests {
                              
	void testEachOutput() {        
		def output = parseCode("myTest", """
<g:each var="t" in="${'blah'}">
</g:each>
""");	

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest extends GroovyPage {
public String getGroovyPageFileName() { "myTest" }
public Object run() {
def params = binding.params
def request = binding.request
def flash = binding.flash
def response = binding.response
def out = binding.out
registerSitemeshPreprocessMode(request)

printHtmlPart(0)
evaluate('"blah"', 2, it) { return "blah" }.each { t ->
printHtmlPart(0)
}
printHtmlPart(0)
}""" + GSP_FOOTER 
),trimAndRemoveCR(output.toString()) )
		assertEquals("\n", output.htmlParts[0])
	}

    void testEachOutputNoLineBreaks() {
        def output = parseCode("myTest", """
<g:each var="t" in="${'blah'}"></g:each>""");

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest extends GroovyPage {
public String getGroovyPageFileName() { "myTest" }
public Object run() {
def params = binding.params
def request = binding.request
def flash = binding.flash
def response = binding.response
def out = binding.out
registerSitemeshPreprocessMode(request)

printHtmlPart(0)
evaluate('"blah"', 1, it) { return "blah" }.each { t ->
}
}""" + GSP_FOOTER
),trimAndRemoveCR(output.toString()) )
		assertEquals("\n", output.htmlParts[0])
    }

		
		void testEachOutVarAndIndex() {
			def output = parseCode("myTest2", """
<g:each var="t" status="i" in="${'blah'}">
</g:each>
""");
						
		  assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest2 extends GroovyPage {
public String getGroovyPageFileName() { "myTest2" }
public Object run() {
def params = binding.params
def request = binding.request
def flash = binding.flash
def response = binding.response
def out = binding.out
registerSitemeshPreprocessMode(request)

printHtmlPart(0)
evaluate('"blah"', 2, it) { return "blah" }.eachWithIndex { t,i ->
printHtmlPart(0)
}
printHtmlPart(0)
}""" + GSP_FOOTER
),trimAndRemoveCR(output.toString()) )
		  assertEquals("\n", output.htmlParts[0])
		}

}
