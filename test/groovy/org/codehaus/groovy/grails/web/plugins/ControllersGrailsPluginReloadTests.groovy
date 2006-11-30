import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ControllersGrailsPluginReloadTests extends AbstractGrailsResourceTests {
	
	def tmpFile
	def ant = new AntBuilder()
	def path = "controllerReloadTests/grails-app/controllers"
	
	void onSetUp() {
		
		
		ant.mkdir(dir:path)
		tmpFile = new File("${path}/TestController.groovy")
		tmpFile.write(
"""
class TestController {
   def list = { "hello" }
}
"""	)
		sleep(1000)
		resourcePattern = "classpath*:${path}/*.groovy"
	}
	
	void onTearDown() {
		ant.delete(dir:"controllerReloadTests")	
	}
	
	void testReloadChanges() {

	   def application = ga
      
	   assert tmpFile.exists()
	   assertEquals 1, resources.size()
	   assertEquals 1, application.controllers.size()
	   
	   assert application.getController("TestController") != null
	   
    	def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.web.plugins.ControllersGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
	    plugin.applicationContext = applicationContext
		plugin.watchedResources = resources
		
		plugin.checkForChanges()
		
		assertEquals "hello", application.getController("TestController").newInstance().list()

		resources[0].file.write(
"""
class TestController {
   def list = { "goodbye" }
}
"""	)	
        	
		sleep(1000)
		plugin.checkForChanges()
		
		assertEquals "goodbye", application.getController("TestController").newInstance().list()
		
	}
}