/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.pages.ext.jsp

import javax.servlet.jsp.PageContext as PC
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.pages.GroovyPagesServlet
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GAA
import javax.servlet.jsp.PageContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import javax.servlet.ServletContext

/**
 * Obtains a reference to the GroovyPagesPageContext class
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 10, 2008
 */
class PageContextFactory {

    static Class pageContextClass

    static {
        def classLoader = Thread.currentThread().getContextClassLoader()

        if(PageContext.metaClass.getMetaMethod('getElContext',[] as Class[])) {
            pageContextClass = classLoader.loadClass('org.codehaus.groovy.grails.web.pages.ext.jsp.GrooovyPagesPageContext21')
        }
        else {
            pageContextClass = classLoader.loadClass('org.codehaus.groovy.grails.web.pages.ext.jsp.GroovyPagesPageContext')
        }
    }


    public static GroovyPagesPageContext getCurrent() {
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()

        def request = webRequest.getCurrentRequest()

        def pageContext = request.getAttribute(PageContext.PAGECONTEXT)
        if(pageContext instanceof GroovyPagesPageContext) return pageContext
        else {
            ServletContext servletContext = webRequest.getServletContext()
            def gspServlet = servletContext.getAttribute(GroovyPagesServlet.SERVLET_INSTANCE)
            if(!gspServlet) {
                gspServlet = new GroovyPagesServlet()
                servletContext.setAttribute GroovyPagesServlet.SERVLET_INSTANCE, gspServlet
            }
            def pageScope = request.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE)
            if(!pageScope) {
                pageScope = new Binding()
                request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, pageScope)
            }

            pageContext = pageContextClass.newInstance(gspServlet, pageScope)
            request.setAttribute(PageContext.PAGECONTEXT, pageContext)

        }
                
        return pageContext
    }
}