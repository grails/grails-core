/* Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

import groovy.lang.GroovyClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

/**
 * Utility class for generating Grails artifacts likes views, controllers etc.
 *
 * @author Graeme Rocher
 * @since 10-Feb-2006
 */
public class GenerateUtils {

    private static Log LOG = LogFactory.getLog(GenerateUtils.class);
    private static final String VIEWS = "view";
    private static final String CONTROLLER = "controller";
	private static final String ALL = "all";

    public static void main(String[] args) throws Exception {
        if(args.length < 2)
            return;

        String type = args[0];
        String domainClassName = args[1];



        ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
        GrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);

        GrailsDomainClass domainClass = findInApplication(application,domainClassName);

        // bootstrap application to try hibernate domain classes
        if(domainClass == null) {
        	GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
            ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)config.configure(new MockServletContext());
        }

        // retry
        domainClass = findInApplication(application,domainClassName);
        if(domainClass == null) {
            LOG.info("Unable to generate ["+type+"] domain class not found for name ["+domainClassName+"]");
            System.exit(0);
        }

        try {
	        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
	
	        GrailsTemplateGenerator generator = (GrailsTemplateGenerator)gcl.parseClass(gcl.getResourceAsStream("org/codehaus/groovy/grails/scaffolding/DefaultGrailsTemplateGenerator.groovy"))
	                                                                            .newInstance();
	        if(!CONTROLLER.equals(type) && !VIEWS.equals(type) && !ALL.equals(type)) {
	            LOG.info("Grails was unable to generate templates for unsupported type ["+type+"]");
	        } else {
	        	if(VIEWS.equals(type) || ALL.equals(type)) {
	        		LOG.info("Generating views for domain class ["+domainClass.getName()+"]");
	        		generator.generateViews(domainClass,".");
	        	}	
	        	if(CONTROLLER.equals(type)|| ALL.equals(type)) {
	        		LOG.info("Generating controller for domain class ["+domainClass.getName()+"]");
	        		generator.generateController(domainClass,".");
	        	}
	        }
        }
        catch(Throwable t) {
        	LOG.info("Error during code generation: " + t.getMessage());
        	LOG.error(t.getMessage(), t);
        }
        finally {
        	System.exit(0);
        }
    }

    /**
     * Finds the specified domain class from the application
     * 
     * @param application The application
     * @param domainClassName The domain class name
     * @return A GrailsDomainClass
     */
    private static GrailsDomainClass findInApplication(GrailsApplication application, String domainClassName) {
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(
            DomainClassArtefactHandler.TYPE, domainClassName);
        if(domainClass == null) {
            domainClass = (GrailsDomainClass) application.getArtefact(
                DomainClassArtefactHandler.TYPE,
                domainClassName.substring(0,1).toUpperCase() + domainClassName.substring(1));
        }
        return domainClass;
    }
}
