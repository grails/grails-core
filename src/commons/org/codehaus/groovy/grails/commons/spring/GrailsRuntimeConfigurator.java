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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsServiceClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.orm.hibernate.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.scaffolding.ScaffoldDomain;
import org.hibernate.SessionFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;

/**
 * A class that handles the runtime configuration of the Grails ApplicationContext
 * 
 * @author Graeme Rocher
 * @since 0.3
 */
public class GrailsRuntimeConfigurator {

	public static final String GRAILS_URL_MAPPINGS = "grailsUrlMappings";
	public static final String SPRING_RESOURCES_XML = "/WEB-INF/spring/resources.xml";
	public static final String SPRING_RESOURCES_GROOVY = "/WEB-INF/spring/resources.groovy";
	public static final String QUARTZ_SCHEDULER_BEAN = "quartzScheduler";
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
	private boolean loadExternalPersistenceConfig = true;
    private boolean parentDataSource = false;
	private GrailsPluginManager pluginManager;

    public GrailsRuntimeConfigurator(GrailsApplication application) {
        this(application, null);
    }
	
	public GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
		super();
		this.application = application;
		this.parent = parent;
		if(parent != null)
			this.parentDataSource = parent.containsBean(DATA_SOURCE_BEAN);
        try {
			this.pluginManager = PluginManagerHolder.getPluginManager();
			if(this.pluginManager  == null) {
				this.pluginManager = new DefaultGrailsPluginManager("**/plugins/*/**GrailsPlugin.groovy", application);
			}
			else {
				LOG.debug("Retrieved thread-bound PluginManager instance");
				this.pluginManager.setApplication(application);
			}
				
				
		} catch (IOException e) {
			LOG.warn("I/O error loading plugin manager!:"+e.getMessage(), e);
		}
    }

	/**
	 * Registers a new service with the specified application context
	 * 
	 * @param grailsServiceClass The service class to register
	 * @param context The app context to register with
	 */
	public void registerService(GrailsServiceClass grailsServiceClass, GrailsWebApplicationContext context) {
		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		
		BeanConfiguration serviceClassBean = springConfig
												.createSingletonBean(MethodInvokingFactoryBean.class)
												.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
												.addProperty("targetMethod", "getGrailsServiceClass")
												.addProperty("arguments", grailsServiceClass.getFullName());
		context.registerBeanDefinition(grailsServiceClass.getFullName() + "Class",serviceClassBean.getBeanDefinition());
		
		
		BeanConfiguration serviceInstance = springConfig
												.createSingletonBean(grailsServiceClass.getFullName() + "Instance")
												.setFactoryBean( grailsServiceClass.getFullName() + "Class")
												.setFactoryMethod("newInstance");
				
		if (grailsServiceClass.byName()) {
			serviceInstance.setAutowire(BeanConfiguration.AUTOWIRE_BY_NAME);
		} else if (grailsServiceClass.byType()) {
			serviceInstance.setAutowire(BeanConfiguration.AUTOWIRE_BY_TYPE);
		}
        else {
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
			context.registerBeanDefinition(grailsServiceClass.getPropertyName(),transactionalProxyBean.getBeanDefinition());
			
		} else {
			context.registerBeanDefinition(grailsServiceClass.getPropertyName(),serviceInstance.getBeanDefinition());
		}		
	}
	
	/**
	 * Registers a tag library with the specified grails application context
	 * 
	 * @param tagLibClass That tag library class
	 * @param context The application context
	 */
	public static void registerTagLibrary(GrailsTagLibClass tagLibClass, GrailsWebApplicationContext context) {
    	RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
    	BeanConfiguration tagLibClassBean = springConfig.createSingletonBean(MethodInvokingFactoryBean.class);
    	tagLibClassBean
        	.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
        	.addProperty("targetMethod", "getGrailsTagLibClass")
        	.addProperty("arguments", tagLibClass.getFullName());
    	context.registerBeanDefinition(tagLibClass.getFullName() + "Class", tagLibClassBean.getBeanDefinition());
    	
        // configure taglib class as hot swappable target source
        Collection args = new ManagedList();
        args.add(new RuntimeBeanReference(tagLibClass.getFullName() + "Class"));
        
        BeanConfiguration tagLibTargetSourceBean = springConfig
											    	.createSingletonBean(	HotSwappableTargetSource.class, 
											    							args);

        context.registerBeanDefinition(tagLibClass.getFullName() + "TargetSource",tagLibTargetSourceBean.getBeanDefinition());
        
	    // setup AOP proxy that uses hot swappable target source            
	    BeanConfiguration tagLibProxyBean = springConfig
										    	.createSingletonBean(ProxyFactoryBean.class)
										    	.addProperty("targetSource", new RuntimeBeanReference(tagLibClass.getFullName() + "TargetSource"))
										    	.addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsTagLibClass");
	    context.registerBeanDefinition(tagLibClass.getFullName() + "Proxy",tagLibProxyBean.getBeanDefinition());

	    // create prototype bean that refers to the AOP proxied taglib class uses it as a factory
	    BeanConfiguration tagLibBean = springConfig
									    	.createPrototypeBean(tagLibClass.getFullName())
									    	.setFactoryBean(tagLibClass.getFullName() + "Proxy")
									    	.setFactoryMethod("newInstance")
									    	.setAutowire("byName");
	    
	    context.registerBeanDefinition(tagLibClass.getFullName(),tagLibBean.getBeanDefinition());
		
	}
	
	/**
	 * Registers a new domain class with the application context
	 * 
	 * @param grailsDomainClass The domain class
	 * @param context The application context
	 */
	public void registerDomainClass(GrailsDomainClass grailsDomainClass, GrailsWebApplicationContext context) {
		
		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		
		BeanConfiguration domainClassBean = springConfig
			.createSingletonBean(MethodInvokingFactoryBean.class)
			.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID, true))
			.addProperty("targetMethod","getGrailsDomainClass")
			.addProperty("arguments", grailsDomainClass.getFullName());
		
		context.registerBeanDefinition(grailsDomainClass.getFullName() + "DomainClass", domainClassBean.getBeanDefinition());

	
        // configure domain class as hot swappable target source
        Collection args = new ManagedList();
        args.add(new RuntimeBeanReference(grailsDomainClass.getFullName() + "DomainClass"));
        BeanConfiguration targetSourceBean = springConfig
        										.createSingletonBean(HotSwappableTargetSource.class, args);
        
        context.registerBeanDefinition(grailsDomainClass.getFullName() + "TargetSource",targetSourceBean.getBeanDefinition());
        

        // setup AOP proxy that uses hot swappable target source
        BeanConfiguration proxyBean = springConfig
        								.createSingletonBean(ProxyFactoryBean.class)
        								.addProperty("targetSource", new RuntimeBeanReference(grailsDomainClass.getFullName() + "TargetSource"))
        								.addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsDomainClass");
        
        context.registerBeanDefinition(grailsDomainClass.getFullName() + "Proxy", proxyBean.getBeanDefinition());
               	
		// create persistent class bean references
		BeanConfiguration persistentClassBean  = springConfig
													.createSingletonBean(MethodInvokingFactoryBean.class)
													.addProperty("targetObject", new RuntimeBeanReference( grailsDomainClass.getFullName() + "Proxy") )
													.addProperty("targetMethod", "getClazz");
	
		context.registerBeanDefinition(grailsDomainClass.getFullName() + "PersistentClass",persistentClassBean.getBeanDefinition());
			
		// configure validator			
		BeanConfiguration validatorBean = springConfig
											.createSingletonBean(GrailsDomainClassValidator.class)
											.addProperty( "domainClass" ,new RuntimeBeanReference(grailsDomainClass.getFullName() + "Proxy") )
											.addProperty(SESSION_FACTORY_BEAN , new RuntimeBeanReference(SESSION_FACTORY_BEAN))
											.addProperty(MESSAGE_SOURCE_BEAN , new RuntimeBeanReference(MESSAGE_SOURCE_BEAN));
		
		context.registerBeanDefinition(grailsDomainClass.getFullName() + "Validator",validatorBean.getBeanDefinition());
	}
	
	/**
	 * Updates an existing domain class within the application context
	 * 
	 * @param domainClass The domain class to update
	 * @param context The context
	 */
	public void updateDomainClass(GrailsDomainClass domainClass, GrailsWebApplicationContext context) {		
		HotSwappableTargetSource ts = (HotSwappableTargetSource)context.getBean(domainClass.getFullName() + "TargetSource");
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
	 * @return An ApplicationContext instance
	 */
	public WebApplicationContext configure(ServletContext context) {		
		RuntimeSpringConfiguration springConfig = parent != null ? new DefaultRuntimeSpringConfiguration(parent) : new DefaultRuntimeSpringConfiguration();
		if(context != null)
			springConfig.setServletContext(context);
		
		if(!this.pluginManager.isInitialised())
			this.pluginManager.loadPlugins();

        Assert.notNull(application);
        
        this.pluginManager.doRuntimeConfiguration(springConfig);
        
		// configure scaffolding
		LOG.debug("[RuntimeConfiguration] Proccessing additional external configurations");
		doPostResourceConfiguration(springConfig);
	
		WebApplicationContext ctx = springConfig.getApplicationContext();
		
		this.pluginManager.setApplicationContext(ctx);
		
		performPostProcessing(ctx);
		
		this.pluginManager.doDynamicMethods();
		
		return ctx;
	}
	
	private void performPostProcessing(WebApplicationContext ctx) {
		this.pluginManager.doPostProcessing(ctx);
	}

	public WebApplicationContext configureDomainOnly() {
		RuntimeSpringConfiguration springConfig = parent != null ? new DefaultRuntimeSpringConfiguration(parent) : new DefaultRuntimeSpringConfiguration();
		springConfig.setServletContext(new MockServletContext());

		if(!this.pluginManager.isInitialised())
			this.pluginManager.loadPlugins();

		
		if(pluginManager.hasGrailsPlugin("hibernate"))
			pluginManager.doRuntimeConfiguration("hibernate", springConfig);
		
		WebApplicationContext ctx = springConfig.getApplicationContext();

		performPostProcessing(ctx);		
		return ctx;		
	}
	private void doPostResourceConfiguration(RuntimeSpringConfiguration springConfig) {
	     try {
	    	 Resource springResources = parent.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML);
	    	 Resource groovySpringResources = parent.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY);
	    	 if(springResources.exists()) {
	    		LOG.debug("[RuntimeConfiguration] Configuring additional beans from " +springResources.getURL());
	    		XmlBeanFactory xmlBf = new XmlBeanFactory(springResources);
				String[] beanNames = xmlBf.getBeanDefinitionNames();
				LOG.debug("[RuntimeConfiguration] Found ["+beanNames.length+"] beans to configure");				
				for (int k = 0; k < beanNames.length; k++) {
					BeanDefinition bd = xmlBf.getBeanDefinition(beanNames[k]);
					
					springConfig.addBeanDefinition(beanNames[k], bd);
				}
	    		 
	    	 }
	    	 else if(LOG.isDebugEnabled()) {
	    		 LOG.debug("[RuntimeConfiguration] " + GrailsRuntimeConfigurator.SPRING_RESOURCES_XML + " not found. Skipping configuration.");
	    	 }
	    	 
	    	 if(groovySpringResources.exists()) {
	    		 BeanBuilder bb = new BeanBuilder();
	    		 bb.setSpringConfig(springConfig);
	    		 bb.loadBeans(groovySpringResources);
	    	 }
		} catch (Exception ex) {
			LOG.warn("[RuntimeConfiguration] Unable to perform post initialization config: " + SPRING_RESOURCES_XML , ex);
		}
	}
	public void refreshSessionFactory(GrailsApplication app, GrailsWebApplicationContext context) {

		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		BeanConfiguration sessionFactoryBean = springConfig
												.createSingletonBean(ConfigurableLocalSessionFactoryBean.class)
												.addProperty("grailsApplication",new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
												.addProperty("classLoader", new RuntimeBeanReference(CLASS_LOADER_BEAN))
												.addProperty("dataSource", new RuntimeBeanReference(DATA_SOURCE_BEAN,parentDataSource))
												.addProperty("hibernateProperties", new RuntimeBeanReference(HIBERNATE_PROPERTIES_BEAN));
		
		GrailsDataSource ds = app.getGrailsDataSource();
		if(ds != null && ds.getConfigurationClass() != null) {
			sessionFactoryBean.addProperty("configClass",ds.getConfigurationClass());
		}

		if(loadExternalPersistenceConfig) {
			URL hibernateConfig = application.getClassLoader().getResource("hibernate.cfg.xml");
			if(hibernateConfig != null) {
				sessionFactoryBean
					.addProperty("configLocation",new ClassPathResource("hibernate.cfg.xml"));
			}
		}
		
		SessionFactory sf = (SessionFactory)context.getBean(SESSION_FACTORY_BEAN);
		context.registerBeanDefinition(SESSION_FACTORY_BEAN,sessionFactoryBean.getBeanDefinition());
		GrailsDomainConfigurationUtil.configureDynamicMethods(context,app);
		
		// update transaction manager reference
		if(context.containsBean(TRANSACTION_MANAGER_BEAN)) {
			((HibernateTransactionManager)context.getBean(TRANSACTION_MANAGER_BEAN)).setSessionFactory(sf);
		}
		// update OSIVI reference
		if(context.containsBean(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN)) {
			((OpenSessionInViewInterceptor)context.getBean(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN)).setSessionFactory(sf);
		}
		// update validators
		String[] validatorBeanNames = context.getBeanNamesForType(Validator.class);
		for (int i = 0; i < validatorBeanNames.length; i++) {
			GrailsDomainClassValidator validator = (GrailsDomainClassValidator) context.getBean(validatorBeanNames[i]);
			validator.setSessionFactory(sf);
		}
		// update scaffold domains
		Map scaffoldDomains = context.getBeansOfType(ScaffoldDomain.class);
		for (Iterator i = scaffoldDomains.values().iterator(); i.hasNext();) {
			ScaffoldDomain sd = (ScaffoldDomain) i.next();
			sd.setSessionFactory(sf);
		}
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


}
