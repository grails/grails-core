package org.codehaus.groovy.grails.web.context

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.mock.web.MockServletContext
import grails.util.Environment

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class GrailsConfigUtilsTests extends GroovyTestCase{


    void testExecuteBootstraps() {
        def app = new DefaultGrailsApplication([SimpleBootStrap] as Class[], getClass().classLoader)
        app.initialise()
        def ctx = new MockApplicationContext()
        def servletContext = new MockServletContext()
        GrailsConfigUtils.executeGrailsBootstraps(app, ctx, servletContext)

        assertEquals "bar", servletContext.getAttribute("foo")
    }

    void testExecuteEnvironmentSpecificBootstraps() {
        System.setProperty("grails.env", "dev")
        
        def app = new DefaultGrailsApplication([EnvironmentSpecificBootStrap] as Class[], getClass().classLoader)
        app.initialise()
        def ctx = new MockApplicationContext()
        def servletContext = new MockServletContext()
        GrailsConfigUtils.executeGrailsBootstraps(app, ctx, servletContext)

        assertEquals "bar", servletContext.getAttribute("foo")
        assertEquals "dev", servletContext.getAttribute("env")

    }
    protected void tearDown() {
        System.setProperty(Environment.KEY, "")

    }
}
class SimpleBootStrap {
    def init = { ServletContext ctx ->
        ctx.setAttribute("foo", "bar")
    }
}
class EnvironmentSpecificBootStrap {
    def init = { ServletContext ctx ->
        environments {
            production {
                ctx.setAttribute("env", "prod")
            }
            development {
                ctx.setAttribute("env", "dev")
            }
        }
        ctx.setAttribute("foo", "bar")
    }

    def destroy = {        
    }
}