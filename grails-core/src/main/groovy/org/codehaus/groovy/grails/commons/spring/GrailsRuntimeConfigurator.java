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
import grails.util.GrailsUtil;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanFactory;
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

    private GrailsApplication application;
    private ApplicationContext parent;
    private GrailsPluginManager pluginManager;
    private static final String DEVELOPMENT_SPRING_RESOURCES_XML = "file:./grails-app/conf/spring/resources.xml";

    public GrailsRuntimeConfigurator(GrailsApplication application) {
        this(application, null);
    }

    public GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        this.application = application;
        this.parent = parent;
        if (parent != null) {
            parent.containsBean(DATA_SOURCE_BEAN);
        }

        try {
            pluginManager = parent != null ? parent.getBean(GrailsPluginManager.class) : null;
            pluginManager = pluginManager != null ? pluginManager : PluginManagerHolder.getPluginManager();
        } catch (BeansException e) {
            // ignore
        }
        if (pluginManager == null) {
            pluginManager = PluginManagerHolder.getPluginManager();
        }
        if (pluginManager == null) {
            pluginManager = new DefaultGrailsPluginManager("**/plugins/*/**GrailsPlugin.groovy", application);
        }
        PluginManagerHolder.setPluginManager(pluginManager);
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
        WebApplicationContext ctx;
        try {
            WebRuntimeSpringConfiguration springConfig = createWebRuntimeSpringConfiguration(application, parent, application.getClassLoader());
            springConfig.setBeanFactory(new ReloadAwareAutowireCapableBeanFactory());

            if (context != null) {
                springConfig.setServletContext(context);
                pluginManager.setServletContext(context);
            }
            if (!pluginManager.isInitialised()) {
                pluginManager.loadPlugins();
            }

            if (!application.isInitialised()) {
                pluginManager.doArtefactConfiguration();
                application.initialise();
            }

            pluginManager.registerProvidedArtefacts(application);

            registerParentBeanFactoryPostProcessors(springConfig);

            pluginManager.doRuntimeConfiguration(springConfig);

            // configure scaffolding
            LOG.debug("[RuntimeConfiguration] Processing additional external configurations");

            if (loadExternalBeans) {
                doPostResourceConfiguration(application,springConfig);
            }

            reset();

            application.setMainContext(springConfig.getUnrefreshedApplicationContext());
            ctx = (WebApplicationContext) springConfig.getApplicationContext();

            pluginManager.setApplicationContext(ctx);
            pluginManager.doDynamicMethods();

            ctx.publishEvent(new GrailsContextEvent(ctx, GrailsContextEvent.DYNAMIC_METHODS_REGISTERED));

            performPostProcessing(ctx);

            application.refreshConstraints();
        }
        finally {
            ClassPropertyFetcher.clearClassPropertyFetcherCache();
        }

        return ctx;
    }

    protected WebRuntimeSpringConfiguration createWebRuntimeSpringConfiguration(
            @SuppressWarnings("unused") GrailsApplication app,
            ApplicationContext parentCtx, ClassLoader classLoader) {
        return new WebRuntimeSpringConfiguration(parentCtx, classLoader);
    }

    @SuppressWarnings("rawtypes")
    private void registerParentBeanFactoryPostProcessors(WebRuntimeSpringConfiguration springConfig) {
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

    public void reconfigure(GrailsApplicationContext current,
            @SuppressWarnings("unused") ServletContext servletContext, boolean loadExternalBeans) {
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

    private void performPostProcessing(WebApplicationContext ctx) {
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

        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();

        performPostProcessing(ctx);
        application.refreshConstraints();

        return ctx;
    }

    private void doPostResourceConfiguration(GrailsApplication app, RuntimeSpringConfiguration springConfig) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Resource springResources;
            if (app.isWarDeployed()) {
                springResources = parent.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML);
            }
            else {
                ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
                springResources = patternResolver.getResource(DEVELOPMENT_SPRING_RESOURCES_XML);
            }

            if (springResources != null && springResources.exists()) {
                LOG.debug("[RuntimeConfiguration] Configuring additional beans from " + springResources.getURL());
                XmlBeanFactory xmlBf = new XmlBeanFactory(springResources);
                xmlBf.setBeanClassLoader(classLoader);
                String[] beanNames = xmlBf.getBeanDefinitionNames();
                LOG.debug("[RuntimeConfiguration] Found [" + beanNames.length + "] beans to configure");
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
                LOG.debug("[RuntimeConfiguration] " + GrailsRuntimeConfigurator.SPRING_RESOURCES_XML + " not found. Skipping configuration.");
            }

            GrailsRuntimeConfigurator.loadSpringGroovyResources(springConfig, app);
        }
        catch (Exception ex) {
            LOG.warn("[RuntimeConfiguration] Unable to perform post initialization config: " + SPRING_RESOURCES_XML, ex);
        }
    }

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "resources.groovy".
     *
     * @param config
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config, GrailsApplication application,
            GenericApplicationContext context) {

        loadExternalSpringConfig(config, application);
        if (context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }
    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object.
     * @param config The config instance
     */
    @SuppressWarnings({ "serial", "rawtypes", "unchecked" })
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, final GrailsApplication application) {
        if (springGroovyResourcesBeanBuilder == null) {
            try {
                Class<?> groovySpringResourcesClass = null;
                try {
                    groovySpringResourcesClass = ClassUtils.forName(GrailsRuntimeConfigurator.SPRING_RESOURCES_CLASS,
                            application.getClassLoader());
                }
                catch (ClassNotFoundException e) {
                    // ignore
                }
                if (groovySpringResourcesClass != null) {
                    springGroovyResourcesBeanBuilder = new BeanBuilder(null, config,Thread.currentThread().getContextClassLoader());
                    springGroovyResourcesBeanBuilder.setBinding(new Binding(new HashMap() {{ 
                        put("application", application); 
                        put("grailsApplication", application); // GRAILS-7550
                    }}));
                    Script script = (Script) groovySpringResourcesClass.newInstance();
                    script.run();
                    Object beans = script.getProperty("beans");
                    springGroovyResourcesBeanBuilder.beans((Closure) beans);
                }
            }
            catch (Exception ex) {
                LOG.error("[RuntimeConfiguration] Unable to load beans from resources.groovy", ex);
            }
        }
        else {
            if (!springGroovyResourcesBeanBuilder.getSpringConfig().equals(config)) {
                springGroovyResourcesBeanBuilder.registerBeans(config);
            }
        }
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, GrailsApplication application) {
        loadExternalSpringConfig(config, application);
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, GrailsApplication application,
            GenericApplicationContext context) {
        loadExternalSpringConfig(config, application);
        doLoadSpringGroovyResources(config, application, context);
    }

    public void setLoadExternalPersistenceConfig(@SuppressWarnings("unused") boolean b) {
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
    public void reset() {
        springGroovyResourcesBeanBuilder = null;
    }
}
