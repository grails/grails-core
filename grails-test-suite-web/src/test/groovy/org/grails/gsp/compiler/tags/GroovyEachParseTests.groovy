package org.grails.gsp.compiler.tags

import org.grails.web.pages.ParseTests

class GroovyEachParseTests extends ParseTests {

    void testEachOutput() {
        def output = parseCode("myTest", """
<g:each var="t" in="${'blah'}">
</g:each>
""")

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest extends org.grails.gsp.GroovyPage {
public String getGroovyPageFileName() { "myTest" }
public Object run() {
Writer out = getOut()
Writer expressionOut = getExpressionOut()
registerSitemeshPreprocessMode()

printHtmlPart(0)
for( t in evaluate('"blah"', 2, it) { return "blah" } ) {
printHtmlPart(0)
}
printHtmlPart(0)
}""" + GSP_FOOTER
),trimAndRemoveCR(output.toString()))
        assertEquals("\n", output.htmlParts[0])
    }

    void testEachOutputNoLineBreaks() {
        def output = parseCode("myTest", """
<g:each var="t" in="${'blah'}"></g:each>""")

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest extends org.grails.gsp.GroovyPage {
public String getGroovyPageFileName() { "myTest" }
public Object run() {
Writer out = getOut()
Writer expressionOut = getExpressionOut()
registerSitemeshPreprocessMode()

printHtmlPart(0)
for( t in evaluate('"blah"', 1, it) { return "blah" } ) {
}
}""" + GSP_FOOTER
),trimAndRemoveCR(output.toString()))
        assertEquals("\n", output.htmlParts[0])
    }

    void testEachOutVarAndIndex() {
        def output = parseCode("myTest2", """
<g:each var="t" status="i" in="${'blah'}">
</g:each>
""")

        assertEquals(trimAndRemoveCR(makeImports()+"""\n
class myTest2 extends org.grails.gsp.GroovyPage {
public String getGroovyPageFileName() { "myTest2" }
public Object run() {
Writer out = getOut()
Writer expressionOut = getExpressionOut()
registerSitemeshPreprocessMode()

printHtmlPart(0)
loop:{
int i = 0
for( t in evaluate('"blah"', 2, it) { return "blah" } ) {
printHtmlPart(0)
i++
}
}
printHtmlPart(0)
}""" + GSP_FOOTER
),trimAndRemoveCR(output.toString()))
        assertEquals("\n", output.htmlParts[0])
    }

}
