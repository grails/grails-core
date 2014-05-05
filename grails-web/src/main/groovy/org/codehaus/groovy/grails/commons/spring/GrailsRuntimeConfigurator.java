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
package org.codehaus.groovy.grails.commons.spring;

import grails.spring.BeanBuilder;
import grails.util.Environment;
import grails.util.Holders;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Handles the runtime configuration of the Grails ApplicationContext.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsRuntimeConfigurator implements ApplicationContextAware {

    public static final String BEAN_ID = "grailsConfigurator";
    public static final String GRAILS_URL_MAPPINGS = "grailsUrlMappings";
    public static final String SPRING_RESOURCES_XML = "/WEB-INF/spring/resources.xml";
    public static final String SPRING_RESOURCES_GROOVY = "/WEB-INF/spring/resources.groovy";
    public static final String SPRING_RESOURCES_CLASS = "resources";
    public static final String OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN = "openSessionInViewInterceptor";
    public static final String TRANSACTION_MANAGER_BEAN = "transactionManager";
    public static final String HIBERNATE_PROPERTIES_BEAN = "hibernateProperties";
    public static final String DIALECT_DETECTOR_BEAN = "dialectDetector";
    public static final String SESSION_FACTORY_BEAN = "sessionFactory";
    public static final String DATA_SOURCE_BEAN = "dataSource";
    public static final String MESSAGE_SOURCE_BEAN = "messageSource";
    public static final String MULTIPART_RESOLVER_BEAN = "multipartResolver";
    public static final String EXCEPTION_HANDLER_BEAN = "exceptionHandler";
    public static final String CUSTOM_EDITORS_BEAN = "customEditors";
    public static final String CLASS_EDITOR_BEAN = "classEditor";
    public static final String CLASS_LOADER_BEAN = "classLoader";

    private static final Log LOG = LogFactory.getLog(GrailsRuntimeConfigurator.class);
    public static final String GRAILS_INITIALIZING = "org.grails.internal.INITIALIZING";

    protected GrailsApplication application;
    protected ApplicationContext parent;
    protected GrailsPluginManager pluginManager;
    protected WebRuntimeSpringConfiguration webSpringConfig;
    protected static final String DEVELOPMENT_SPRING_RESOURCES_XML = "file:./grails-app/conf/spring/resources.xml";


    public GrailsRuntimeConfigurator(GrailsApplication application) {
        this(application, null);
    }

    public GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        this.application = application;
        this.parent = parent;
        initializePluginManager();
    }

    protected void initializePluginManager() {
        try {
            pluginManager = parent == null ? null : parent.getBean(GrailsPluginManager.class);
        } catch (BeansException e) {
            // ignore
        }
        if (pluginManager == null) {
            pluginManager = Holders.getPluginManager();
        }
        if (pluginManager == null) {
            pluginManager = new DefaultGrailsPluginManager("**/plugins/*/**GrailsPlugin.groovy", application);
        }
        Holders.setPluginManager(pluginManager);
    }

    /**
     * Configures the Grails application context at runtime.
     *
     * @return A WebApplicationContext instance
     */
    public WebApplicationContext configure() {
        return configure(null);
    }

    /**
     * Configures the Grails application context at runtime
     *
     * @param context A ServletContext instance
     * @return An ApplicationContext instance
     */
    public WebApplicationContext configure(ServletContext context) {
        return configure(context, true);
    }

    public WebApplicationContext configure(ServletContext context, boolean loadExternalBeans) {
        Assert.notNull(application);

        // TODO GRAILS-720 this causes plugin beans to be re-created - should get getApplicationContext always call refresh?
        WebApplicationContext mainContext;
        try {
            webSpringConfig = createWebRuntimeSpringConfiguration(application, parent, application.getClassLoader());

            if (context != null) {
                webSpringConfig.setServletContext(context);
            }
            if (!pluginManager.isInitialised()) {
                pluginManager.loadPlugins();
            }

            if (!application.isInitialised()) {
                pluginManager.doArtefactConfiguration();
                application.initialise();
            }

            pluginManager.registerProvidedArtefacts(application);

            registerParentBeanFactoryPostProcessors(webSpringConfig);

            pluginManager.doRuntimeConfiguration(webSpringConfig);

            LOG.debug("[RuntimeConfiguration] Processing additional external configurations");

            if (loadExternalBeans) {
                doPostResourceConfiguration(application, webSpringConfig);
            }

            reset();

            mainContext = (WebApplicationContext)webSpringConfig.getUnrefreshedApplicationContext();
            application.setMainContext(mainContext);

            Environment.setInitializing(true);
            initializeContext(mainContext);
            Environment.setInitializing(false);

            pluginManager.setApplicationContext(mainContext);
            pluginManager.doDynamicMethods();

            mainContext.publishEvent(new GrailsContextEvent(mainContext, GrailsContextEvent.DYNAMIC_METHODS_REGISTERED));

            performPostProcessing(mainContext);

            application.refreshConstraints();
        }
        finally {
            ClassPropertyFetcher.clearClassPropertyFetcherCache();
        }

        return mainContext;
    }

    protected void initializeContext(ApplicationContext mainContext) {
        webSpringConfig.getApplicationContext();
    }

    protected WebRuntimeSpringConfiguration createWebRuntimeSpringConfiguration(GrailsApplication app,
            ApplicationContext parentCtx, ClassLoader classLoader) {
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parentCtx, classLoader);
        springConfig.setBeanFactory(new OptimizedAutowireCapableBeanFactory());
        return springConfig;
    }

    @SuppressWarnings("rawtypes")
    protected void registerParentBeanFactoryPostProcessors(WebRuntimeSpringConfiguration springConfig) {
        if (parent == null) {
            return;
        }

        Map parentPostProcessors = parent.getBeansOfType(BeanFactoryPostProcessor.class);
        for (Object o : parentPostProcessors.values()) {
            BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) o;
            ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                .addBeanFactoryPostProcessor(postProcessor);
        }
    }

    public void reconfigure(GrailsApplicationContext current, ServletContext servletContext, boolean loadExternalBeans) {
        RuntimeSpringConfiguration springConfig = parent != null ? new DefaultRuntimeSpringConfiguration(parent) : new DefaultRuntimeSpringConfiguration();
        Assert.state(pluginManager.isInitialised(),
                "Cannot re-configure Grails application when it hasn't even been configured yet!");

        pluginManager.doRuntimeConfiguration(springConfig);

        List<String> beanNames = springConfig.getBeanNames();
        for (Object beanName : beanNames) {
            String name = (String) beanName;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Re-creating bean definition [" + name + "]");
            }
            current.registerBeanDefinition(name, springConfig.createBeanDefinition(name));
            // force initialisation
            current.getBean(name);
        }
        pluginManager.doDynamicMethods();
        pluginManager.doPostProcessing(current);

        if (loadExternalBeans) {
            doPostResourceConfiguration(application, springConfig);
        }

        reset();
    }

    protected void performPostProcessing(WebApplicationContext ctx) {
        pluginManager.doPostProcessing(ctx);
    }

    public WebApplicationContext configureDomainOnly() {
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parent, application.getClassLoader());
        springConfig.setServletContext(new MockServletContext());

        if (!pluginManager.isInitialised()) {
            pluginManager.loadPlugins();
        }

        if (pluginManager.hasGrailsPlugin("hibernate")) {
            pluginManager.doRuntimeConfiguration("hibernate", springConfig);
        }
        else if (pluginManager.hasGrailsPlugin("hibernate4")) {
            pluginManager.doRuntimeConfiguration("hibernate4", springConfig);
        }

        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();

        performPostProcessing(ctx);
        application.refreshConstraints();

        return ctx;
    }

    protected void doPostResourceConfiguration(GrailsApplication app, RuntimeSpringConfiguration springConfig) {
        ClassLoader classLoader = app.getClassLoader();
        String resourceName = null;
        try {
            Resource springResources;
            if (app.isWarDeployed()) {
                resourceName = GrailsRuntimeConfigurator.SPRING_RESOURCES_XML;
                springResources = parent.getResource(resourceName);
            }
            else {
                resourceName = DEVELOPMENT_SPRING_RESOURCES_XML;
                ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
                springResources = patternResolver.getResource(resourceName);
            }

            if (springResources != null && springResources.exists()) {
                if (LOG.isDebugEnabled()) LOG.debug("[RuntimeConfiguration] Configuring additional beans from " + springResources.getURL());
                DefaultListableBeanFactory xmlBf = new OptimizedAutowireCapableBeanFactory();
                new XmlBeanDefinitionReader(xmlBf).loadBeanDefinitions(springResources);
                xmlBf.setBeanClassLoader(classLoader);
                String[] beanNames = xmlBf.getBeanDefinitionNames();
                if (LOG.isDebugEnabled()) LOG.debug("[RuntimeConfiguration] Found [" + beanNames.length + "] beans to configure");
                for (String beanName : beanNames) {
                    BeanDefinition bd = xmlBf.getBeanDefinition(beanName);
                    final String beanClassName = bd.getBeanClassName();
                    Class<?> beanClass = beanClassName == null ? null : ClassUtils.forName(beanClassName, classLoader);

                    springConfig.addBeanDefinition(beanName, bd);
                    String[] aliases = xmlBf.getAliases(beanName);
                    for (String alias : aliases) {
                        springConfig.addAlias(alias, beanName);
                    }
                    if (beanClass != null) {
                        if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass)) {
                            ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                                .addBeanFactoryPostProcessor((BeanFactoryPostProcessor) xmlBf.getBean(beanName));
                        }
                    }
                }
            }
            else if (LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] " + resourceName + " not found. Skipping configuration.");
            }
        }
        catch (Exception ex) {
            LOG.error("[RuntimeConfiguration] Unable to perform post initialization config: " + resourceName, ex);
        }

        GrailsRuntimeConfigurator.loadSpringGroovyResources(springConfig, app);
    }



    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object.
     * @param config The config instance
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, final GrailsApplication application) {
        RuntimeSpringConfigUtilities.loadExternalSpringConfig(config, application);
    }

    public static BeanBuilder reloadSpringResourcesConfig(RuntimeSpringConfiguration config, GrailsApplication application, Class<?> groovySpringResourcesClass) throws InstantiationException, IllegalAccessException {
        return RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(config, application, groovySpringResourcesClass);
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, GrailsApplication application) {
        RuntimeSpringConfigUtilities.loadExternalSpringConfig(config, application);
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, GrailsApplication application,
            GenericApplicationContext context) {
        RuntimeSpringConfigUtilities.loadSpringGroovyResourcesIntoContext(config, application, context);
    }

    public void setLoadExternalPersistenceConfig(boolean b) {
        // do nothing
    }

    public void setPluginManager(GrailsPluginManager manager) {
        pluginManager = manager;
    }

    public GrailsPluginManager getPluginManager() {
        return pluginManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        parent = applicationContext;
    }

    /**
     * Resets the GrailsRumtimeConfigurator.
     */
    public static void reset() {
        RuntimeSpringConfigUtilities.reset();
    }

    // for testing
    WebRuntimeSpringConfiguration getWebRuntimeSpringConfiguration() {
        return webSpringConfig;
    }
}
