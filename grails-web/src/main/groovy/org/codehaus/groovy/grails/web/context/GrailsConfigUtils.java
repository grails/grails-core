/*
 * Copyright 2004-2005 the original author or authors.
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

import java.lang.reflect.Constructor;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.BootstrapArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsBootstrapClass;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * A common class where shared configurational methods can reside.
 *
 * @author Graeme Rocher
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
        if (beanNames.length > 0) {
            interceptor = (PersistenceContextInterceptor)webContext.getBean(beanNames[0]);
        }

        if (interceptor != null) {
            interceptor.init();
        }
        // init the Grails application
        try {
            GrailsClass[] bootstraps = application.getArtefacts(BootstrapArtefactHandler.TYPE);
            for (GrailsClass bootstrap : bootstraps) {
                final GrailsBootstrapClass bootstrapClass = (GrailsBootstrapClass) bootstrap;
                final Object instance = bootstrapClass.getReferenceInstance();
                webContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                        instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
                bootstrapClass.callInit(servletContext);
            }
            if (interceptor != null) {
                interceptor.flush();
            }
        }
        finally {
            if (interceptor != null) {
                interceptor.destroy();
            }
        }
    }

    public static WebApplicationContext configureWebApplicationContext(ServletContext servletContext, WebApplicationContext parent) {
        ServletContextHolder.setServletContext(servletContext);
        GrailsApplication application = (GrailsApplication)parent.getBean(GrailsApplication.APPLICATION_ID);

        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrailsContextLoader] Configuring Grails Application");
        }

        if (application.getParentContext() == null) {
            application.setApplicationContext(parent);
        }

        GrailsRuntimeConfigurator configurator = null;
        if (parent.containsBean(GrailsRuntimeConfigurator.BEAN_ID)) {
            // get configurator from parent application context
            configurator = (GrailsRuntimeConfigurator)parent.getBean(GrailsRuntimeConfigurator.BEAN_ID);
        }
        else {
            // get configurator from servlet context
            configurator = determineGrailsRuntimeConfiguratorFromServletContext(application, servletContext, parent);
        }

        if (configurator == null) {
            // no configurator, use default
            configurator = new GrailsRuntimeConfigurator(application,parent);
            if (parent.containsBean(GrailsPluginManager.BEAN_NAME)) {
                GrailsPluginManager pluginManager = (GrailsPluginManager)parent.getBean(GrailsPluginManager.BEAN_NAME);
                configurator.setPluginManager(pluginManager);
            }
        }

        final GrailsPluginManager pluginManager = configurator.getPluginManager();

        // return a context that obeys grails' settings
        WebApplicationContext webContext = configurator.configure(servletContext);
        pluginManager.setApplicationContext(webContext);

        configureServletContextAttributes(servletContext, application, pluginManager, webContext);
        LOG.info("[GrailsContextLoader] Grails application loaded.");
        return webContext;
    }

    public static void configureServletContextAttributes(ServletContext servletContext, GrailsApplication application, GrailsPluginManager pluginManager, WebApplicationContext webContext) {
        servletContext.setAttribute(ApplicationAttributes.PLUGIN_MANAGER, pluginManager);
        // use config file locations if available
        servletContext.setAttribute(ApplicationAttributes.PARENT_APPLICATION_CONTEXT, webContext.getParent());
        servletContext.setAttribute(GrailsApplication.APPLICATION_ID, application);

        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, webContext);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webContext);
    }

    public static GrailsRuntimeConfigurator determineGrailsRuntimeConfiguratorFromServletContext(
            GrailsApplication application, ServletContext servletContext, ApplicationContext parent) {
        GrailsRuntimeConfigurator configurator = null;

        if (servletContext.getInitParameter(GrailsRuntimeConfigurator.BEAN_ID + "Class") != null) {
            // try to load configurator class as specified in servlet context
            String configuratorClassName = servletContext.getInitParameter(
                    GrailsRuntimeConfigurator.BEAN_ID + "Class").toString();
            Class<?> configuratorClass = null;
            try {
                configuratorClass = ClassUtils.forName(configuratorClassName, application.getClassLoader());
            }
            catch (Exception e) {
                String msg = "failed to create Grails runtime configurator as specified in web.xml: " + configuratorClassName;
                LOG.error("[GrailsContextLoader] " + msg, e);
                throw new IllegalArgumentException(msg);
            }

            if (!GrailsRuntimeConfigurator.class.isAssignableFrom(configuratorClass)) {
                throw new IllegalArgumentException("class " + configuratorClassName +
                        " is not assignable to " + GrailsRuntimeConfigurator.class.getName());
            }
            Constructor<?> constr = ClassUtils.getConstructorIfAvailable(
                    configuratorClass, GrailsApplication.class, ApplicationContext.class);
            configurator = (GrailsRuntimeConfigurator) BeanUtils.instantiateClass(
                    constr, application, parent);
        }

        return configurator;
    }

    /**
     * Checks if a Config parameter is true or a System property with the same name is true
     *
     * @param application
     * @param propertyName
     * @return true if the Config parameter is true or the System property with the same name is true
     */
    public static boolean isConfigTrue(GrailsApplication application, String propertyName) {
        return ((application != null && application.getFlatConfig() != null && DefaultTypeTransformation.castToBoolean(application.getFlatConfig().get(propertyName))) ||
                Boolean.getBoolean(propertyName));
    }

    // support GrailsApplication mocking, see ControllersGrailsPluginTests
    public static boolean isConfigTrue(Object application, String propertyName) {
        return false;
    }
}
