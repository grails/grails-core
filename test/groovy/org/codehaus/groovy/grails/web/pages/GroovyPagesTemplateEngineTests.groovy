    package org.codehaus.groovy.grails.web.pages

import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import org.codehaus.groovy.grails.support.*
import grails.util.*

class GroovyPagesTemplateEngineTests extends GroovyTestCase {


    void testShowSourceParameter() {
        try {
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.request
            request.addParameter("showSource", "true")

            System.setProperty("grails.env", "development")
            assert grails.util.GrailsUtil.isDevelopmentEnv()                                        

            def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

            def t = gpte.createTemplate("<%='hello'%>", "hello_test")
            def w = t.make()

            def sw = new StringWriter()
            def pw = new PrintWriter(sw)

            w.writeTo(pw)

            assertTrue(sw.toString().indexOf("out.print('hello')") > -1)

        }
        finally {
            System.setProperty("grails.env", "")
        }
    }

    void testEstablishNameForResource() {
        def res = new UrlResource("http://grails.org/some.path/foo.gsp")

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        assertEquals "some_path_foo_gsp", gpte.establishPageName(res, null)                
    }


    void testCreateTemplateFromCurrentRequest2() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()


        def uri1 = "/WEB-INF/grails-app/views/another.gsp"
        assertNotNull(webRequest.request)
        webRequest.request.requestURI = "/another"
        webRequest.request.servletPath = "/another"



        def rl = new MockStringResourceLoader()
        rl.registerMockResource(uri1, "<%='success 2'%>")


        def gpte = new GroovyPagesTemplateEngine(new MockServletContext(rl))

        def t = gpte.createTemplate()
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "success 2", sw.toString()
    }
    
    void testCreateTemplateFromCurrentRequest1() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()


        def uri1 = "/somedir/myview"
        assertNotNull(webRequest.request)
        webRequest.request.requestURI = uri1
        webRequest.request.servletPath = uri1

        def uri2 = "/WEB-INF/grails-apps/views/another.gsp"

        def rl = new MockStringResourceLoader()
        rl.registerMockResource(uri1, "<%='success 1'%>")
        rl.registerMockResource(uri2, "<%='success 2'%>")
        
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext(rl))
        
        def t = gpte.createTemplate()
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "success 1", sw.toString()


    }


    void testCreateTemplateFromResource() {
        GrailsWebUtil.bindMockWebRequest()

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        def t = gpte.createTemplate(new ByteArrayResource("<%='hello'%>".bytes))
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "hello", sw.toString()

    }

    void testCreateTemplateWithBinding() {

        GrailsWebUtil.bindMockWebRequest()

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        def t = gpte.createTemplate('Hello ${foo}', "hello_test")
        def w = t.make(foo:"World")

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "Hello World", sw.toString()

    }

    void testCreateTemplateFromText() {
    
        GrailsWebUtil.bindMockWebRequest()
        
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        def t = gpte.createTemplate("<%='hello'%>", "hello_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "hello", sw.toString()
    }

    void testGetUriWithinGrailsViews() {
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        assertEquals "/WEB-INF/grails-app/views/myview.gsp", gpte.getUriWithinGrailsViews("/myview")        
        assertEquals "/WEB-INF/grails-app/views/myview.gsp", gpte.getUriWithinGrailsViews("myview")
        assertEquals "/WEB-INF/grails-app/views/mydir/myview.gsp", gpte.getUriWithinGrailsViews("mydir/myview")
        assertEquals "/WEB-INF/grails-app/views/mydir/myview.gsp", gpte.getUriWithinGrailsViews("/mydir/myview")
    }

    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }
    void setUp() {
        RequestContextHolder.setRequestAttributes(null)
    }
}