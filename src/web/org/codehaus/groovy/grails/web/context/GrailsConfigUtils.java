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
package org.codehaus.groovy.grails.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * A common class where shared configurational methods can reside
 *
 * @author Graeme Rocher
 * @since 22-Feb-2006
 */
public class GrailsConfigUtils {

	private static final Log LOG = LogFactory.getLog(GrailsConfigUtils.class);

    /**
	 * Executes Grails bootstrap classes
	 *
	 * @param application The Grails ApplicationContext instance
	 * @param webContext The WebApplicationContext instance
	 * @param servletContext The ServletContext instance
	 */
	public static void executeGrailsBootstraps(GrailsApplication application, WebApplicationContext webContext,
        ServletContext servletContext) {

		PersistenceContextInterceptor interceptor = null;
		String[] beanNames = webContext.getBeanNamesForType(PersistenceContextInterceptor.class);
		if(beanNames.length > 0) {
			interceptor = (PersistenceContextInterceptor)webContext.getBean(beanNames[0]);
		}

	    if(interceptor != null)
	    	interceptor.init();
        // init the Grails application
        try {
            GrailsClass[] bootstraps =  application.getArtefacts(BootstrapArtefactHandler.TYPE);
            for (int i = 0; i < bootstraps.length; i++) {
                final GrailsBootstrapClass bootstrapClass = (GrailsBootstrapClass) bootstraps[i];
                final Object instance = bootstrapClass.getReference().getWrappedInstance();
                webContext.getAutowireCapableBeanFactory()
                            .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
                bootstrapClass.callInit(  servletContext );
            }
            if(interceptor != null)
                interceptor.flush();
        }
        finally {
            if(interceptor != null)
                interceptor.destroy();
        }

	}

	public static WebApplicationContext configureWebApplicationContext(ServletContext servletContext, WebApplicationContext parent) {
		GrailsApplication application = (GrailsApplication)parent.getBean(GrailsApplication.APPLICATION_ID);

		if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Configurating Grails Application");
		}

		if(application.getParentContext() == null) {
			application.setApplicationContext(parent);
		}

	    GrailsRuntimeConfigurator configurator;
        if(parent.containsBean(GrailsRuntimeConfigurator.BEAN_ID)) {
            configurator = (GrailsRuntimeConfigurator)parent.getBean(GrailsRuntimeConfigurator.BEAN_ID);
        }
        else {
            configurator = new GrailsRuntimeConfigurator(application,parent);
            if(parent.containsBean(GrailsPluginManager.BEAN_NAME)) {
                GrailsPluginManager pluginManager = (GrailsPluginManager)parent.getBean(GrailsPluginManager.BEAN_NAME);
                configurator.setPluginManager(pluginManager);
            }            
        }

        servletContext.setAttribute(ApplicationAttributes.PLUGIN_MANAGER, configurator.getPluginManager());
        // use config file locations if available
        servletContext.setAttribute(ApplicationAttributes.PARENT_APPLICATION_CONTEXT,parent);
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID,application);

        ServletContextHolder.setServletContext(servletContext);


        // return a context that obeys grails' settings
	    WebApplicationContext webContext = configurator.configure( servletContext );
        configurator.getPluginManager().setApplicationContext(webContext);
	    servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,webContext );
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webContext);
        LOG.info("[GrailsContextLoader] Grails application loaded.");
		return webContext;
	}
}
