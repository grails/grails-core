import grails.spring.WebBeanBuilder
import org.codehaus.groovy.grails.cli.support.CommandLineResourceLoader
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )  

target ('default': "This target will load the Grails application context into the command window with a variable named 'ctx'") {
	bootstrap()
}

parentContext = null // default parent context is null
       
target(loadApp:"Loads the Grails application object") {
	event("AppLoadStart", ["Loading Grails Application"])
	profile("Loading parent ApplicationContext") {
		def builder = parentContext ? new WebBeanBuilder(parentContext) :  new WebBeanBuilder()
		beanDefinitions = builder.beans {
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				this.resources = "file:${basedir}/**/grails-app/**/*.groovy"
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
			pluginMetaManager(DefaultPluginMetaManager, resolveResources("file:${basedir}/plugins/*/plugin.xml"))
		}		
	}
                                                    
	appCtx = beanDefinitions.createApplicationContext()
	def ctx = appCtx

    // The mock servlet context needs to resolve resources relative to the 'web-app'
    // directory. We also need to use a FileSystemResourceLoader, otherwise paths are
    // evaluated against the classpath - not what we want!
    servletContext = new MockServletContext('web-app', new FileSystemResourceLoader())
    ctx.servletContext = servletContext
	grailsApp = ctx.grailsApplication 
	ApplicationHolder.application = grailsApp
	classLoader = grailsApp.classLoader
	packageApp()
    PluginManagerHolder.pluginManager = null
    loadPlugins()
    pluginManager = PluginManagerHolder.pluginManager
    pluginManager.application = grailsApp
    pluginManager.doArtefactConfiguration()
    grailsApp.initialise()
	event("AppLoadEnd", ["Loading Grails Application"])
}                                      
target(configureApp:"Configures the Grails application and builds an ApplicationContext") {
	event("ConfigureAppStart", [grailsApp, appCtx])	
    appCtx.resourceLoader = new  CommandLineResourceLoader()
	profile("Performing runtime Spring configuration") {
	    def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
        appCtx = config.configure(servletContext)
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,appCtx );
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
	}
	event("ConfigureAppEnd", [grailsApp, appCtx])		
}

target(shutdownApp:"Shuts down the running Grails application") {
    appCtx?.close()
    ApplicationHolder.setApplication(null);
    ServletContextHolder.setServletContext(null);
    PluginManagerHolder.setPluginManager(null);
    ConfigurationHolder.setConfig(null);    
}

monitorCallback = {}

target(monitorApp:"Monitors an application for changes using the PluginManager and reloads changes") {
    long lastModified = classesDir.lastModified()
    while(true) {
        sleep(3500)
        try {
            pluginManager.checkForChanges()

            lastModified = recompileCheck(lastModified) {
                compile()
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader()
                classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], contextLoader.rootLoader)
                Thread.currentThread().setContextClassLoader(classLoader)
                // reload plugins
                loadPlugins()
                loadApp()
                configureApp()
                monitorCallback()
            }

        } catch (Exception e) {
            println e.message
        }
    }
}

target(bootstrap: "The implementation target") {  
	depends(loadApp, configureApp)
}
