package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter
import org.springframework.context.ApplicationContext

/**
 * Tests the page execution environment, including how tags are invoked.
 *
 * The method executePage() has been added to simplify the testing code.
 * The method getBinding() is a copy of the code from
 * GroovyPagesTemplateEngine.GroovyPageTemplateWritable, except that
 * the context variable is being passed in.
 *
 * @author Daiji
 */
class GroovyPageTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        String taglibCode = "import org.codehaus.groovy.grails.web.taglib.*\n"+
        "\n"+
        "class MyTagLib {\n"+
        "Closure isaid = { attrs, body ->\n"+
        "out.print('I said, \"')\n"+
        "out << body()\n" +
        "out.print('\"')\n"+
        "}\n"+
        "}"

        gcl.parseClass(taglibCode)
    }

    void testReservedNames() {
        assertTrue GroovyPage.isReservedName(GroovyPage.REQUEST)
        assertTrue GroovyPage.isReservedName(GroovyPage.RESPONSE)
        assertTrue GroovyPage.isReservedName(GroovyPage.SESSION)
        assertTrue GroovyPage.isReservedName(GroovyPage.SERVLET_CONTEXT)
        assertTrue GroovyPage.isReservedName(GroovyPage.APPLICATION_CONTEXT)
        assertTrue GroovyPage.isReservedName(GroovyPage.PARAMS)
        assertTrue GroovyPage.isReservedName(GroovyPage.OUT)
        assertTrue GroovyPage.isReservedName(GroovyPage.FLASH)
        assertTrue GroovyPage.isReservedName(GroovyPage.PAGE_SCOPE)
    }

    void testRunPage() {

        String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
        "import org.codehaus.groovy.grails.web.taglib.*\n"+
        "\n"+
        "class test_index_gsp extends GroovyPage {\n"+
        "String getGroovyPageFileName() { \"test\" }\n"+
        "public Object run() {\n"+
        "def out=getOut()\n"+
        "out.print('<div>RunPage test</div>')\n"+
        "}\n"+
        "}"

        def result = runPageCode(pageCode)
        assertEquals("<div>RunPage test</div>",result)
    }

    def runPageCode(pageCode) {
        def result = null
        runTest {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new GrailsPrintWriter(sw)

            String contentType = "text/html;charset=UTF-8"
            response.setContentType(contentType) // must come before response.getWriter()

            GroovyPage gspScript = parseGroovyPage(pageCode)
            gspScript.binding = getBinding(pw)
            gspScript.initRun(pw, webRequest, ga, null)
            gspScript.run()
            result =  sw.toString()
        }
        return result
    }

    private GroovyPage parseGroovyPage(pageCode) {
        GroovyPage gspScript = gcl.parseClass(pageCode).newInstance()
        gspScript.setJspTagLibraryResolver(appCtx.getBean("jspTagLibraryResolver"))
        gspScript.setGspTagLibraryLookup(appCtx.getBean("gspTagLibraryLookup"))
        return gspScript
    }

    void testInvokeBodyTag() {

        String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
                "import org.codehaus.groovy.grails.web.taglib.*\n"+
                "\n"+
                "class test_index_gsp extends GroovyPage {\n"+
                "String getGroovyPageFileName() { \"test\" }\n"+
                "public Object run() {\n"+
                "setBodyClosure(1) { out.print('Boo!') }\n"+
                "invokeTag('isaid', 'g', -1, [:], 1)\n"+
                "}\n"+
                "}"

        String expectedOutput = "I said, \"Boo!\""
        def result = runPageCode(pageCode)
        assertEquals(expectedOutput,result)
    }

    void testInvokeBodyTagWithUnknownNamespace() throws Exception {

        String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
                "import org.codehaus.groovy.grails.web.taglib.*\n"+
                "\n"+
                "class test_index_gsp extends GroovyPage {\n"+
                "String getGroovyPageFileName() { \"test\" }\n"+
                "public Object run() {\n"+
                "def out = getOut()\n"+
                "setBodyClosure(1) { out.print('Boo!') }\n"+
                "invokeTag('Person','foaf', -1, [a:'b',c:'d'], 1)\n"+
                "}\n"+
                "}"

        String expectedOutput = "<foaf:Person a=\"b\" c=\"d\">Boo!</foaf:Person>"
        def result = runPageCode(pageCode)
        assertEquals(expectedOutput,result)
    }

    void testInvokeBodyTagAsMethod() {
        String pageCode = "import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
                "import org.codehaus.groovy.grails.web.taglib.*\n"+
                "\n"+
                "class test_index_gsp extends GroovyPage {\n"+
                "String getGroovyPageFileName() { \"test\" }\n"+
                "public Object run() {\n"+
                "out.print(isaid([:],'Boo!'))\n"+
                "}\n"+
                "}"
        String expectedOutput = "I said, \"Boo!\""

        def result = runPageCode(pageCode)
        assertEquals(expectedOutput,result)
    }

    def getBinding(out) {
        // if there is no controller in the request configure using existing attributes, creating objects where necessary
        Binding binding = new GroovyPageBinding()
        GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(servletContext)
        binding.setVariable(GroovyPage.REQUEST, request)
        binding.setVariable(GroovyPage.RESPONSE, response)
        binding.setVariable(GroovyPage.FLASH, attrs.getFlashScope(request))
        binding.setVariable(GroovyPage.SERVLET_CONTEXT, servletContext)
        ApplicationContext appContext = attrs.applicationContext
        binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appContext)
        binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID))
        binding.setVariable(GroovyPage.SESSION, request.getSession())
        binding.setVariable(GroovyPage.PARAMS, new GrailsParameterMap(request))
        binding.setVariable(GroovyPage.WEB_REQUEST, webRequest)
        binding.setVariable(GroovyPage.OUT, out)
        webRequest.out = out

        return binding
    }
}
