import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ControllersGrailsPluginTests extends AbstractGrailsMockTests {
	
	
	void onSetUp() {
		gcl.parseClass(
"""
class TestController {
   def list = {}			
}
""")
	}
	
	void testControllersPlugin() {
		def pluginFile = getClass().classLoader.getResource("org/codehaus/groovy/grails/web/plugins/ControllersGrailsPlugin.groovy")
		
		assert pluginFile != null
		
		def pluginClass = gcl.parseClass(pluginFile.openStream())
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("TestControllerTargetSource")
		assert appCtx.containsBean("TestControllerProxy")
		assert appCtx.containsBean("TestControllerClass")
		assert appCtx.containsBean("TestController")
	}
	
	void testReloadChanges() {
		
	}
}