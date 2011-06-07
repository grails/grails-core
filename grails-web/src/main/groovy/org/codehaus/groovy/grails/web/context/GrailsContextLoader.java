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

import grails.util.Environment;
import grails.util.GrailsUtil;
import grails.util.Metadata;
import groovy.grape.Grape;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.security.AccessControlException;

/**
 * @author graemerocher
 */
public class GrailsContextLoader extends ContextLoader {

    public static final Log LOG = LogFactory.getLog(GrailsContextLoader.class);
    GrailsApplication application;

    @Override
    protected WebApplicationContext createWebApplicationContext(ServletContext servletContext, ApplicationContext parent) throws BeansException {
        // disable annoying ehcache up-to-date check
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
        ExpandoMetaClass.enableGlobally();
        Metadata metadata = Metadata.getCurrent();
        if (metadata != null && metadata.isWarDeployed()) {
            Grape.setEnableAutoDownload(false);
            Grape.setEnableGrapes(false);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrailsContextLoader] Loading context. Creating parent application context");
        }

        WebApplicationContext  ctx;
        try {
            ctx = super.createWebApplicationContext(servletContext, parent);

            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsContextLoader] Created parent application context");
            }

            application = ctx.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            ctx =  GrailsConfigUtils.configureWebApplicationContext(servletContext, ctx);
            GrailsConfigUtils.executeGrailsBootstraps(application, ctx, servletContext);
        }
        catch (Throwable e) {
            if (Environment.isDevelopmentMode()) {
                LOG.error("Error executing bootstraps: " + e.getMessage(), e);
                // bail out early in order to show appropriate error
                System.exit(1);
                return null;
            }

            if (e instanceof BeansException) throw (BeansException)e;

            throw new BootstrapException("Error executing bootstraps", e);
        }
        return ctx;
    }

    @Override
    public void closeWebApplicationContext(ServletContext servletContext) {
        // clean up in war mode, in run-app these references may be needed again
        if (application == null || !application.isWarDeployed()) {
            return;
        }

        if (application != null) {
            ClassLoader classLoader = application.getClassLoader();
            if (classLoader instanceof GroovyClassLoader) {
                MetaClassRegistry metaClassRegistry = GroovySystem.getMetaClassRegistry();
                Class<?>[] loadedClasses = ((GroovyClassLoader)classLoader).getLoadedClasses();
                for (Class<?> loadedClass : loadedClasses) {
                    metaClassRegistry.removeMetaClass(loadedClass);
                }
            }
        }

        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        GrailsPluginManager pluginManager = null;
        if (ctx.containsBean(GrailsPluginManager.BEAN_NAME)) {
            pluginManager = ctx.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
        }

        if (pluginManager != null) {
            try {
                pluginManager.shutdown();
            }
            catch (Exception e) {
                GrailsUtil.sanitize(e);
                LOG.error("Error occurred shutting down plug-in manager: " + e.getMessage(), e);
            }
        }

        ConfigurableApplicationContext parent = ctx != null ? (ConfigurableApplicationContext) ctx.getParent() : null;

        super.closeWebApplicationContext(servletContext);

        if (parent != null) {
            LOG.info("Destroying Spring parent WebApplicationContext " + parent.getDisplayName());
            parent.close();
        }

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        }
        catch (AccessControlException e) {
            // container doesn't allow, probably related to WAR deployment on AppEngine. proceed.
        }

        PluginManagerHolder.setPluginManager(null);
        ConfigurationHolder.setConfig(null);
        ConfigurationHelper.clearCachedConfigs();
        ExpandoMetaClass.disableGlobally();
        MimeType.reset();
        application = null;
    }
}
