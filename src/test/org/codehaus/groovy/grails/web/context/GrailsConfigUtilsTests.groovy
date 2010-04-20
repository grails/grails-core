package org.codehaus.groovy.grails.web.context

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.context.ApplicationContext;
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
    
    void testDerivedGrailsConfigurator() {
        def app = new DefaultGrailsApplication([SimpleBootStrap] as Class[], getClass().classLoader)
        app.initialise()
        def ctx = new MockApplicationContext()
        def servletContext = new MockServletContext()
        servletContext.addInitParameter("grailsConfiguratorClass", MyGrailsRuntimeConfigurator.class.getName())
        
        def configurator = GrailsConfigUtils.determineGrailsRuntimeConfiguratorFromServletContext(app, servletContext, ctx);

        assertNotNull configurator
        assertEquals configurator.class.name, MyGrailsRuntimeConfigurator.class.name
    }
    
    void testNonExistingGrailsConfigurator() {
        def app = new DefaultGrailsApplication([SimpleBootStrap] as Class[], getClass().classLoader)
        app.initialise()
        def ctx = new MockApplicationContext()
        def servletContext = new MockServletContext()
        servletContext.addInitParameter("grailsConfiguratorClass", "org.codehaus.groovy.grails.web.context.ClassDoesNotExist")
        
        try {
        	def configurator = GrailsConfigUtils.determineGrailsRuntimeConfiguratorFromServletContext(app, servletContext, ctx);
        	
        	fail "expected IllegalArgumentException because of invalid class name"
        }
        catch (IllegalArgumentException e) {
        	// expected
        }
    }
    
    void testDefaultGrailsConfigurator() {
        def app = new DefaultGrailsApplication([SimpleBootStrap] as Class[], getClass().classLoader)
        app.initialise()
        def ctx = new MockApplicationContext()
        def servletContext = new MockServletContext()
        
        def configurator = GrailsConfigUtils.determineGrailsRuntimeConfiguratorFromServletContext(app, servletContext, ctx);

        assertNull configurator
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
class MyGrailsRuntimeConfigurator extends GrailsRuntimeConfigurator {

	public MyGrailsRuntimeConfigurator(GrailsApplication application,
			ApplicationContext parent) {
		super(application, parent);
	}

	public MyGrailsRuntimeConfigurator(GrailsApplication application) {
		super(application);
	}
}