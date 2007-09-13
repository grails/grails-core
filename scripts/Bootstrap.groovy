import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.plugins.*
import org.springframework.core.io.*
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

task ('default': "This task will load the Grails application context into the command window with a variable named 'ctx'") {
	bootstrap()
}
       
task(loadApp:"Loads the Grails application object") {
	def beans = new grails.spring.BeanBuilder().beans {
		resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
			resources = "file:${basedir}/**/grails-app/**/*.groovy"
		}
		grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
			grailsResourceHolder = resourceHolder
		}
		grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
		pluginMetaManager(DefaultPluginMetaManager, resolveResources("file:${basedir}/plugins/*/plugin.xml"))
	}
                                                    
	appCtx = beans.createApplicationContext()
	def ctx = appCtx
	ctx.servletContext = new MockServletContext()
	grailsApp = ctx.grailsApplication 
	ApplicationHolder.application = grailsApp
	
    pluginManager = new DefaultGrailsPluginManager(pluginResources as Resource[], grailsApp)

   	PluginManagerHolder.setPluginManager(pluginManager)
   	pluginManager.loadPlugins()
	pluginManager.doArtefactConfiguration()
	grailsApp.initialise()	 	
}                                      
task(configureApp:"Configures the Grails application and builds an ApplicationContext") {
	def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
	appCtx = config.configure(new MockServletContext())	
}                                                                                  

task(bootstrap: "The implementation task") {  
	depends(classpath)
	
	def parent = new ClassPathXmlApplicationContext("applicationContext.xml");
	application = parent.getBean("grailsApplication"); 
	
	Thread.currentThread().setContextClassLoader(application.getClassLoader())
	
	def config = new GrailsRuntimeConfigurator(application,parent);
	servletContext = new MockServletContext();
	ctx = config.configure(servletContext);
	servletContext.setAttribute( ApplicationAttributes.APPLICATION_CONTEXT, ctx);	
}