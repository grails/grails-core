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
package org.codehaus.groovy.grails.commons;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffolder;
import org.codehaus.groovy.grails.scaffolding.ScaffoldDomain;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * A common class where shared configurational methods can reside
 *
 * @author Graeme Rocher
 * @since 22-Feb-2006
 */
public class GrailsConfigUtils {
	
	private static final Log LOG = LogFactory.getLog(GrailsConfigUtils.class);

    public static void configureScaffolders(GrailsApplication application, ApplicationContext appContext) {
        GrailsControllerClass[] controllerClasses = application.getControllers();
        for (int i = 0; i < controllerClasses.length; i++) {
            GrailsControllerClass controllerClass = controllerClasses[i];
            if(controllerClass.isScaffolding()) {
                try {
                    GrailsScaffolder gs = (GrailsScaffolder)appContext.getBean(controllerClass.getFullName() + "Scaffolder");
                    if(gs != null) {
                        ScaffoldDomain sd = gs.getScaffoldRequestHandler()
                                                .getScaffoldDomain();

                        GrailsDomainClass dc = application.getGrailsDomainClass(sd.getPersistentClass().getName());
                        if(dc != null) {
                            sd.setIdentityPropertyName(dc.getIdentifier().getName());
                            sd.setValidator(dc.getValidator());
                        }
                    }
                } catch (NoSuchBeanDefinitionException e) {
                    // ignore
                }
            }
        }
    }

	/**
	 * Executes Grails bootstrap classes
	 * 
	 * @param application The Grails ApplicationContext instance
	 * @param webContext The WebApplicationContext instance
	 * @param servletContext The ServletContext instance
	 */
	public static void executeGrailsBootstraps(GrailsApplication application, WebApplicationContext webContext, ServletContext servletContext) {
		
		PersistenceContextInterceptor interceptor = null;
		String[] beanNames = webContext.getBeanNamesForType(PersistenceContextInterceptor.class);
		if(beanNames.length > 0) {
			interceptor = (PersistenceContextInterceptor)webContext.getBean(beanNames[0]);
		}
	
	    if(interceptor != null) {
	    	interceptor.init();
	    	// init the Grails application
	        try {
	            GrailsBootstrapClass[] bootstraps =  application.getGrailsBootstrapClasses();
	            for (int i = 0; i < bootstraps.length; i++) {
	                bootstraps[i].callInit(  servletContext );
	            }
	        }
	        finally {
	        	interceptor.destroy();
	        }
	        
	    }
	}

	public static WebApplicationContext configureWebApplicationContext(ServletContext servletContext, WebApplicationContext parent) {
		GrailsApplication application = (GrailsApplication)parent.getBean(GrailsApplication.APPLICATION_ID);
	
		if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Configurating Grails Application");
		}
		
	    GrailsRuntimeConfigurator configurator = new GrailsRuntimeConfigurator(application,parent);
        if(parent.containsBean(GrailsPluginManager.BEAN_NAME)) {
        	GrailsPluginManager pluginManager = (GrailsPluginManager)parent.getBean(GrailsPluginManager.BEAN_NAME);
        	configurator.setPluginManager(pluginManager);
        }
        servletContext.setAttribute(GrailsApplicationAttributes.PLUGIN_MANAGER, configurator.getPluginManager());
        // use config file locations if available
        servletContext.setAttribute(GrailsApplicationAttributes.PARENT_APPLICATION_CONTEXT,parent);        
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID,application);
        
	    
	    // return a context that obeys grails' settings
	    WebApplicationContext webContext = configurator.configure( servletContext );
	    configurator.getPluginManager().setApplicationContext(webContext);
	    servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT,webContext );
	    LOG.info("[GrailsContextLoader] Grails application loaded.");
		return webContext;
	}
}
