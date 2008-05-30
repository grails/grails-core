package org.codehaus.groovy.grails.commons;

import java.io.IOException;

import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.core.io.Resource;


public class GrailsPluginManagerTests extends AbstractGrailsMockTests {

	private static final String RESOURCE_PATH = "classpath:org/codehaus/groovy/grails/plugins/ClassEditorGrailsPlugin.groovy";

    protected void onSetUp() {
        super.onSetUp();

        gcl.parseClass('''
dataSource {
	pooled = false
	driverClassName = "org.hsqldb.jdbcDriver"
	username = "sa"
	password = ""
    dbCreate = "create-drop" // one of 'create', 'create-drop','update'
    url = "jdbc:hsqldb:mem:devDB"

}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='org.hibernate.cache.OSCacheProvider'
}
        ''')
    }



    public void testObservablePlugin() {
         def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin, ObservingGrailsPlugin] as Class[], ga)

         manager.loadPlugins()
         
         assertTrue manager.hasGrailsPlugin("another")

         // Get the "another" plugin and all the plugins that are
         // observing it.
         def plugin = manager.getGrailsPlugin("another")
         def observers = manager.getPluginObservers(plugin)

         // Check that the observers are what we expect (note that the
         // core plugin "logging" should be one of them).
         def expectedObservers = ["logging", "observing"]
         assertTrue observers*.name.containsAll(expectedObservers)
         assertEquals expectedObservers.size(), observers.size()

         // Also check that the "logging" plugin is observing the "my"
         // plugin (which is not observed by any other plugins).
         observers = manager.getPluginObservers(manager.getGrailsPlugin("my"))
         expectedObservers = ["logging"]

         assertTrue observers*.name.containsAll(expectedObservers)
         assertEquals expectedObservers.size(), observers.size()

         // Make sure the observers are being notified of changes to
         // the observed plugin.
         def event = [source:"foo"]
         manager.informObservers("another", event)

         assertEquals "bar", event.source
    }	

    public void testNoSelfObserving() {
        def manager = new DefaultGrailsPluginManager([AnotherGrailsPlugin,ObservingAllGrailsPlugin] as Class[], ga)

        manager.loadPlugins()

        // Get the "another" plugin and all the plugins that are
        // observing it.
        def plugin = manager.getGrailsPlugin("another")
        def observers = manager.getPluginObservers(plugin)

        // Check that the observers are what we expect (note that the
        // core plugin "logging" should be one of them).
        def expectedObservers = ["logging", "observingAll"]
        assertTrue observers*.name.containsAll(expectedObservers)
        assertEquals expectedObservers.size(), observers.size()

        // Now check that the "observingAll" plugin is *not* observing
        // itself.
        observers = manager.getPluginObservers(manager.getGrailsPlugin("observingAll"))
        expectedObservers = ["logging"]

        assertTrue observers*.name.containsAll(expectedObservers)
        assertEquals expectedObservers.size(), observers.size()
    }

	public void testDisabledPlugin() {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin,DisabledGrailsPlugin] as Class[], ga)

		manager.loadPlugins()

	    assertTrue manager.hasGrailsPlugin("my")
	    assertNotNull manager.getGrailsPlugin("my").instance
	    assertFalse manager.hasGrailsPlugin("disabled")
    }

	public void testDefaultGrailsPluginManager() throws Exception {
		DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(RESOURCE_PATH,ga);
		assertEquals(1, manager.getPluginResources().length);
	}

	public void testLoadPlugins() throws Exception {
        println "LOAD PLUGINS"
        GrailsPluginManager manager = new DefaultGrailsPluginManager(RESOURCE_PATH,ga);
		manager.loadPlugins();


		GrailsPlugin plugin = manager.getGrailsPlugin("classEditor");
		assertNotNull(plugin);
		assertEquals("classEditor",plugin.getName());
		assertEquals("1.1", plugin.getVersion());
		
		plugin = manager.getGrailsPlugin("classEditor", "1.1");
		assertNotNull(plugin);
		
		plugin = manager.getGrailsPlugin("classEditor", "1.2");
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
			assert !manager.hasGrailsPlugin("my")
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
        parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
		
		def springConfig = new org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration(parent)
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
		parent.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
		def springConfig = new org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration(parent)
		springConfig.servletContext = createMockServletContext()
		
		manager.doRuntimeConfiguration(springConfig)
		
		def ctx = springConfig.getApplicationContext()
		
		assert ctx.containsBean("classEditor")

		manager.doPostProcessing(ctx)
		
		assert ctx.containsBean("localeResolver")
	}


	public void testEviction() {
		def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin,SomeOtherGrailsPlugin,ShouldEvictSomeOtherGrailsPlugin] as Class[], ga)

		manager.loadPlugins()

		assertFalse manager.hasGrailsPlugin("someOther")
		assertTrue manager.hasGrailsPlugin("my")
		assertTrue manager.hasGrailsPlugin("another")
		assertTrue manager.hasGrailsPlugin("shouldEvictSomeOther")
	}

    void testShutdownCalled() {
        def manager = new DefaultGrailsPluginManager([MyGrailsPlugin,AnotherGrailsPlugin] as Class[], ga)

        manager.loadPlugins()

        assertEquals "nullme",MyGrailsPlugin.SHUTDOWN_FIELD
        manager.shutdown()
        assertNull MyGrailsPlugin.SHUTDOWN_FIELD
    }

}
class MyGrailsPlugin {
    static SHUTDOWN_FIELD = "nullme"
    def dependsOn = [another:1.2]
	def version = 1.1
	def doWithSpring = {
		classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader )				
	}
    def onShutdown = {
         SHUTDOWN_FIELD = null
    }
}
class AnotherGrailsPlugin {
	def version = 1.2
    def watchedResources = ['classpath:org/codehaus/groovy/grails/plugins/*.xml']
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
class ShouldEvictSomeOtherGrailsPlugin {
	def evict = ['someOther']
	def version = 1.1
}
class DisabledGrailsPlugin {
    def version = 1.0
    def status = "disabled"
}
class ObservingGrailsPlugin {
    def version = "1.0-RC1"
    def observe = ['another']

    def onChange = { event ->
        assert event.source != null
        event.source = "bar"
    }
}
class ObservingAllGrailsPlugin {
    def version = "1.0"
    def observe = ['*']
}
