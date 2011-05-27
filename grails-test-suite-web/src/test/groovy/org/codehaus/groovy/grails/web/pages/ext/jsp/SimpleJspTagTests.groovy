package org.codehaus.groovy.grails.web.pages.ext.jsp

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPagesServlet
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.JstlUtils

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class SimpleJspTagTests extends GroovyTestCase {

    GrailsWebRequest webRequest

    protected void setUp() {
        webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
    }

    protected void tearDown() {
        RequestContextHolder.setRequestAttributes null
    }

    void testSimpleTagUsage() {

        def resolver = new MockRootLoaderTagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication = new DefaultGrailsApplication()

        JspTagLib tagLib = resolver.resolveTagLibrary("http://java.sun.com/jsp/jstl/fmt")

        assert tagLib

        JspTag formatNumberTag = tagLib.getTag("formatNumber")

        assert formatNumberTag

        def writer = new StringWriter()

        JstlUtils.exposeLocalizationContext webRequest.getRequest(),null
        formatNumberTag.doTag writer, [value:"10", pattern:".00"]

        assertEquals "10.00", writer.toString()
    }
}
