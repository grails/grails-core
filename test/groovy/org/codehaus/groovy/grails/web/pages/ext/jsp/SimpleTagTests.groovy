package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.pages.GroovyPagesServlet
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import javax.servlet.jsp.tagext.SimpleTagSupport
import javax.servlet.jsp.JspException
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder
import javax.servlet.jsp.JspWriter

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2009
 */

public class SimpleTagTests extends GroovyTestCase{
    GrailsWebRequest webRequest
    protected void setUp() {
        webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
     }

     protected void tearDown() {
         RequestContextHolder.setRequestAttributes null
     }


    void testSimpleTagWithBodyUsage() {
        def resolver = new MockRootLoaderTagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication = new DefaultGrailsApplication()

        JspTag jspTag = new JspTagImpl(BodySimpleTagSupport)
        def sw = new StringWriter()
        jspTag.doTag(sw, [:]) {
            "testbody"
        }

        assertEquals "bodySimpleTagSupport:testbody", sw.toString().trim()

    }

    void testSimpleTagUsage() {



        def resolver = new MockRootLoaderTagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication = new DefaultGrailsApplication()

        JspTag jspTag = new JspTagImpl(ExtendsSimpleTagSupport)
        def sw = new StringWriter()
        jspTag.doTag(sw, [:])

        assertEquals "extendsSimpleTagSupport:output", sw.toString().trim()
    }


}
class ExtendsSimpleTagSupport extends SimpleTagSupport {
    @Override
    public void doTag() throws JspException, IOException {
        getJspContext().getOut().println("extendsSimpleTagSupport:output");
    }
}
class BodySimpleTagSupport extends SimpleTagSupport {
    @Override
    public void doTag() throws JspException, IOException {

        JspWriter out = getJspContext().getOut()
        out.print("bodySimpleTagSupport:");
        super.getJspBody().invoke(out)
    }
}