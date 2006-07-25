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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsServiceClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.GrailsTaskClass;
import org.codehaus.groovy.grails.commons.GrailsTaskClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsResponseHandlerFactory;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsScaffoldViewResolver;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsScaffolder;
import org.codehaus.groovy.grails.scaffolding.DefaultScaffoldRequestHandler;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffoldDomain;
import org.codehaus.groovy.grails.scaffolding.ScaffoldDomain;
import org.codehaus.groovy.grails.scaffolding.ViewDelegatingScaffoldResponseHandler;
import org.codehaus.groovy.grails.support.ClassEditor;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.hibernate.SessionFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.config.BeanReferenceFactoryBean;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerBean;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springmodules.beans.factory.config.MapToPropertiesFactoryBean;

/**
 * A class that handles the runtime configuration of the Grails ApplicationContext
 * 
 * @author Graeme
 * @since 0.3
 */
public class GrailsRuntimeConfigurator {

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

	public GrailsRuntimeConfigurator(GrailsApplication application) {
		super();
		this.application = application;
	}
	
	public GrailsRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
		super();
		this.application = application;
		this.parent = parent;
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
			serviceInstance.setAutowire("byName");
		} else if (grailsServiceClass.byType()) {
			serviceInstance.setAutowire("byType");
		}
		context.registerBeanDefinition(grailsServiceClass.getFullName() + "Instance",serviceInstance.getBeanDefinition());
		
	    // configure the service instance as a hotswappable target source
	
	    // if its transactional configure transactional proxy
	    if (grailsServiceClass.isTransactional()) {
			Properties transactionAttributes = new Properties();
			transactionAttributes.put("*", "PROPAGATION_REQUIRED");
			
			BeanConfiguration transactionalProxyBean = springConfig
								.createSingletonBean(TransactionProxyFactoryBean.class)
								.addProperty("target", new RuntimeBeanReference(grailsServiceClass.getFullName() + "Instance"))
								.addProperty("proxyTargetClass", Boolean.TRUE)
								.addProperty("transactionAttributes", transactionAttributes)
								.addProperty(TRANSACTION_MANAGER_BEAN, new RuntimeBeanReference(TRANSACTION_MANAGER_BEAN));
			context.registerBeanDefinition(grailsServiceClass.getPropertyName(),transactionalProxyBean.getBeanDefinition());
			
		} else {
	        // otherwise configure a standard proxy
			BeanConfiguration instanceRef = springConfig
												.createSingletonBean(BeanReferenceFactoryBean.class)
												.addProperty("targetBeanName",grailsServiceClass.getFullName() + "Instance" );
			
			context.registerBeanDefinition(grailsServiceClass.getName() + "Service",instanceRef.getBeanDefinition());
		}		
	}
	
	/**
	 * Registers a tag library with the specified grails application context
	 * 
	 * @param tagLibClass That tag library class
	 * @param context The application context
	 */
	public void registerTagLibrary(GrailsTagLibClass tagLibClass, GrailsWebApplicationContext context) {
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
		
        Assert.notNull(application);
        
        // put reference to Grails class loader instance on spring ctx
        springConfig
        	.addSingletonBean(CLASS_LOADER_BEAN, MethodInvokingFactoryBean.class)
        	.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID, true))
        	.addProperty("targetMethod", "getClassLoader");
        
        // add class editor, this allows automatic type conversion from Strings to java.lang.Class instances
        springConfig
        	.addSingletonBean(CLASS_EDITOR_BEAN, ClassEditor.class)
        	.addProperty(CLASS_LOADER_BEAN, new RuntimeBeanReference(CLASS_LOADER_BEAN));
        
        BeanConfiguration propertyEditors = springConfig
        										.addSingletonBean(CUSTOM_EDITORS_BEAN, CustomEditorConfigurer.class);
		Map customEditors = new ManagedMap();
		customEditors.put(java.lang.Class.class, new RuntimeBeanReference(CLASS_EDITOR_BEAN));		
		propertyEditors.addProperty(CUSTOM_EDITORS_BEAN, customEditors);
		
		// configure exception handler which allows Grails to display the errors view
		springConfig
			.addSingletonBean(EXCEPTION_HANDLER_BEAN, GrailsExceptionResolver.class)
			.addProperty("exceptionMappings","java.lang.Exception=/error");
		
		// configure multipart resolver used to handle file uploads in Spring MVC
		springConfig
			.addSingletonBean(MULTIPART_RESOLVER_BEAN,CommonsMultipartResolver.class);
        
		LOG.info("[SpringConfig] Configuring i18n support");
		populateI18nSupport(springConfig);
		
		LOG.info("[SpringConfig] Configuring Grails data source");
		populateDataSourceReferences(springConfig);	
		
		// configure domain classes
		LOG.info("[SpringConfig] Configuring Grails domain");
		populateDomainClassReferences(springConfig);		
		
		// configure services
		LOG.info("[SpringConfig] Configuring Grails services");
		populateServiceClassReferences(springConfig);
		
		// configure grails controllers
		LOG.info("[SpringConfig] Configuring Grails controllers");
		populateControllerReferences(springConfig);
		
		// configure scaffolding
		LOG.info("[SpringConfig] Configuring Grails scaffolding");
		populateScaffoldingReferences(springConfig);		
	
		// configure tasks
		LOG.info("[SpringConfig] Configuring Grails scheduled jobs");
		populateJobReferences(springConfig);
		
		return springConfig.getApplicationContext();		
	}

	private void populateJobReferences(RuntimeSpringConfiguration springConfig) {
		GrailsTaskClass[] grailsTaskClasses = application.getGrailsTasksClasses();
		
        Collection schedulerReferences = new ManagedList();
		
        // todo jdbc job loading
        
		for (int i = 0; i < grailsTaskClasses.length; i++) {
			GrailsTaskClass grailsTaskClass = grailsTaskClasses[i];
			
			springConfig
				.addSingletonBean(grailsTaskClass.getFullName() + "Class",MethodInvokingFactoryBean.class)
				.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
				.addProperty("targetMethod", "getGrailsTaskClass")
				.addProperty("arguments", grailsTaskClass.getFullName());					

			/* additional indirrection so that autowireing would be possible */
			springConfig
					.addSingletonBean(grailsTaskClass.getFullName())
					.setFactoryBean(grailsTaskClass.getFullName() + "Class")
					.setFactoryMethod("newInstance")
					.setAutowire("byName");
			
			
			
			springConfig
					.addSingletonBean( grailsTaskClass.getFullName()+"JobDetail",MethodInvokingJobDetailFactoryBean.class )
					.addProperty("targetObject", new RuntimeBeanReference(grailsTaskClass.getFullName()) )
					.addProperty("targetMethod", GrailsTaskClassProperty.EXECUTE)
					.addProperty("group", grailsTaskClass.getGroup());			
			
			
			if( !grailsTaskClass.isCronExpressionConfigured() ){		/* configuring Task using startDelay and timeOut */
			

				springConfig
					.addSingletonBean(grailsTaskClass.getFullName()+"SimpleTrigger",SimpleTriggerBean.class)
					.addProperty("jobDetail", new RuntimeBeanReference(grailsTaskClass.getFullName()+"JobDetail"))
					.addProperty("startDelay", grailsTaskClass.getStartDelay() )
					.addProperty("repeatInterval", grailsTaskClass.getTimeout() );				
				
				
				schedulerReferences.add(new RuntimeBeanReference(grailsTaskClass.getFullName()+"SimpleTrigger"));
			
			}else{	/* configuring Task using cronExpression */
				

				springConfig
					.addSingletonBean(grailsTaskClass.getFullName()+"CronTrigger",CronTriggerBean.class)
					.addProperty("jobDetail", new RuntimeBeanReference(grailsTaskClass.getFullName()+"JobDetail"))
					.addProperty("cronExpression", grailsTaskClass.getCronExpression());
								
				schedulerReferences.add(new RuntimeBeanReference(grailsTaskClass.getFullName()+"CronTrigger"));				
				
			}
		
		}
		

		springConfig
			.addSingletonBean("grailsSchedulerBean",SchedulerFactoryBean.class)
			.addProperty("triggers", schedulerReferences);		
	}

	private void populateScaffoldingReferences(RuntimeSpringConfiguration springConfig) {
		// go through all the controllers
		GrailsControllerClass[] simpleControllers = application.getControllers();
		for (int i = 0; i < simpleControllers.length; i++) {
            // retrieve appropriate domain class
            Class scaffoldedClass = simpleControllers[i].getScaffoldedClass();
            GrailsDomainClass domainClass;
            if(scaffoldedClass == null) {
                domainClass = application.getGrailsDomainClass(simpleControllers[i].getName());
                if(domainClass != null) {
                    scaffoldedClass = domainClass.getClazz();
                }
            }

            if(scaffoldedClass == null) {
                LOG.info("[Spring] Scaffolding disabled for controller ["+simpleControllers[i].getFullName()+"], no equivalent domain class named ["+simpleControllers[i].getName()+"]");
            }
            else {
                BeanConfiguration scaffolder = 
                	springConfig.addSingletonBean(simpleControllers[i].getFullName() + "Scaffolder",DefaultGrailsScaffolder.class);

                // create scaffold domain
                Collection constructorArguments = new ManagedList();
                constructorArguments.add(scaffoldedClass.getName());

                constructorArguments.add(new RuntimeBeanReference(SESSION_FACTORY_BEAN));

               springConfig
               		.addSingletonBean(	scaffoldedClass.getName() + "ScaffoldDomain",
                						GrailsScaffoldDomain.class, 
                						constructorArguments );


                // create and configure request handler
                BeanConfiguration requestHandler = 
                	springConfig
                		.createSingletonBean(DefaultScaffoldRequestHandler.class)
                		.addProperty("scaffoldDomain", new RuntimeBeanReference(scaffoldedClass.getName() + "ScaffoldDomain"));

                // create response factory
                constructorArguments = new ArrayList();
                constructorArguments.add(new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true));

                // configure default response handler
                BeanConfiguration defaultResponseHandler = 
                		springConfig
                			.createSingletonBean(ViewDelegatingScaffoldResponseHandler.class);

                // configure a simple view delegating resolver
                BeanConfiguration defaultViewResolver = 
                		springConfig
                			.createSingletonBean(DefaultGrailsScaffoldViewResolver.class,constructorArguments);
                			
                defaultResponseHandler.addProperty("scaffoldViewResolver", defaultViewResolver.getBeanDefinition());

                // create constructor arguments response handler factory
                constructorArguments = new ArrayList();
                constructorArguments.add(new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true));
                constructorArguments.add(defaultResponseHandler.getBeanDefinition());

                BeanConfiguration responseHandlerFactory = 
                	springConfig.createSingletonBean( DefaultGrailsResponseHandlerFactory.class,constructorArguments );

                scaffolder.addProperty( "scaffoldResponseHandlerFactory", responseHandlerFactory.getBeanDefinition() );
                scaffolder.addProperty("scaffoldRequestHandler", requestHandler.getBeanDefinition());
            }
		}
	}

	private void populateControllerReferences(RuntimeSpringConfiguration springConfig) {

        Properties urlMappings = new Properties();
        
		// Create the Grails Spring MVC controller bean that delegates to Grails controllers
		springConfig
			.addSingletonBean(SimpleGrailsController.APPLICATION_CONTEXT_ID,SimpleGrailsController.class)
			.setAutowire("byType");
		
		// Configure the view resolver which resolves JSP or GSP views
        springConfig
        	.addSingletonBean("jspViewResolver",GrailsViewResolver.class)
        	.addProperty("viewClass",org.springframework.web.servlet.view.JstlView.class)
        	.addProperty("prefix",GrailsApplicationAttributes.PATH_TO_VIEWS)
        	.addProperty("suffix",".jsp");
	
		BeanConfiguration simpleUrlHandlerMapping = null;
		if (application.getControllers().length > 0 || application.getPageFlows().length > 0) {
			simpleUrlHandlerMapping = 
				springConfig.addSingletonBean("grailsUrlHandlerMapping",GrailsUrlHandlerMapping.class);

            springConfig
		    	.addSingletonBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_ID,ProxyFactoryBean.class)
		    	.addProperty("targetSource", new RuntimeBeanReference(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE))
		    	.addProperty("proxyInterfaces", "org.springframework.web.servlet.HandlerMapping");

            
            Collection args = new ManagedList();
            args.add(new RuntimeBeanReference("grailsUrlHandlerMapping"));
            springConfig
            	.addSingletonBean(	GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE,
            						HotSwappableTargetSource.class,
            						args);
            
            // configure handler interceptors
            
            // this configures the open session in view interceptor (OSIVI)
            springConfig
            	.addSingletonBean(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN,OpenSessionInViewInterceptor.class)
            	.addProperty("flushMode",new Integer(HibernateAccessor.FLUSH_AUTO))
            	.addProperty(SESSION_FACTORY_BEAN, new RuntimeBeanReference(SESSION_FACTORY_BEAN));



            Collection interceptors = new ManagedList();
            interceptors.add(new RuntimeBeanReference(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN));
            simpleUrlHandlerMapping.addProperty("interceptors", interceptors);                        
        }        
		
		GrailsControllerClass[] simpleControllers = application.getControllers();
		for (int i = 0; i < simpleControllers.length; i++) {
			GrailsControllerClass simpleController = simpleControllers[i];
			if (!simpleController.getAvailable()) {
				continue;
			}
            // setup controller class by retrieving it from the  grails application
            springConfig
            	.addSingletonBean(simpleController.getFullName() + "Class",MethodInvokingFactoryBean.class)
            	.addProperty("targetObject", new RuntimeBeanReference("grailsApplication",true))
            	.addProperty("targetMethod", "getController")
            	.addProperty("arguments", simpleController.getFullName());
			
            // configure controller class as hot swappable target source
            Collection args = new ManagedList();
            args.add(new RuntimeBeanReference(simpleController.getFullName() + "Class"));
            springConfig
            	.addSingletonBean(simpleController.getFullName() + "TargetSource",HotSwappableTargetSource.class, args);
            

            // setup AOP proxy that uses hot swappable target source
            springConfig
            	.addSingletonBean(simpleController.getFullName() + "Proxy",ProxyFactoryBean.class)
            	.addProperty("targetSource", new RuntimeBeanReference(simpleController.getFullName() + "TargetSource"))
            	.addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsControllerClass");
            

            // create prototype bean that uses the controller AOP proxy controller class bean as a factory
            springConfig
            	.addPrototypeBean(simpleController.getFullName())
            	.setFactoryBean(simpleController.getFullName() + "Proxy")
            	.setFactoryMethod("newInstance")
            	.setAutowire("byName");

     
			for (int x = 0; x < simpleController.getURIs().length; x++) {
				if(!urlMappings.containsKey(simpleController.getURIs()[x]))
					urlMappings.put(simpleController.getURIs()[x], SimpleGrailsController.APPLICATION_CONTEXT_ID);
			}		
		}		
		if (simpleUrlHandlerMapping != null) {
			simpleUrlHandlerMapping
				.addProperty("mappings", urlMappings);
		}
        GrailsTagLibClass[] tagLibs = application.getGrailsTabLibClasses();
        for (int i = 0; i < tagLibs.length; i++) {
            GrailsTagLibClass grailsTagLib = tagLibs[i];
            // setup taglib class by retrieving it from the grails application bean

            springConfig
            	.addSingletonBean(grailsTagLib.getFullName() + "Class",MethodInvokingFactoryBean.class)
            	.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
            	.addProperty("targetMethod", "getGrailsTagLibClass")
            	.addProperty("arguments", grailsTagLib.getFullName());

            // configure taglib class as hot swappable target source
            Collection args = new ManagedList();
            args.add(new RuntimeBeanReference(grailsTagLib.getFullName() + "Class"));

            springConfig
            	.addSingletonBean(	grailsTagLib.getFullName() + "TargetSource",
            						HotSwappableTargetSource.class, 
            						args);

            // setup AOP proxy that uses hot swappable target source            
            springConfig
            	.addSingletonBean(grailsTagLib.getFullName() + "Proxy",ProxyFactoryBean.class)
            	.addProperty("targetSource", new RuntimeBeanReference(grailsTagLib.getFullName() + "TargetSource"))
            	.addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsTagLibClass");
            

            // create prototype bean that refers to the AOP proxied taglib class uses it as a factory
            springConfig
            	.addPrototypeBean(grailsTagLib.getFullName())
            	.setFactoryBean(grailsTagLib.getFullName() + "Proxy")
            	.setFactoryMethod("newInstance")
            	.setAutowire("byName");
        }		
		
	}

	private void populateServiceClassReferences(RuntimeSpringConfiguration springConfig) {
		GrailsServiceClass[] serviceClasses = application.getGrailsServiceClasses();
		for (int i = 0; i <serviceClasses.length; i++) {
			GrailsServiceClass grailsServiceClass = serviceClasses[i];
			
			springConfig
				.addSingletonBean(grailsServiceClass.getFullName() + "Class", MethodInvokingFactoryBean.class)
				.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
				.addProperty("targetMethod", "getGrailsServiceClass")
				.addProperty("arguments", grailsServiceClass.getFullName());
			
			
			BeanConfiguration serviceInstance = springConfig
													.addSingletonBean(grailsServiceClass.getFullName() + "Instance")
													.setFactoryBean( grailsServiceClass.getFullName() + "Class")
													.setFactoryMethod("newInstance");
			
			if (grailsServiceClass.byName()) {
				serviceInstance.setAutowire("byName");
			} else if (grailsServiceClass.byType()) {
				serviceInstance.setAutowire("byType");
			}
            // configure the service instance as a hotswappable target source

            // if its transactional configure transactional proxy
            if (grailsServiceClass.isTransactional()) {
				Properties transactionAttributes = new Properties();
				transactionAttributes.put("*", "PROPAGATION_REQUIRED");
				
				springConfig
					.addSingletonBean(grailsServiceClass.getPropertyName(),TransactionProxyFactoryBean.class)
					.addProperty("target", new RuntimeBeanReference(grailsServiceClass.getFullName() + "Instance"))
					.addProperty("proxyTargetClass", Boolean.TRUE)
					.addProperty("transactionAttributes", transactionAttributes)
					.addProperty(TRANSACTION_MANAGER_BEAN, new RuntimeBeanReference(TRANSACTION_MANAGER_BEAN));
				
			} else {
                // otherwise configure a standard proxy
				springConfig
					.addSingletonBean(grailsServiceClass.getName() + "Service", BeanReferenceFactoryBean.class)
					.addProperty("targetBeanName",grailsServiceClass.getFullName() + "Instance" );
			}
		}
	}

	private void populateDomainClassReferences(RuntimeSpringConfiguration springConfig) {
			GrailsDomainClass[] grailsDomainClasses = application.getGrailsDomainClasses();
		for (int i = 0; i < grailsDomainClasses.length; i++) {
			GrailsDomainClass grailsDomainClass = grailsDomainClasses[i];
			
			springConfig
				.addSingletonBean(grailsDomainClass.getFullName() + "DomainClass", MethodInvokingFactoryBean.class)
				.addProperty("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID, true))
				.addProperty("targetMethod","getGrailsDomainClass")
				.addProperty("arguments", grailsDomainClass.getFullName());

			
            // configure domain class as hot swappable target source
            Collection args = new ManagedList();
            args.add(new RuntimeBeanReference(grailsDomainClass.getFullName() + "DomainClass"));
            springConfig
            	.addSingletonBean(grailsDomainClass.getFullName() + "TargetSource",HotSwappableTargetSource.class, args);
            

            // setup AOP proxy that uses hot swappable target source
            springConfig
            	.addSingletonBean(grailsDomainClass.getFullName() + "Proxy",ProxyFactoryBean.class)
            	.addProperty("targetSource", new RuntimeBeanReference(grailsDomainClass.getFullName() + "TargetSource"))
            	.addProperty("proxyInterfaces", "org.codehaus.groovy.grails.commons.GrailsDomainClass");
            
            
			// create persistent class bean references
			springConfig
				.addSingletonBean(grailsDomainClass.getFullName() + "PersistentClass", MethodInvokingFactoryBean.class)
				.addProperty("targetObject", new RuntimeBeanReference( grailsDomainClass.getFullName() + "Proxy") )
				.addProperty("targetMethod", "getClazz");

			/*Collection constructorArguments = new ArrayList();
			// configure persistent methods
			constructorArguments.add(SpringConfigUtils.createBeanReference("grailsApplication"));
			constructorArguments.add(SpringConfigUtils.createLiteralValue(grailsDomainClass.getClazz().getName()));
			constructorArguments.add(SpringConfigUtils.createBeanReference("sessionFactory"));
			constructorArguments.add(classLoader);
			Bean hibernatePersistentMethods = SpringConfigUtils.createSingletonBean(DomainClassMethods.class, constructorArguments);
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "PersistentMethods", hibernatePersistentMethods));*/

			// configure validator			
			springConfig
				.addSingletonBean(grailsDomainClass.getFullName() + "Validator", GrailsDomainClassValidator.class)
				.addProperty( "domainClass" ,new RuntimeBeanReference(grailsDomainClass.getFullName() + "Proxy") )
				.addProperty(SESSION_FACTORY_BEAN , new RuntimeBeanReference(SESSION_FACTORY_BEAN))
				.addProperty(MESSAGE_SOURCE_BEAN , new RuntimeBeanReference(MESSAGE_SOURCE_BEAN));			
		}
	}

	private void populateDataSourceReferences(RuntimeSpringConfiguration springConfig) {
		
		GrailsDataSource ds = application.getGrailsDataSource();	
		BeanConfiguration localSessionFactoryBean = springConfig
														.addSingletonBean(SESSION_FACTORY_BEAN,ConfigurableLocalSessionFactoryBean.class);
		
		if(ds != null) {
			BeanConfiguration dataSource;
			if(ds.isPooled()) {
				dataSource = springConfig
								.addSingletonBean(DATA_SOURCE_BEAN, BasicDataSource.class)
								.setDestroyMethod("close");
			}
			else {
				dataSource = springConfig
									.addSingletonBean(DATA_SOURCE_BEAN, DriverManagerDataSource.class);
			}
			dataSource
				.addProperty("driverClassName", ds.getDriverClassName())
				.addProperty("url", ds.getUrl())
				.addProperty("username", ds.getUsername())
				.addProperty("password",ds.getPassword());
			
            if(ds.getConfigurationClass() != null) {
                LOG.info("[SpringConfig] Using custom Hibernate configuration class ["+ds.getConfigurationClass()+"]");
                localSessionFactoryBean
                	.addProperty("configClass", ds.getConfigurationClass());
            }			
		}
		else {
			// if no data source exists create in-memory HSQLDB instance
			springConfig
				.addSingletonBean(DATA_SOURCE_BEAN, BasicDataSource.class)
				.setDestroyMethod("close")
				.addProperty("driverClassName", "org.hsqldb.jdbcDriver")
				.addProperty("url", "jdbc:hsqldb:mem:grailsDB")
				.addProperty("username", "sa")
				.addProperty("password","");
		}
	
		// set-up auto detection of Hibernate dialect
		// first attempt to load any extra dialects defined in hibernate-dialects.properties
        Properties vendorNameDialectMappings = new Properties();
		URL hibernateDialects = this.application.getClassLoader().getResource("hibernate-dialects.properties");
		if(hibernateDialects != null) {
			Properties p = new Properties();
			try {
				p.load(hibernateDialects.openStream());
				Iterator iter = p.entrySet().iterator();
				while(iter.hasNext()) {
					Map.Entry e = (Map.Entry)iter.next();
					// Note: the entry is reversed in the properties file since the database product
					// name can contain spaces.
					vendorNameDialectMappings.put(e.getValue(), "org.hibernate.dialect." + e.getKey());
				}
			} catch (IOException e) {
				LOG.info("[SpringConfig] Error loading hibernate-dialects.properties file: " + e.getMessage());
			}
		}
		
		// setup dialect detector bean
		springConfig
			.addSingletonBean(DIALECT_DETECTOR_BEAN, HibernateDialectDetectorFactoryBean.class)
			.addProperty(DATA_SOURCE_BEAN, new RuntimeBeanReference(DATA_SOURCE_BEAN))
			.addProperty("vendorNameDialectMappings",vendorNameDialectMappings);
		
		
		
		ManagedMap hibernatePropertiesMap = new ManagedMap();
		// enable sql logging yes/no
		if(ds != null && ds.isLoggingSql()) {
			hibernatePropertiesMap.put("hibernate.show_sql","true");
			hibernatePropertiesMap.put("hibernate.format_sql","true");
		}
		// configure dialect
		if(ds != null && ds.getDialect()!= null) {
			hibernatePropertiesMap.put("hibernate.dialect", ds.getDialect().getName());
		}
		else {
			hibernatePropertiesMap.put("hibernate.dialect", new RuntimeBeanReference(DIALECT_DETECTOR_BEAN));
		}
		if(ds == null ) {
			hibernatePropertiesMap.put("hibernate.hbm2ddl.auto", "create-drop");
		}
		else {
			if(ds.getDbCreate() != null) {
				hibernatePropertiesMap.put("hibernate.hbm2ddl.auto", ds.getDbCreate());
			}
		}
		
		localSessionFactoryBean
			.addProperty(DATA_SOURCE_BEAN,new RuntimeBeanReference(DATA_SOURCE_BEAN));
		
		if(loadExternalPersistenceConfig) {
			URL hibernateConfig = application.getClassLoader().getResource("hibernate.cfg.xml");
			if(hibernateConfig != null) {
				localSessionFactoryBean
					.addProperty("configLocation", "classpath:hibernate.cfg.xml");
			}		
		}
		
		springConfig
			.addSingletonBean(HIBERNATE_PROPERTIES_BEAN,MapToPropertiesFactoryBean.class)
			.addProperty("map", hibernatePropertiesMap);
				
		
		
		localSessionFactoryBean
			.addProperty(HIBERNATE_PROPERTIES_BEAN, new RuntimeBeanReference(HIBERNATE_PROPERTIES_BEAN))
			.addProperty(GrailsApplication.APPLICATION_ID, new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
			.addProperty("classLoader", new RuntimeBeanReference(CLASS_LOADER_BEAN));
		

		springConfig
			.addSingletonBean(TRANSACTION_MANAGER_BEAN,HibernateTransactionManager.class)
			.addProperty(SESSION_FACTORY_BEAN, new RuntimeBeanReference(SESSION_FACTORY_BEAN));
	}

	private void populateI18nSupport(RuntimeSpringConfiguration springConfig) {
		// set-up message source
		springConfig
			.addSingletonBean(MESSAGE_SOURCE_BEAN, ReloadableResourceBundleMessageSource.class)
			.addProperty("basename","WEB-INF/grails-app/i18n/messages");
		
		// create locale change interceptor
		springConfig
			.addSingletonBean("localChangeInterceptor",LocaleChangeInterceptor.class)
			.addProperty("paramName","lang");
		
		// set-up cookie based locale resolver
		springConfig
			.addSingletonBean("localeResolver",CookieLocaleResolver.class);
	}

	public void refreshSessionFactory(GrailsApplication app, GrailsWebApplicationContext context) {

		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		BeanConfiguration sessionFactoryBean = springConfig
												.createSingletonBean(ConfigurableLocalSessionFactoryBean.class)
												.addProperty("grailsApplication",new RuntimeBeanReference(GrailsApplication.APPLICATION_ID,true))
												.addProperty("classLoader", new RuntimeBeanReference(CLASS_LOADER_BEAN))
												.addProperty("dataSource", new RuntimeBeanReference(DATA_SOURCE_BEAN))
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
		GrailsDomainConfigurationUtil.configureDynamicMethods(sf,app);
		
		// update transaction manager reference
		if(context.containsBean(TRANSACTION_MANAGER_BEAN)) {
			((HibernateTransactionManager)context.getBean(TRANSACTION_MANAGER_BEAN)).setSessionFactory(sf);
		}
		// update OSIVI reference
		if(context.containsBean(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN)) {
			((OpenSessionInViewInterceptor)context.getBean(OPEN_SESSION_IN_VIEW_INTERCEPTOR_BEAN)).setSessionFactory(sf);
		}
		// update validators
		String[] validatorBeanNames = context.getBeanDefinitionNames(Validator.class);
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
}
