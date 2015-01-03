package org.grails.gsp.jsp

import grails.core.DefaultGrailsApplication
import grails.util.GrailsWebMockUtil

import org.grails.web.pages.GroovyPagesServlet
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.JstlUtils

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class IterativeJspTagTests extends GroovyTestCase {

    GrailsWebRequest webRequest

    protected void setUp() {
        webRequest = GrailsWebMockUtil.bindMockWebRequest()
        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
    }

    protected void tearDown() {
        RequestContextHolder.resetRequestAttributes()
        GroovySystem.metaClassRegistry.removeMetaClass TagLibraryResolverImpl
    }

    void testIterativeTag() {
        def resolver =  new TagLibraryResolverImpl()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication= new DefaultGrailsApplication()
        resolver.tldScanPatterns = ['classpath*:/META-INF/c-1_0-rt.tld'] as String[]
        resolver.resourceLoader = new DefaultResourceLoader(this.class.classLoader)

        JspTagLib tagLib = resolver.resolveTagLibrary("http://java.sun.com/jstl/core_rt")
        assert tagLib

        JspTag formatNumberTag = tagLib.getTag("forEach")
        assert formatNumberTag

        def writer = new StringWriter()

        JstlUtils.exposeLocalizationContext webRequest.getRequest(),null

        int count = 0
        def pageContext = PageContextFactory.getCurrent()
        def array = []
        formatNumberTag.doTag(writer, [items:[1,2,3], var:"num"]) {
            array << pageContext.getAttribute("num")
            count++
        }

        assertEquals 3, count
        assertEquals([1,2,3],array)
        // forEach is a TryCatchFinally tag and should remove all attributes in the scope at the end of the loop
        assertNull pageContext.getAttribute("num")
    }
}
