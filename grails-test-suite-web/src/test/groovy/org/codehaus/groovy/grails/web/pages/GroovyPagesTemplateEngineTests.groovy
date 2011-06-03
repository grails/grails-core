package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.UrlResource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import grails.util.GrailsUtil
import grails.util.GrailsWebUtil

class GroovyPagesTemplateEngineTests extends GroovyTestCase {

    void testCommentAtEndOfTemplate() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def request = webRequest.request
        request.addParameter("showSource", "true")

        System.setProperty("grails.env", "development")
        assert GrailsUtil.isDevelopmentEnv()

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

        // It is important that the template ends with the comment. Whitespace or anything else after
        // the comment will not trigger the problem.  See GRAILS-1737
        def pageSource = "<html><body></body></html><%-- should not be in the output --%>"

        def t = gpte.createTemplate(pageSource, "comment_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertTrue(sw.toString().indexOf("should not be in the output") == -1)
    }

    void testShowSourceParameter() {
        try {
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.request
            request.addParameter("showSource", "true")

            System.setProperty("grails.env", "development")
            assert GrailsUtil.isDevelopmentEnv()

            def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
            gpte.afterPropertiesSet()

            def t = gpte.createTemplate("<%='hello'%>", "hello_test")
            def w = t.make()

            def sw = new StringWriter()
            def pw = new PrintWriter(sw)

            w.writeTo(pw)

            assertTrue(sw.toString().indexOf(GroovyPage.OUT_STATEMENT + ".print('hello')") > -1)

        }
        finally {
            System.setProperty("grails.env", "")
        }
    }

    void testEstablishNameForResource() {
        def res = new UrlResource("http://grails.org/some.path/foo.gsp")

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

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
        gpte.afterPropertiesSet()

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
        gpte.afterPropertiesSet()

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
        gpte.afterPropertiesSet()

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
        gpte.afterPropertiesSet()

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
        gpte.afterPropertiesSet()

        def t = gpte.createTemplate("<%='hello'%>", "hello_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)

        assertEquals "hello", sw.toString()
    }

    void testForEachInProductionMode() {
        System.setProperty("grails.env", "production")
        GrailsWebUtil.bindMockWebRequest()

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

        def t = gpte.createTemplate("<g:each var='num' in='\${1..5}'>\${num} </g:each>", "foreach_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        w.writeTo(pw)
        System.setProperty("grails.env", "development")

        assertEquals "1 2 3 4 5 ", sw.toString()
    }

    void testGetUriWithinGrailsViews() {
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

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
