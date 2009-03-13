package org.codehaus.groovy.grails.web.pages.ext.jsp

import javax.servlet.jsp.JspApplicationContext
import javax.servlet.ServletContext

/**
 * To support JSP 2.1 engines we need to provide a custom JspFactory
 * 
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 10, 2008
 */
class GroovyPagesJspFactory21 extends GroovyPagesJspFactory{

    protected String getSpecificationVersion() { "2.1" }

    JspApplicationContext getJspApplicationContext(ServletContext servletContext) {
        def jspCtx = servletContext.getAttribute(GroovyPagesJspApplicationContext.getName())

        if(!jspCtx) {
            synchronized(servletContext) {
                if(!servletContext.getAttribute(GroovyPagesJspApplicationContext.getName())) {
                    jspCtx = new GroovyPagesJspApplicationContext()
                    servletContext.setAttribute(GroovyPagesJspApplicationContext.getName(), jspCtx)
                }
            }
        }
        return jspCtx
    }

}