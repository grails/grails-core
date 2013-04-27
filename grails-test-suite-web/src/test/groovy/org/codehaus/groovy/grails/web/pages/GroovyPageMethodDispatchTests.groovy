package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter
import org.springframework.web.context.request.RequestContextHolder

class GroovyPageMethodDispatchTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass(
"""
import org.codehaus.groovy.grails.web.taglib.*
import org.codehaus.groovy.grails.web.pages.*

class TestController {
    def index = {}
}
class Test1TagLib {
    Closure tag1 = { attrs, body ->

        out << "print"

        def result = tag2(test:'blah')
        out << result

        result = body()

        out << result
    }
}
class Test2TagLib {
    Closure tag2 = { attrs, body -> out << attrs.test }
    Closure tag3 = { attrs, body ->
                out << body() }
}
class MyPage extends org.codehaus.groovy.grails.web.pages.GroovyPage {
    String getGroovyPageFileName() { "test" }
    def run() {
        setBodyClosure(1) {
            out << "foo"
            ""
        }
        invokeTag("tag1", 'g', -1, [attr1:"test"], 1)

        def tagResult=tag3([:], new GroovyPage.ConstantClosure('TEST'))?.toString()
        if (tagResult != 'TEST') {
                out << '<ERROR in tag3 output>' << tagResult
        }
        out << "hello" + tag2(test:"test2", new GroovyPageTagBody(this, webRequest, {

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
            script.initRun(webRequest.out, webRequest, null)
            script.run()
            script.cleanup()

            assertEquals "printblahfoohellotest2",sw.toString()
        }
    }
}
