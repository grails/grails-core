package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.*;

class GroovyEachParseTests extends ParseTests {
                              
	void testEachOutput() {        
		String output = parseCode("myTest", """
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

out.print('\\n')
evaluate('"blah"', 2, it) { return "blah" }.each { t ->
out.print('\\n')
}
out.print('\\n')
}
}"""),trimAndRemoveCR(output) )
	}

    void testEachOutputNoLineBreaks() {
        String output = parseCode("myTest", """
<g:each var="t" in="${'blah'}"></g:each>""");

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest extends GroovyPage {
public String getGroovyPageFileName() { "myTest" }
public Object run() {
def params = binding.params
def request = binding.request
def flash = binding.flash
def response = binding.response

out.print('\\n')
evaluate('"blah"', 2, it) { return "blah" }.each { t ->
}
}
}"""),trimAndRemoveCR(output) )
    }

		
		void testEachOutVarAndIndex() {
			String output = parseCode("myTest2", """
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

out.print('\\n')
evaluate('"blah"', 2, it) { return "blah" }.eachWithIndex { t,i ->
out.print('\\n')
}
out.print('\\n')
}
}"""),trimAndRemoveCR(output) )
		}

}
