package org.grails.web.pages
import grails.core.GrailsApplication
import org.grails.buffer.GrailsPrintWriterAdapter
import org.grails.gsp.GroovyPage
import org.grails.gsp.GroovyPageBinding
import org.grails.gsp.GroovyPagesMetaUtils
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.grails.web.servlet.DefaultGrailsApplicationAttributes
import org.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.grails.web.util.GrailsApplicationAttributes
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
        String taglibCode = """import org.grails.taglib.*
        import grails.gsp.*

        @TagLib
        class MyTagLib {
            Closure isaid = { attrs, body ->
                out.print('I said, \"')
                out << body()
                out.print('\"')
            }
        }"""

        gcl.parseClass(taglibCode)
    }

    void testReservedNames() {
        assertTrue GroovyPage.isReservedName(GroovyPage.OUT)
        assertTrue GroovyPage.isReservedName(GroovyPage.PAGE_SCOPE)
    }

    void testRunPage() {

        String pageCode = "import org.grails.gsp.GroovyPage\n" +
        "import org.grails.taglib.*\n"+
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
            PrintWriter pw = new GrailsPrintWriterAdapter(sw)

            String contentType = "text/html;charset=UTF-8"
            response.setContentType(contentType) // must come before response.getWriter()

            GroovyPage gspScript = parseGroovyPage(pageCode)
            gspScript.binding = getBinding(pw)
            gspScript.initRun(pw, OutputContextLookupHelper.lookupOutputContext(), null)
            gspScript.run()
            gspScript.cleanup()
            result =  sw.toString()
        }
        return result
    }

    private GroovyPage parseGroovyPage(pageCode) {
        GroovyPage gspScript = gcl.parseClass(pageCode).newInstance()
        gspScript.setJspTagLibraryResolver(appCtx.getBean("jspTagLibraryResolver"))
        gspScript.setGspTagLibraryLookup(appCtx.getBean("gspTagLibraryLookup"))
        GroovyPagesMetaUtils.registerMethodMissingForGSP(gspScript.getClass(), appCtx.getBean("gspTagLibraryLookup"))
        return gspScript
    }

    void testInvokeBodyTag() {

        String pageCode = "import org.grails.gsp.GroovyPage\n" +
                "import org.grails.taglib.*\n"+
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

        String pageCode = "import org.grails.gsp.GroovyPage\n" +
                "import org.grails.taglib.*\n"+
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
        String pageCode = "import org.grails.gsp.GroovyPage\n" +
                "import org.grails.taglib.*\n"+
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
        ApplicationContext appContext = attrs.applicationContext
        binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID))
        binding.setVariable(GroovyPage.OUT, out)
        webRequest.out = out

        return binding
    }
}
