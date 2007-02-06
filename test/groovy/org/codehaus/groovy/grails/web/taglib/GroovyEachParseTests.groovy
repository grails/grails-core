package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.*;

class GroovyEachParseTests extends ParseTests {

	void testEachOutput() {
		String output = parseCode("myTest", """
<g:each var="t" in="${'blah'}">
</g:each>
""");	
		def expected = '"blah".each { t ->'

			assertEquals(trimAndRemoveCR("""import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.taglib.*

class myTest extends GroovyPage {
public Object run() {
out.print('\\n')
"blah".each { t ->
out.print('\\n')
}
out.print('\\n')
}
}
"""),trimAndRemoveCR(output) )

				
	}
		
		void testEachOutputNoVar() {
			String output = parseCode("myTest2", """
	<g:each in="${'blah'}">
	</g:each>
	""");	
		
			assertEquals(trimAndRemoveCR("""import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.taglib.*

class myTest2 extends GroovyPage {
public Object run() {
out.print('\\n\\t')
"blah".each { 
out.print('\\n\\t')
}
out.print('\\n\\t')
}
}
"""),trimAndRemoveCR(output) )
					
		}	
		
		void testEachOutVarAndIndex() {
			String output = parseCode("myTest2", """
	<g:each var="t" status="i" in="${'blah'}">
	</g:each>
	""");	
						
		  assertEquals(trimAndRemoveCR("""import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.taglib.*

class myTest2 extends GroovyPage {
public Object run() {
out.print('\\n\\t')
"blah".eachWithIndex { t,i ->
out.print('\\n\\t')
}
out.print('\\n\\t')
}
}
"""),trimAndRemoveCR(output) )
		}

}
