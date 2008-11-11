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
import groovy.lang.Closure;
import groovy.lang.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.*;

/**
 * A class that handles the runtime configuration of the Grails ApplicationContext
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
    private boolean parentDataSource;
    private GrailsPluginManager pluginManager;
    private boolean loadExternalPersistenceConfig;
    private static final String DEVELOPMENT_SPRING_RESOURCES_XML = "file:./grails-app/conf/spring/resources.xml";

    public GrailsRuntimeConfigurator(GrailsApplication application) {
        this(application, null);
    }

    public GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        super();
        this.application = application;
        this.parent = parent;
        parentDataSource = false;
        if (parent != null)
            this.parentDataSource = parent.containsBean(DATA_SOURCE_BEAN);
        try {
            this.pluginManager = PluginManagerHolder.getPluginManager();
            if (this.pluginManager == null) {
                this.pluginManager = new DefaultGrailsPluginManager("**/plugins/*/**GrailsPlugin.groovy", application);
                PluginManagerHolder.setPluginManager(this.pluginManager);
            } else {
                LOG.debug("Retrieved thread-bound PluginManager instance");
                this.pluginManager.setApplication(application);
            }


        } catch (IOException e) {
            throw new GrailsConfigurationException("I/O error loading plugin manager!:" + e.getMessage(), e);
        }
    }

    /**
     * Registers a new service with the specified application context
     *
     * @param grailsServiceClass The service class to register
     * @param context            The app context to register with
     */
    public void registerService(GrailsServiceClass grailsServiceClass, GrailsApplicationContext context) {
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();

        BeanConfiguration serviceClassBean = springConfig
                .createSingletonBean(MethodInvokingFactoryBean.class)
                .addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID, true))
                .addProperty("targetMethod", "getArtefact")
                .addProperty("arguments", new Object[]{
                        ServiceArtefactHandler.TYPE,
                        grailsServiceClass.getFullName()});
        context.registerBeanDefinition(grailsServiceClass.getFullName() + "Class", serviceClassBean.getBeanDefinition());


        BeanConfiguration serviceInstance = springConfig
                .createSingletonBean(grailsServiceClass.getFullName() + "Instance")
                .setFactoryBean(grailsServiceClass.getFullName() + "Class")
                .setFactoryMethod("newInstance");

        if (grailsServiceClass.byName()) {
            serviceInstance.setAutowire(BeanConfiguration.AUTOWIRE_BY_NAME);
        } else if (grailsServiceClass.byType()) {
            serviceInstance.setAutowire(BeanConfiguration.AUTOWIRE_BY_TYPE);
        } else {
            serviceInstance.setAutowire(BeanConfiguration.AUTOWIRE_BY_TYPE);
        }

        //context.registerBeanDefinition(grailsServiceClass.getFullName() + "Instance",serviceInstance.getBeanDefinition());

        // configure the service instance as a hotswappable target source

        // if its transactional configure transactional proxy
        if (grailsServiceClass.isTransactional()) {
            Properties transactionAttributes = new Properties();
            transactionAttributes.put("*", "PROPAGATION_REQUIRED");

            BeanConfiguration transactionalProxyBean = springConfig
                    .createSingletonBean(TransactionProxyFactoryBean.class)
                    .addProperty("target", serviceInstance.getBeanDefinition())
                    .addProperty("proxyTargetClass", Boolean.TRUE)
                    .addProperty("transactionAttributes", transactionAttributes)
                    .addProperty(TRANSACTION_MANAGER_BEAN, new RuntimeBeanReference(TRANSACTION_MANAGER_BEAN));
            context.registerBeanDefinition(grailsServiceClass.getPropertyName(), transactionalProxyBean.getBeanDefinition());

        } else {
            context.registerBeanDefinition(grailsServiceClass.getPropertyName(), serviceInstance.getBeanDefinition());
        }
    }

    /**
     * Registers a tag library with the specified grails application context
     *
     * @param tagLibClass That tag library class
     * @param context     The application context
     */
    public static void registerTagLibrary(GrailsTagLibClass tagLibClass, GrailsApplicationContext context) {
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
        BeanConfiguration tagLibClassBean = springConfig.createSingletonBean(MethodInvokingFactoryBean.class);
        tagLibClassBean
                .addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID, true))
                .addProperty("targetMethod", "getArtefact")
                .addProperty("arguments", new Object[]{TagLibArtefactHandler.TYPE, tagLibClass.getFullName()});
        context.registerBeanDefinition(tagLibClass.getFullName() + "Class", tagLibClassBean.getBeanDefinition());

        // configure taglib class as hot swappable target source
        Collection args = new ManagedList();
        args.add(new RuntimeBeanReference(tagLibClass.getFullName() + "Class"));

        BeanConfiguration tagLibTargetSourceBean = springConfig
                .createSingletonBean(HotSwappableTargetSource.class,
                        args);

        context.registerBeanDefinition(tagLibClass.getFullName() + "TargetSource", tagLibTargetSourceBean.getBeanDefinition());

        // setup AOP proxy that uses hot swappable target source
        BeanConfiguration tagLibProxyBean = springConfig
                .createSingletonBean(ProxyFactoryBean.class)
                .addProperty("targetSource", new RuntimeBeanReference(tagLibClass.getFullName() + "TargetSource"))
                .addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsTagLibClass");
        context.registerBeanDefinition(tagLibClass.getFullName() + "Proxy", tagLibProxyBean.getBeanDefinition());

        // create prototype bean that refers to the AOP proxied taglib class uses it as a factory
        BeanConfiguration tagLibBean = springConfig
                .createPrototypeBean(tagLibClass.getFullName())
                .setFactoryBean(tagLibClass.getFullName() + "Proxy")
                .setFactoryMethod("newInstance")
                .setAutowire("byName");

        context.registerBeanDefinition(tagLibClass.getFullName(), tagLibBean.getBeanDefinition());

    }


    /**
     * Updates an existing domain class within the application context
     *
     * @param domainClass The domain class to update
     * @param context     The context
     */
    public void updateDomainClass(GrailsDomainClass domainClass, GrailsApplicationContext context) {
        HotSwappableTargetSource ts = (HotSwappableTargetSource) context.getBean(domainClass.getFullName() + "TargetSource");
        ts.swap(domainClass);
    }

    /**
     * Configures the Grails application context at runtime
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

        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parent, application.getClassLoader());
        if (context != null) {
            springConfig.setServletContext(context);
            this.pluginManager.setServletContext(context);
        }
        if (!this.pluginManager.isInitialised()) {
            this.pluginManager.loadPlugins();
        }

        if (!application.isInitialised()) {
            pluginManager.doArtefactConfiguration();
            application.initialise();
        }

        this.pluginManager.registerProvidedArtefacts(application);

        registerParentBeanFactoryPostProcessors(springConfig);
        
        this.pluginManager.doRuntimeConfiguration(springConfig);

        // configure scaffolding
        LOG.debug("[RuntimeConfiguration] Processing additional external configurations");

        if (loadExternalBeans) {
            doPostResourceConfiguration(application,springConfig);
        }

        reset();


        // TODO GRAILS-720 this causes plugin beans to be re-created - should get getApplicationContext always call refresh?
        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();

        this.pluginManager.setApplicationContext(ctx);

        this.pluginManager.doDynamicMethods();

        ctx.publishEvent(new GrailsContextEvent(ctx, GrailsContextEvent.DYNAMIC_METHODS_REGISTERED));


        performPostProcessing(ctx);

        application.refreshConstraints();

        return ctx;
    }

    private void registerParentBeanFactoryPostProcessors(WebRuntimeSpringConfiguration springConfig) {
        if(parent != null) {
            Map parentPostProcessors = parent.getBeansOfType(BeanFactoryPostProcessor.class);
            for (Iterator i = parentPostProcessors.values().iterator(); i.hasNext();) {
                BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) i.next();
                ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                        .addBeanFactoryPostProcessor(postProcessor);

            }
        }
    }

    public void reconfigure(GrailsApplicationContext current, ServletContext servletContext, boolean loadExternalBeans) {
        RuntimeSpringConfiguration springConfig = parent != null ? new DefaultRuntimeSpringConfiguration(parent) : new DefaultRuntimeSpringConfiguration();
        if (!this.pluginManager.isInitialised())
            throw new IllegalStateException("Cannot re-configure Grails application when it hasn't even been configured yet!");

        this.pluginManager.doRuntimeConfiguration(springConfig);

        List beanNames = springConfig.getBeanNames();
        for (Iterator i = beanNames.iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Re-creating bean definition [" + name + "]");
            }
            current.registerBeanDefinition(name, springConfig.createBeanDefinition(name));
            // force initialisation
            current.getBean(name);
        }
        this.pluginManager.doDynamicMethods();

        this.pluginManager.doPostProcessing(current);

        if (loadExternalBeans)
            doPostResourceConfiguration(application, springConfig);
                
        reset();

    }

    private void performPostProcessing(WebApplicationContext ctx) {
        this.pluginManager.doPostProcessing(ctx);

    }

    public WebApplicationContext configureDomainOnly() {
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parent, application.getClassLoader());
        springConfig.setServletContext(new MockServletContext());

        if (!this.pluginManager.isInitialised())
            this.pluginManager.loadPlugins();


        if (pluginManager.hasGrailsPlugin("hibernate"))
            pluginManager.doRuntimeConfiguration("hibernate", springConfig);

        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();

        performPostProcessing(ctx);
        application.refreshConstraints();

        return ctx;
    }

    private void doPostResourceConfiguration(GrailsApplication application, RuntimeSpringConfiguration springConfig) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Resource springResources = null;
            if(application.isWarDeployed()) {
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
                for (int k = 0; k < beanNames.length; k++) {
                    BeanDefinition bd = xmlBf.getBeanDefinition(beanNames[k]);
                    final String beanClassName = bd.getBeanClassName();
                    Class beanClass = beanClassName == null ? null : ClassUtils.forName(beanClassName, classLoader);
                    if (SESSION_FACTORY_BEAN.equals(beanNames[k])) {
                        Class configurableLocalSessionFactoryBeanClass = org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean.class;
                        if (beanClass == null || !configurableLocalSessionFactoryBeanClass.isAssignableFrom(beanClass)) {
                            LOG.warn("[RuntimeConfiguration] Found custom Hibernate SessionFactory bean defined in " + springResources.getURL() +
                                    ". The bean will not be configured as Grails needs to use its own specialized Hibernate SessionFactoryBean" +
                                    " in order to inject dynamic bahavior into domain classes." +
                                    " Use specialized Hibernate SessionFactoryBean '" + configurableLocalSessionFactoryBeanClass +
                                    "' for custom Hibernate SessionFactory bean defined in '" + springResources.getURL() +
                                    "' instead of '" + beanClassName + "' in order to configure custom Hibernate SessionFactory bean.");
                            continue;
                        }
                    }

                    springConfig.addBeanDefinition(beanNames[k], bd);

                    if (beanClass != null) {
                        if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass)) {
                            ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                                    .addBeanFactoryPostProcessor((BeanFactoryPostProcessor) xmlBf.getBean(beanNames[k]));
                        }
                    }
                }


            } else if (LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] " + GrailsRuntimeConfigurator.SPRING_RESOURCES_XML + " not found. Skipping configuration.");
            }

            GrailsRuntimeConfigurator.loadSpringGroovyResources(springConfig, classLoader);

        } catch (Exception ex) {
            LOG.warn("[RuntimeConfiguration] Unable to perform post initialization config: " + SPRING_RESOURCES_XML, ex);
        }
    }

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "resources.groovy"
     *
     * @param config
     * @param classLoader
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader,

                                                    GenericApplicationContext context) {
        
        if(springGroovyResourcesBeanBuilder == null) {
            try {
                Class groovySpringResourcesClass = null;
                try {
                    groovySpringResourcesClass = ClassUtils.forName(GrailsRuntimeConfigurator.SPRING_RESOURCES_CLASS,
                        classLoader);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (groovySpringResourcesClass != null) {
                    springGroovyResourcesBeanBuilder = new BeanBuilder(Thread.currentThread().getContextClassLoader());
                    springGroovyResourcesBeanBuilder.setSpringConfig(config);
                    Script script = (Script) groovySpringResourcesClass.newInstance();
                    script.run();
                    Object beans = script.getProperty("beans");
                    springGroovyResourcesBeanBuilder.beans((Closure) beans);
                }
            } catch (Exception ex) {
                LOG.warn("[RuntimeConfiguration] Unable to perform load beans from resources.groovy", ex);
            }

        }
        else {
            RuntimeSpringConfiguration existingSpringConfig = springGroovyResourcesBeanBuilder.getSpringConfig();
            List beanNames = existingSpringConfig.getBeanNames();
            for (Iterator i = beanNames.iterator(); i.hasNext();) {
                String beanName = i.next().toString();
                BeanConfiguration beanConfig = existingSpringConfig.getBeanConfig(beanName);
                if(beanConfig!= null) {
                    config.addBeanConfiguration(beanName, beanConfig);
                }
                else {
                    BeanDefinition definition = existingSpringConfig.getBeanDefinition(beanName);
                    if(definition!=null)
                        config.addBeanDefinition(beanName, definition);
                }
            }
        }
        if (context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }

    }



    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader) {
            doLoadSpringGroovyResources(config, classLoader, null);
    }


    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, ClassLoader classLoader,
                                                            GenericApplicationContext context) {
        doLoadSpringGroovyResources(config, classLoader, context);
    }

    public void setLoadExternalPersistenceConfig(boolean b) {
        this.loadExternalPersistenceConfig = b;
    }

    public void setPluginManager(GrailsPluginManager manager) {
        this.pluginManager = manager;
    }

    public GrailsPluginManager getPluginManager() {
        return this.pluginManager;
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parent = applicationContext;
    }

    /**
     * Resets the GrailsRumtimeConfigurator
     */
    public void reset() {
        springGroovyResourcesBeanBuilder = null;
    }
}
