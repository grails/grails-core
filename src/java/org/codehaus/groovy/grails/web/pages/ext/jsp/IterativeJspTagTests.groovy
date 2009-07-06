package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.pages.GroovyPagesServlet
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.web.servlet.support.JstlUtils
import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.FileSystemResource

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2008
 */
class IterativeJspTagTests extends GroovyTestCase {

    GrailsWebRequest webRequest
    protected void setUp() {
        webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
     }

     protected void tearDown() {
         RequestContextHolder.setRequestAttributes null
         GroovySystem.metaClassRegistry.removeMetaClass TagLibraryResolver
     }


    void testIterativeTag() {



        TagLibraryResolver.metaClass.resolveRootLoader = {->
            def rootLoader = new RootLoader([] as URL[], Thread.currentThread().getContextClassLoader())
            def res = new FileSystemResource("lib/standard-2.4.jar")
            rootLoader.addURL res.getURL()
            return rootLoader
        }
        def resolver =  new TagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication= new DefaultGrailsApplication()

        JspTagLib tagLib = resolver.resolveTagLibrary( "http://java.sun.com/jstl/core_rt" )

        assert tagLib


        JspTag formatNumberTag = tagLib.getTag("forEach")

        assert formatNumberTag

        def writer = new StringWriter()


        JstlUtils.exposeLocalizationContext webRequest.getRequest(),null

        int count = 0
        def pageContext = PageContextFactory.getCurrent()
        def array = []
        formatNumberTag.doTag( writer, [items:[1,2,3], var:"num"] ) {
            array << pageContext.getAttribute("num")
            count++
        }

        assertEquals 3, count
        assertEquals( [1,2,3],array )
        // forEach is a TryCatchFinally tag and should remove all attributes in the scope at the end of the loop
        assertNull pageContext.getAttribute("num")

    }


}