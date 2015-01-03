package org.grails.web.pages

import org.grails.buffer.GrailsPrintWriter
import org.grails.gsp.GroovyPage
import org.grails.gsp.GroovyPageBinding
import org.grails.gsp.GroovyPagesMetaUtils
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.context.request.RequestContextHolder

class GroovyPageMethodDispatchTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass(
"""
import org.grails.taglib.*
import org.grails.gsp.*
import grails.gsp.*

@grails.artefact.Artefact('Controller')
class TestController {
    def index = {}
}
@TagLib
class Test1TagLib {
    Closure tag1 = { attrs, body ->

        out << "print"

        def result = tag2(test:'blah')
        out << result

        result = body()

        out << result
    }
}
@TagLib
class Test2TagLib {
    Closure tag2 = { attrs, body -> out << attrs.test }
    Closure tag3 = { attrs, body ->
                out << body() }
}
class MyPage extends GroovyPage {
    String getGroovyPageFileName() { "test" }
    def run() {
        setBodyClosure(1) {
            out << "foo"
            ""
        }
        invokeTag("tag1", 'g', -1, [attr1:"test"], 1)

        def tagResult=tag3([:], new TagOutput.ConstantClosure('TEST'))?.toString()
        if (tagResult != 'TEST') {
                out << '<ERROR in tag3 output>' << tagResult
        }
        out << "hello" + tag2(test:"test2", new TagBodyClosure(this, getOutputContext(), {

        }))
    }
}
""")
    }

    void testGroovyPage() {
        runTest {
            def webRequest = RequestContextHolder.currentRequestAttributes()
            GroovyPage script = gcl.loadClass("MyPage").newInstance()
            GroovyPagesMetaUtils.registerMethodMissingForGSP(script.getClass(), appCtx.getBean('gspTagLibraryLookup'))
            script.setJspTagLibraryResolver(appCtx.getBean('jspTagLibraryResolver'))
            script.setGspTagLibraryLookup(appCtx.getBean('gspTagLibraryLookup'))
            def controller = ga.getControllerClass("TestController").newInstance()
            def sw = new StringWriter()
            webRequest.out =  new GrailsPrintWriter(sw)
            def b = new GroovyPageBinding(application:controller.servletContext,
                                request:controller.request,
                                response:controller.response,
                                flash:controller.flash,
                                out: webRequest.out ,
                                webRequest:webRequest)
            script.binding = b
            script.initRun(webRequest.out, OutputContextLookupHelper.lookupOutputContext(), null)
            script.run()
            script.cleanup()

            assertEquals "printblahfoohellotest2",sw.toString()
        }
    }
}
