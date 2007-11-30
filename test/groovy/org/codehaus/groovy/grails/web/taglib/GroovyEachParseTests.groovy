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
public Object run() {
"blah".each { t ->
}
}
}"""),trimAndRemoveCR(output) )

				               
	}
		
		void testEachOutputNoVar() {
			String output = parseCode("myTest2", """
	<g:each in="${'blah'}">
	</g:each>
	""");	             
		
			assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest2 extends GroovyPage {
public Object run() {
out.print(STATIC_HTML_CONTENT_0)
"blah".each { 
out.print(STATIC_HTML_CONTENT_1)
}
out.print(STATIC_HTML_CONTENT_2)
}
static final STATIC_HTML_CONTENT_0 = '''\\t'''

static final STATIC_HTML_CONTENT_1 = '''\\t'''

static final STATIC_HTML_CONTENT_2 = '''\\t'''

}"""),trimAndRemoveCR(output) )
					
		}	
		
		void testEachOutVarAndIndex() {
			String output = parseCode("myTest2", """
	<g:each var="t" status="i" in="${'blah'}">
	</g:each>
	""");	
						
		  assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest2 extends GroovyPage {
public Object run() {
out.print(STATIC_HTML_CONTENT_0)
"blah".eachWithIndex { t,i ->
out.print(STATIC_HTML_CONTENT_1)
}
out.print(STATIC_HTML_CONTENT_2)
}
static final STATIC_HTML_CONTENT_0 = '''\\t'''

static final STATIC_HTML_CONTENT_1 = '''\\t'''

static final STATIC_HTML_CONTENT_2 = '''\\t'''

}"""),trimAndRemoveCR(output) )
		}

}
