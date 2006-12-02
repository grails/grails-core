package org.codehaus.groovy.grails.plugins;

import java.io.IOException;

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;


public class GrailsPluginManagerTests extends AbstractGrailsMockTests {

	private static final String RESOURCE_PATH = "classpath:org/codehaus/groovy/grails/plugins/ClassEditorGrailsPlugin.groovy";

	public void testDefaultGrailsPluginManager() throws Exception {
		DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(RESOURCE_PATH,ga);
		assertEquals(1, manager.getPluginResources().length);
	}

	public void testLoadPlugins() throws Exception {
		GrailsPluginManager manager = new DefaultGrailsPluginManager(RESOURCE_PATH,ga);
		manager.loadPlugins();
		
		GrailsPlugin plugin = manager.getGrailsPlugin("classEditor");
		assertNotNull(plugin);
		assertEquals("classEditor",plugin.getName());
		assertEquals(1.1, plugin.getVersion());
		
		plugin = manager.getGrailsPlugin("classEditor", 1.1);
		assertNotNull(plugin);
		
		plugin = manager.getGrailsPlugin("classEditor", 1.2);
		assertNull(plugin);
	}
	
	public void testWithLoadLastPlugin() throws Exception {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin,ShouldLoadLastGrailsPlugin] as Class[], ga)
		
		manager.loadPlugins()
		
	}
	
	public void testDependencyResolutionFailure() throws Exception {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin] as Class[], ga)
		
		try {
			manager.loadPlugins()
			fail("Should have failed with a dependency resolution error")
		}		
		catch(PluginException pe) {
			// expected
		}
	}
	
	public void testDependencyResolutionSucces() throws Exception {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin, SomeOtherGrailsPlugin] as Class[], ga)
		
		manager.loadPlugins()
	}

	public void testDoRuntimeConfiguration() {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin] as Class[], ga)
		
		manager.loadPlugins()
		
		def parent = createMockApplicationContext()
		parent.registerMockBean("grailsApplication", ga)
		
		def springConfig = new org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration(parent)
		springConfig.servletContext = createMockServletContext()
		manager.doRuntimeConfiguration(springConfig)
		
		def ctx = springConfig.getApplicationContext()
		
		assert ctx.containsBean("classEditor")
	}

	public void testDoPostProcessing() {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin] as Class[], ga)
		
		manager.loadPlugins()
		
		def parent = createMockApplicationContext()
		parent.registerMockBean("grailsApplication", ga)
		def springConfig = new org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration(parent)
		springConfig.servletContext = createMockServletContext()
		
		manager.doRuntimeConfiguration(springConfig)
		
		def ctx = springConfig.getApplicationContext()
		
		assert ctx.containsBean("classEditor")

		manager.doPostProcessing(ctx)
		
		assert ctx.containsBean("localeResolver")
	}

}
class MyGrailsPlugin {
	def dependsOn = [another:1.2]
	def version = 1.1
	def doWithSpring = {
		classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader )				
	}
}
class AnotherGrailsPlugin {
	def version = 1.2	
	def doWithApplicationContext = { ctx ->
    	RootBeanDefinition bd = new RootBeanDefinition(CookieLocaleResolver.class);
    	ctx.registerBeanDefinition("localeResolver", bd);			
	}
}
class SomeOtherGrailsPlugin {
	def version = 1.4
	def dependsOn = [my:1.1, another:1.2]
}
class ShouldLoadLastGrailsPlugin {
	def loadAfter = ["my", "someOther"]
	def version = 1.5	             
}