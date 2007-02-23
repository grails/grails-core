import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

task ('default': "This task will load the Grails application context into the command window with a variable named 'ctx'") {
	bootstrap()
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