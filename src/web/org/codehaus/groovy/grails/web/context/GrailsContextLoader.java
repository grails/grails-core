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

import grails.util.GrailsUtil;
import groovy.lang.ExpandoMetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * @author graemerocher
 *
 */
public class GrailsContextLoader extends ContextLoader {

	public static final Log LOG = LogFactory.getLog(GrailsContextLoader.class);
    GrailsApplication application;

    protected WebApplicationContext createWebApplicationContext(ServletContext servletContext, ApplicationContext parent) throws BeansException {

        ExpandoMetaClass.enableGlobally();

        if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Loading context. Creating parent application context");
		}
        WebApplicationContext  ctx = null;
        try {
            ctx = super.createWebApplicationContext(servletContext, parent);

            application = (GrailsApplication) ctx.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            ctx =  GrailsConfigUtils.configureWebApplicationContext(servletContext, ctx);
            GrailsConfigUtils.executeGrailsBootstraps(application, ctx, servletContext);
        } catch (Exception e) {
            GrailsUtil.deepSanitize(e);
            if(e instanceof BeansException) throw (BeansException)e;
            else {
                throw new BootstrapException("Error executing bootstraps", e);
            }
        }
        return ctx;
    }

    public void closeWebApplicationContext(ServletContext servletContext) {
        super.closeWebApplicationContext(servletContext);    

        // clean up in war mode, in run-app these references may be needed again
        if(application!= null && application.isWarDeployed()) {
            ApplicationHolder.setApplication(null);
            ServletContextHolder.setServletContext(null);
            GrailsPluginManager pluginManager = PluginManagerHolder.currentPluginManager();
            try {
                pluginManager.shutdown();
            } catch (Exception e) {
                GrailsUtil.sanitize(e);
                LOG.error("Error occurred shutting down plug-in manager: " + e.getMessage(), e);
            }
            PluginManagerHolder.setPluginManager(null);
            ConfigurationHolder.setConfig(null);
            application = null;
        }

    }
}
