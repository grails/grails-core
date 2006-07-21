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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsPageFlowClass;
import org.codehaus.groovy.grails.commons.GrailsServiceClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.GrailsTaskClass;
import org.codehaus.groovy.grails.commons.GrailsTaskClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsResponseHandlerFactory;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsScaffoldViewResolver;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsScaffolder;
import org.codehaus.groovy.grails.scaffolding.DefaultScaffoldRequestHandler;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffoldDomain;
import org.codehaus.groovy.grails.scaffolding.ViewDelegatingScaffoldResponseHandler;
import org.codehaus.groovy.grails.support.ClassEditor;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.pageflow.GrailsFlowBuilder;
import org.codehaus.groovy.grails.web.pageflow.execution.servlet.GrailsServletFlowExecutionManager;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
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
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.webflow.config.FlowFactoryBean;
import org.springframework.webflow.mvc.FlowController;
import org.springmodules.beans.factory.config.MapToPropertiesFactoryBean;
import org.springmodules.beans.factory.drivers.Bean;
import org.springmodules.db.hsqldb.ServerBean;

/**
 * <p>Creates beans and bean references for a Grails application.
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 * 
 * @deprecated Use org.codeaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator instead
 */
public class SpringConfig {

	private GrailsApplication application = null;
	private static final Log LOG = LogFactory.getLog(SpringConfig.class);

	public SpringConfig(GrailsApplication application) {
		super();
		this.application = application;
	}

	public Collection getBeanReferences() {
		Collection beanReferences = new ArrayList();
		Map urlMappings = new HashMap();

        /**
         * Regarding conversion to plugins:
         *
         * N/A: not applicable, does not need to be converted
         * OK: already converted to plugin (please mention plugin name
         */


        Assert.notNull(application);

		// configure general references
        // N/A
        Bean classLoader = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
		classLoader.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
		classLoader.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getClassLoader"));

        // Register custom class editor
        // OK: org.codehaus.groovy.grails.plugins.support.classeditor.ClassEditorPlugin
        Bean classEditor = SpringConfigUtils.createSingletonBean(ClassEditor.class);
		classEditor.setProperty("classLoader", classLoader);
		
		Bean propertyEditors = SpringConfigUtils.createSingletonBean(CustomEditorConfigurer.class);
		Map customEditors = new HashMap();
		customEditors.put(SpringConfigUtils.createLiteralValue("java.lang.Class"), classEditor);
		propertyEditors.setProperty("customEditors", SpringConfigUtils.createMap(customEditors));
		beanReferences.add(SpringConfigUtils.createBeanReference("customEditors", propertyEditors));
			
		// configure exception handler
        // OK: org.codehaus.groovy.grails.web.plugins.support.exceptionResolver.GrailsExceptionResolverPlugin
        Bean exceptionHandler = SpringConfigUtils.createSingletonBean(GrailsExceptionResolver.class);
		exceptionHandler.setProperty("exceptionMappings", SpringConfigUtils.createLiteralValue("java.lang.Exception=error"));
		beanReferences.add(SpringConfigUtils.createBeanReference("exceptionHandler", exceptionHandler));

        // configure multipart support
        // OK: org.codehaus.groovy.grails.web.plugins.support.multipartresolver.CommonsMultipartResolverPlugin
        Bean multipartResolver = SpringConfigUtils.createSingletonBean(CommonsMultipartResolver.class);
        beanReferences.add(SpringConfigUtils.createBeanReference("multipartResolver", multipartResolver));

        // configure data source & hibernate
		LOG.info("[SpringConfig] Configuring i18n support");
		populateI18nSupport(beanReferences);		
		
		// configure data source & hibernate
		LOG.info("[SpringConfig] Configuring Grails data source");
		populateDataSourceReferences(beanReferences);
		
		// configure domain classes
		LOG.info("[SpringConfig] Configuring Grails domain");
		populateDomainClassReferences(beanReferences, classLoader);
				
		// configure services
		LOG.info("[SpringConfig] Configuring Grails services");
		populateServiceClassReferences(beanReferences);
		
		// configure grails page flows
		LOG.info("[SpringConfig] Configuring GrabuildSessionFactoryils page flows");
		populatePageFlowReferences(beanReferences, urlMappings);
	
		// configure grails controllers
		LOG.info("[SpringConfig] Configuring Grails controllers");
		populateControllerReferences(beanReferences, urlMappings);
		
		// configure scaffolding
		LOG.info("[SpringConfig] Configuring Grails scaffolding");
		populateScaffoldingReferences(beanReferences);
		
		// configure tasks
		LOG.info("[SpringConfig] Configuring Grails tasks");
		populateTasksReferences(beanReferences);		
		
				
		return beanReferences;
	}
	
	// configures tasks
	private void populateTasksReferences(Collection beanReferences) {
		GrailsTaskClass[] grailsTaskClasses = application.getGrailsTasksClasses();
		
        Collection schedulerReferences = new ArrayList();
		
        // todo jdbc job loading
        
		for (int i = 0; i < grailsTaskClasses.length; i++) {
			GrailsTaskClass grailsTaskClass = grailsTaskClasses[i];
			
			Bean taskClassBean = 	SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			taskClassBean.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
			taskClassBean.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getGrailsTaskClass"));
			taskClassBean.setProperty("arguments", SpringConfigUtils.createLiteralValue(grailsTaskClass.getFullName()));
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName() + "Class", taskClassBean));			

			/* additional indirrection so that autowireing would be possible */
			Bean taskInstance = SpringConfigUtils.createSingletonBean();
			taskInstance.setFactoryBean(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName() + "Class"));
			taskInstance.setFactoryMethod("newInstance");
			taskInstance.setAutowire("byName");
			beanReferences.add(SpringConfigUtils.createBeanReference( grailsTaskClass.getFullName(), taskInstance));
			
			Bean jobDetailFactoryBean = SpringConfigUtils.createSingletonBean( MethodInvokingJobDetailFactoryBean.class );
			jobDetailFactoryBean.setProperty("targetObject", SpringConfigUtils.createBeanReference( grailsTaskClass.getFullName() ));
			jobDetailFactoryBean.setProperty("targetMethod", SpringConfigUtils.createLiteralValue(GrailsTaskClassProperty.EXECUTE));
			jobDetailFactoryBean.setProperty("group", SpringConfigUtils.createLiteralValue(grailsTaskClass.getGroup()));			
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"JobDetail", jobDetailFactoryBean));
			
			if( !grailsTaskClass.isCronExpressionConfigured() ){		/* configuring Task using startDelay and timeOut */
			
				Bean triggerBean = SpringConfigUtils.createSingletonBean(SimpleTriggerBean.class);
				triggerBean.setProperty("jobDetail", SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"JobDetail"));
				triggerBean.setProperty("startDelay", SpringConfigUtils.createLiteralValue( grailsTaskClass.getStartDelay() ));
				triggerBean.setProperty("repeatInterval", SpringConfigUtils.createLiteralValue( grailsTaskClass.getTimeout() ));				
				beanReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"SimpleTrigger", triggerBean));
				
				schedulerReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"SimpleTrigger"));
			
			}else{	/* configuring Task using cronExpression */
				
				Bean triggerBean = SpringConfigUtils.createSingletonBean(CronTriggerBean.class);
				triggerBean.setProperty("jobDetail", SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"JobDetail"));
				triggerBean.setProperty("cronExpression", SpringConfigUtils.createLiteralValue(grailsTaskClass.getCronExpression()));
				beanReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"CronTrigger", triggerBean));
				
				schedulerReferences.add(SpringConfigUtils.createBeanReference(grailsTaskClass.getFullName()+"CronTrigger"));				
				
			}
		
		}
		
		Bean schedulerFactoryBean = SpringConfigUtils.createSingletonBean(SchedulerFactoryBean.class);
        schedulerFactoryBean.setProperty("triggers", SpringConfigUtils.createList(schedulerReferences));
        beanReferences.add(SpringConfigUtils.createBeanReference("GrailsSchedulerBean",schedulerFactoryBean));
		
	}

	private void populateI18nSupport(Collection beanReferences) {
		// setup message source
        // OK: org.codehaus.groovy.grails.web.plugins.support.i18n.ReloadableResourceBundleMessageSourcePlugin
        Bean messageSource = SpringConfigUtils.createSingletonBean( ReloadableResourceBundleMessageSource.class );
		messageSource.setProperty( "basename", SpringConfigUtils.createLiteralValue("WEB-INF/classes/grails-app/i18n/messages"));
		beanReferences.add(SpringConfigUtils.createBeanReference("messageSource", messageSource));
		
		// setup locale change interceptor
        // OK: org.codehaus.groovy.grails.web.plugins.support.i18n.LocaleChangeInterceptorPlugin
        Bean localeChangeInterceptor = SpringConfigUtils.createSingletonBean(LocaleChangeInterceptor.class);
		localeChangeInterceptor.setProperty("paramName", SpringConfigUtils.createLiteralValue("lang"));
		beanReferences.add(SpringConfigUtils.createBeanReference("localeChangeInterceptor", localeChangeInterceptor));
		
		// setup locale resolver
        // OK: org.codehaus.groovy.grails.web.plugins.support.i18n.CookieLocaleResolverPlugin
        Bean localeResolver = SpringConfigUtils.createSingletonBean(CookieLocaleResolver.class);
		beanReferences.add(SpringConfigUtils.createBeanReference("localeResolver", localeResolver));
	}
	// configures scaffolding
	private void populateScaffoldingReferences(Collection beanReferences) {
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
                Bean scaffolder = SpringConfigUtils.createSingletonBean(DefaultGrailsScaffolder.class);

                // create scaffold domain
                Collection constructorArguments = new ArrayList();
                constructorArguments.add(SpringConfigUtils.createLiteralValue(scaffoldedClass.getName()));

                constructorArguments.add(SpringConfigUtils.createBeanReference("sessionFactory"));

                Bean domain = SpringConfigUtils.createSingletonBean(GrailsScaffoldDomain.class, constructorArguments);
//                domain.setProperty("validator", SpringConfigUtils.createBeanReference( domainClass.getFullName() + "Validator"));

                beanReferences.add( SpringConfigUtils.createBeanReference( scaffoldedClass.getName() + "ScaffoldDomain",domain ) );

                // create and configure request handler
                Bean requestHandler = SpringConfigUtils.createSingletonBean(DefaultScaffoldRequestHandler.class);
                requestHandler.setProperty("scaffoldDomain", SpringConfigUtils.createBeanReference(scaffoldedClass.getName() + "ScaffoldDomain"));

                // create response factory
                constructorArguments = new ArrayList();
                constructorArguments.add(SpringConfigUtils.createBeanReference("grailsApplication"));

                // configure default response handler
                Bean defaultResponseHandler = SpringConfigUtils.createSingletonBean(ViewDelegatingScaffoldResponseHandler.class);

                // configure a simple view delegating resolver
                Bean defaultViewResolver = SpringConfigUtils.createSingletonBean(DefaultGrailsScaffoldViewResolver.class,constructorArguments);
                defaultResponseHandler.setProperty("scaffoldViewResolver", defaultViewResolver);

                // create constructor arguments response handler factory
                constructorArguments = new ArrayList();
                constructorArguments.add(SpringConfigUtils.createBeanReference("grailsApplication"));
                constructorArguments.add(defaultResponseHandler);

                Bean responseHandlerFactory = SpringConfigUtils.createSingletonBean( DefaultGrailsResponseHandlerFactory.class,constructorArguments );

                scaffolder.setProperty( "scaffoldResponseHandlerFactory", responseHandlerFactory );
                scaffolder.setProperty("scaffoldRequestHandler", requestHandler);

                beanReferences.add( SpringConfigUtils.createBeanReference( simpleControllers[i].getFullName() + "Scaffolder",scaffolder  ) );
            }
		}
	}

	private void populateControllerReferences(Collection beanReferences, Map urlMappings) {

		Bean simpleGrailsController = SpringConfigUtils.createSingletonBean(SimpleGrailsController.class);
		simpleGrailsController.setAutowire("byType");
		beanReferences.add(SpringConfigUtils.createBeanReference(SimpleGrailsController.APPLICATION_CONTEXT_ID, simpleGrailsController));

        Bean grailsViewResolver = SpringConfigUtils.createSingletonBean(GrailsViewResolver.class);
		
		grailsViewResolver.setProperty("viewClass",SpringConfigUtils.createLiteralValue("org.springframework.web.servlet.view.JstlView"));
		grailsViewResolver.setProperty("prefix", SpringConfigUtils.createLiteralValue(GrailsApplicationAttributes.PATH_TO_VIEWS));
		grailsViewResolver.setProperty("suffix", SpringConfigUtils.createLiteralValue(".jsp"));
		beanReferences.add(SpringConfigUtils.createBeanReference("jspViewResolver", grailsViewResolver));
		
		Bean simpleUrlHandlerMapping = null;
		if (application.getControllers().length > 0 || application.getPageFlows().length > 0) {
			simpleUrlHandlerMapping = SpringConfigUtils.createSingletonBean(GrailsUrlHandlerMapping.class);
			//beanReferences.add(SpringConfigUtils.createBeanReference(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_ID + "Target", simpleUrlHandlerMapping));

            Collection args = new ArrayList();
            args.add(simpleUrlHandlerMapping);
            Bean simpleUrlHandlerMappingTargetSource = SpringConfigUtils.createSingletonBean(HotSwappableTargetSource.class, args);
            beanReferences.add(SpringConfigUtils.createBeanReference(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE,simpleUrlHandlerMappingTargetSource));

            // configure handler interceptors
            Bean openSessionInViewInterceptor = SpringConfigUtils.createSingletonBean(OpenSessionInViewInterceptor.class);
            openSessionInViewInterceptor.setProperty("flushMode",SpringConfigUtils.createLiteralValue(String.valueOf(HibernateAccessor.FLUSH_AUTO)));
            openSessionInViewInterceptor.setProperty("sessionFactory", SpringConfigUtils.createBeanReference("sessionFactory"));
            beanReferences.add(SpringConfigUtils.createBeanReference("openSessionInViewInterceptor",openSessionInViewInterceptor));


            Collection interceptors = new ArrayList();
            interceptors.add(SpringConfigUtils.createBeanReference("openSessionInViewInterceptor"));
            simpleUrlHandlerMapping.setProperty("interceptors", SpringConfigUtils.createList(interceptors));            

            Bean simpleUrlHandlerMappingProxy = SpringConfigUtils.createSingletonBean(ProxyFactoryBean.class);
            simpleUrlHandlerMappingProxy.setProperty("targetSource", SpringConfigUtils.createBeanReference(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE));
            simpleUrlHandlerMappingProxy.setProperty("proxyInterfaces", SpringConfigUtils.createLiteralValue("org.springframework.web.servlet.HandlerMapping"));
            beanReferences.add(SpringConfigUtils.createBeanReference(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_ID, simpleUrlHandlerMappingProxy));
        }
		
		GrailsControllerClass[] simpleControllers = application.getControllers();
		for (int i = 0; i < simpleControllers.length; i++) {
			GrailsControllerClass simpleController = simpleControllers[i];
			if (!simpleController.getAvailable()) {
				continue;
			}
            // setup controller class by retrieving it from the  grails application
            Bean controllerClass = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			controllerClass.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
			controllerClass.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getController"));
			controllerClass.setProperty("arguments", SpringConfigUtils.createLiteralValue(simpleController.getFullName()));
			beanReferences.add(SpringConfigUtils.createBeanReference(simpleController.getFullName() + "Class", controllerClass));

            // configure controller class as hot swappable target source
            Collection args = new ArrayList();
            args.add(SpringConfigUtils.createBeanReference(simpleController.getFullName() + "Class"));
            Bean controllerTargetSource = SpringConfigUtils.createSingletonBean(HotSwappableTargetSource.class, args);
            beanReferences.add(SpringConfigUtils.createBeanReference(simpleController.getFullName() + "TargetSource", controllerTargetSource));

            // setup AOP proxy that uses hot swappable target source
            Bean controllerClassProxy = SpringConfigUtils.createSingletonBean(ProxyFactoryBean.class);
            controllerClassProxy.setProperty("targetSource", SpringConfigUtils.createBeanReference(simpleController.getFullName() + "TargetSource"));
            controllerClassProxy.setProperty("proxyInterfaces", SpringConfigUtils.createLiteralValue("org.codehaus.groovy.grails.commons.GrailsControllerClass"));
            beanReferences.add(SpringConfigUtils.createBeanReference(simpleController.getFullName() + "Proxy", controllerClassProxy));

            // create prototype bean that uses the controller AOP proxy controller class bean as a factory
            Bean controller = SpringConfigUtils.createPrototypeBean();
			controller.setFactoryBean(SpringConfigUtils.createBeanReference(simpleController.getFullName() + "Proxy"));
			controller.setFactoryMethod("newInstance");
			controller.setAutowire("byName");

            beanReferences.add(SpringConfigUtils.createBeanReference(simpleController.getFullName(), controller));
			for (int x = 0; x < simpleController.getURIs().length; x++) {
				if(!urlMappings.containsKey(simpleController.getURIs()[x]))
					urlMappings.put(simpleController.getURIs()[x], SimpleGrailsController.APPLICATION_CONTEXT_ID);
			}		
		}		
		if (simpleUrlHandlerMapping != null) {
			simpleUrlHandlerMapping.setProperty("mappings", SpringConfigUtils.createProperties(urlMappings));
		}

        GrailsTagLibClass[] tagLibs = application.getGrailsTabLibClasses();
        for (int i = 0; i < tagLibs.length; i++) {
            GrailsTagLibClass grailsTagLib = tagLibs[i];
            // setup taglib class by retrieving it from the grails application bean
            Bean taglibClass = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
            taglibClass.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
            taglibClass.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getGrailsTagLibClass"));
            taglibClass.setProperty("arguments", SpringConfigUtils.createLiteralValue(grailsTagLib.getFullName()));
            beanReferences.add(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "Class", taglibClass));

            // configure taglib class as hot swappable target source
            Collection args = new ArrayList();
            args.add(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "Class"));
            Bean taglibTargetSource = SpringConfigUtils.createSingletonBean(HotSwappableTargetSource.class, args);

            // setup AOP proxy that uses hot swappable target source
            beanReferences.add(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "TargetSource", taglibTargetSource));
            Bean taglibClassProxy = SpringConfigUtils.createSingletonBean(ProxyFactoryBean.class);
            taglibClassProxy.setProperty("targetSource", SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "TargetSource"));
            taglibClassProxy.setProperty("proxyInterfaces", SpringConfigUtils.createLiteralValue("org.codehaus.groovy.grails.commons.GrailsTagLibClass"));
            beanReferences.add(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "Proxy", taglibClassProxy));

            // create prototype bean that refers to the AOP proxied taglib class uses it as a factory
            Bean taglib = SpringConfigUtils.createPrototypeBean();
            taglib.setFactoryBean(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName() + "Proxy"));
            taglib.setFactoryMethod("newInstance");
            taglib.setAutowire("byName");

            beanReferences.add(SpringConfigUtils.createBeanReference(grailsTagLib.getFullName(), taglib));
        }
    }

	private void populateDataSourceReferences(Collection beanReferences) {
		boolean dependsOnHsqldbServer = false;
		GrailsDataSource grailsDataSource = application.getGrailsDataSource();
        Bean localSessionFactoryBean = SpringConfigUtils.createSingletonBean(ConfigurableLocalSessionFactoryBean.class);

        if (grailsDataSource != null) {

            // OK: org.codehaus.groovy.grails.plugins.datasource.PooledDatasourcePlugin
            // OK: org.codehaus.groovy.grails.plugins.datasource.NonPooledDataSourcePlugin
            Bean dataSource;
			if (grailsDataSource.isPooled()) {
				dataSource = SpringConfigUtils.createSingletonBean(BasicDataSource.class);
				dataSource.setDestroyMethod("close");
			} else {
				dataSource = SpringConfigUtils.createSingletonBean(DriverManagerDataSource.class);
			}
			dataSource.setProperty("driverClassName", SpringConfigUtils.createLiteralValue(grailsDataSource.getDriverClassName()));
			dataSource.setProperty("url", SpringConfigUtils.createLiteralValue(grailsDataSource.getUrl()));
			dataSource.setProperty("username", SpringConfigUtils.createLiteralValue(grailsDataSource.getUsername()));
			dataSource.setProperty("password", SpringConfigUtils.createLiteralValue(grailsDataSource.getPassword()));
            if(grailsDataSource.getConfigurationClass() != null) {
                LOG.info("[SpringConfig] Using custom Hibernate configuration class ["+grailsDataSource.getConfigurationClass()+"]");
                localSessionFactoryBean.setProperty("configClass", SpringConfigUtils.createLiteralValue(grailsDataSource.getConfigurationClass().getName()));
            }
            beanReferences.add(SpringConfigUtils.createBeanReference("dataSource", dataSource));
		} else {
            // N/A
			Bean dataSource = SpringConfigUtils.createSingletonBean(BasicDataSource.class);
			dataSource.setDestroyMethod("close");
			dataSource.setProperty("driverClassName", SpringConfigUtils.createLiteralValue("org.hsqldb.jdbcDriver"));
			dataSource.setProperty("url", SpringConfigUtils.createLiteralValue("jdbc:hsqldb:hsql://localhost:9101/"));
			dataSource.setProperty("username", SpringConfigUtils.createLiteralValue("sa"));
			dataSource.setProperty("password", SpringConfigUtils.createLiteralValue(""));
			beanReferences.add(SpringConfigUtils.createBeanReference("dataSource", dataSource));
			
			Bean hsqldbServer = SpringConfigUtils.createSingletonBean(ServerBean.class);
			hsqldbServer.setProperty("dataSource", SpringConfigUtils.createBeanReference("dataSource"));
			Map hsqldbProperties = new HashMap();
			hsqldbProperties.put("server.port", "9101");
			hsqldbProperties.put("server.database.0", "mem:temp");
			hsqldbServer.setProperty("serverProperties", SpringConfigUtils.createProperties(hsqldbProperties));
			beanReferences.add(SpringConfigUtils.createBeanReference("hsqldbServer", hsqldbServer));
			dependsOnHsqldbServer = true;
		}

        // OK: org.codehaus.groovy.grails.plugins.hibernate.HibernateDialectDetectorPlugin
        Map vendorNameDialectMappings = new HashMap();
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
			
		Bean dialectDetector = SpringConfigUtils.createSingletonBean(HibernateDialectDetectorFactoryBean.class);
		dialectDetector.setProperty("dataSource", SpringConfigUtils.createBeanReference("dataSource"));
		dialectDetector.setProperty("vendorNameDialectMappings", SpringConfigUtils.createProperties(vendorNameDialectMappings));
		if (dependsOnHsqldbServer) {
			Collection dependsOn = new ArrayList();
			dependsOn.add(SpringConfigUtils.createBeanReference("hsqldbServer"));
			dialectDetector.setDependsOn(dependsOn);
		}
		
		Map hibernatePropertiesMap = new HashMap();
		// enable sql logging yes/no
		if(grailsDataSource != null && grailsDataSource.isLoggingSql()) {
			hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.show_sql"),SpringConfigUtils.createLiteralValue("true"));
			hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.format_sql"),SpringConfigUtils.createLiteralValue("true"));
		}
		// configure dialect
		if(grailsDataSource != null && grailsDataSource.getDialect()!= null) {
			hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.dialect"), SpringConfigUtils.createLiteralValue(grailsDataSource.getDialect().getName()));
		}
		else {
			hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.dialect"), dialectDetector);
		}
		if(grailsDataSource == null ) {
			hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.hbm2ddl.auto"), SpringConfigUtils.createLiteralValue("create-drop"));
		}
		else {
			if(grailsDataSource.getDbCreate() != null) {
				hibernatePropertiesMap.put(SpringConfigUtils.createLiteralValue("hibernate.hbm2ddl.auto"), SpringConfigUtils.createLiteralValue(grailsDataSource.getDbCreate()));
			}
		}
		Bean hibernateProperties = SpringConfigUtils.createSingletonBean(MapToPropertiesFactoryBean.class);
		hibernateProperties.setProperty("map", SpringConfigUtils.createMap(hibernatePropertiesMap));

        // N/A
        Bean grailsClassLoader = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
		grailsClassLoader.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
		grailsClassLoader.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getClassLoader"));
		

		localSessionFactoryBean.setProperty("dataSource", SpringConfigUtils.createBeanReference("dataSource"));
		ClassLoader cl = this.application.getClassLoader();
		URL hibernateConfig = cl.getResource("hibernate.cfg.xml");
		if(hibernateConfig != null) {
			localSessionFactoryBean.setProperty("configLocation", SpringConfigUtils.createLiteralValue("classpath:hibernate.cfg.xml"));
		}
        localSessionFactoryBean.setProperty("hibernateProperties", hibernateProperties);
		localSessionFactoryBean.setProperty("grailsApplication", SpringConfigUtils.createBeanReference("grailsApplication"));
		localSessionFactoryBean.setProperty("classLoader", grailsClassLoader);
		beanReferences.add(SpringConfigUtils.createBeanReference("sessionFactory", localSessionFactoryBean));

        // OK: org.codehaus.groovy.grails.plugins.hibernate.HibernateTransactionManagerPlugin
        Bean transactionManager = SpringConfigUtils.createSingletonBean(HibernateTransactionManager.class);
		transactionManager.setProperty("sessionFactory", SpringConfigUtils.createBeanReference("sessionFactory"));
		beanReferences.add(SpringConfigUtils.createBeanReference("transactionManager", transactionManager));
	}

	private void populateDomainClassReferences(Collection beanReferences, Bean classLoader) {
		GrailsDomainClass[] grailsDomainClasses = application.getGrailsDomainClasses();
		for (int i = 0; i < grailsDomainClasses.length; i++) {
			GrailsDomainClass grailsDomainClass = grailsDomainClasses[i];
			
			Bean domainClassBean = 	SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			domainClassBean.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
			domainClassBean.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getGrailsDomainClass"));
			domainClassBean.setProperty("arguments", SpringConfigUtils.createLiteralValue(grailsDomainClass.getFullName()));
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "DomainClass", domainClassBean));
			
			// create persistent class bean references
			Bean persistentClassBean = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			persistentClassBean.setProperty("targetObject", SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "DomainClass"));
			persistentClassBean.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getClazz"));
			
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "PersistentClass", persistentClassBean));
			
			/*Collection constructorArguments = new ArrayList();
			// configure persistent methods
			constructorArguments.add(SpringConfigUtils.createBeanReference("grailsApplication"));
			constructorArguments.add(SpringConfigUtils.createLiteralValue(grailsDomainClass.getClazz().getName()));
			constructorArguments.add(SpringConfigUtils.createBeanReference("sessionFactory"));
			constructorArguments.add(classLoader);
			Bean hibernatePersistentMethods = SpringConfigUtils.createSingletonBean(DomainClassMethods.class, constructorArguments);
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "PersistentMethods", hibernatePersistentMethods));*/

			// configure validator			
			Bean validatorBean = SpringConfigUtils.createSingletonBean( GrailsDomainClassValidator.class);
			validatorBean.setProperty( "domainClass" ,SpringConfigUtils.createBeanReference(grailsDomainClass.getFullName() + "DomainClass") );
			validatorBean.setProperty( "sessionFactory" ,SpringConfigUtils.createBeanReference("sessionFactory") );
			validatorBean.setProperty( "messageSource" ,SpringConfigUtils.createBeanReference("messageSource") );
			beanReferences.add( SpringConfigUtils.createBeanReference( grailsDomainClass.getFullName() + "Validator", validatorBean ) );			
		}
	}

	private void populateServiceClassReferences(Collection beanReferences) {
		GrailsServiceClass[] serviceClasses = application.getGrailsServiceClasses();
		for (int i = 0; i <serviceClasses.length; i++) {
			GrailsServiceClass grailsServiceClass = serviceClasses[i];
			Bean serviceClass = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			serviceClass.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
			serviceClass.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getGrailsServiceClass"));
			serviceClass.setProperty("arguments", SpringConfigUtils.createLiteralValue(grailsServiceClass.getFullName()));
			beanReferences.add(SpringConfigUtils.createBeanReference(grailsServiceClass.getFullName() + "Class", serviceClass));
			
			Bean serviceInstance = SpringConfigUtils.createSingletonBean();
			serviceInstance.setFactoryBean(SpringConfigUtils.createBeanReference(grailsServiceClass.getFullName() + "Class"));
			serviceInstance.setFactoryMethod("newInstance");
			if (grailsServiceClass.byName()) {
				serviceInstance.setAutowire("byName");
			} else if (grailsServiceClass.byType()) {
				serviceInstance.setAutowire("byType");
			}
            // configure the service instance as a hotswappable target source

            // if its transactional configure transactional proxy
            if (grailsServiceClass.isTransactional()) {
				Map transactionAttributes = new HashMap();
				transactionAttributes.put("*", "PROPAGATION_REQUIRED");
				Bean transactionalProxy = SpringConfigUtils.createSingletonBean(TransactionProxyFactoryBean.class);
				transactionalProxy.setProperty("target", serviceInstance);
				transactionalProxy.setProperty("proxyTargetClass", SpringConfigUtils.createLiteralValue("true"));
				transactionalProxy.setProperty("transactionAttributes", SpringConfigUtils.createProperties(transactionAttributes));
				transactionalProxy.setProperty("transactionManager", SpringConfigUtils.createBeanReference("transactionManager"));
				beanReferences.add(SpringConfigUtils.createBeanReference(WordUtils.uncapitalize(grailsServiceClass.getName()) + "Service", transactionalProxy));
			} else {
                // otherwise configure a standard proxy
                beanReferences.add(SpringConfigUtils.createBeanReference(WordUtils.uncapitalize(grailsServiceClass.getName()) + "Service", serviceInstance));
			}
		}
	}

	/**
	 * Configures Grails page flows
	 */
	private void populatePageFlowReferences(Collection beanReferences, Map urlMappings) {
		GrailsPageFlowClass[] pageFlows = application.getPageFlows();
		
		for (int i = 0; i < pageFlows.length; i++) {
			GrailsPageFlowClass pageFlow = pageFlows[i];
			if (!pageFlow.getAvailable()) {
				continue;
			}
			Bean pageFlowClass = SpringConfigUtils.createSingletonBean(MethodInvokingFactoryBean.class);
			pageFlowClass.setProperty("targetObject", SpringConfigUtils.createBeanReference("grailsApplication"));
			pageFlowClass.setProperty("targetMethod", SpringConfigUtils.createLiteralValue("getPageFlow"));
			pageFlowClass.setProperty("arguments", SpringConfigUtils.createLiteralValue(pageFlow.getFullName()));
			beanReferences.add(SpringConfigUtils.createBeanReference(pageFlow.getFullName() + "Class", pageFlowClass));
			
			Bean pageFlowInstance = SpringConfigUtils.createSingletonBean();
			pageFlowInstance.setFactoryBean(SpringConfigUtils.createBeanReference(pageFlow.getFullName() + "Class"));
			pageFlowInstance.setFactoryMethod("newInstance");
			if (pageFlow.byType()) {
				pageFlowInstance.setAutowire("byType");
			} else if (pageFlow.byName()) {
				pageFlowInstance.setAutowire("byName");
			}
			beanReferences.add(SpringConfigUtils.createBeanReference(pageFlow.getFullName(), pageFlowInstance));
			
			Bean flowBuilder = SpringConfigUtils.createSingletonBean(GrailsFlowBuilder.class);
			flowBuilder.setProperty("pageFlowClass", SpringConfigUtils.createBeanReference(pageFlow.getFullName() + "Class"));
			
			Bean flowFactoryBean = SpringConfigUtils.createSingletonBean(FlowFactoryBean.class);
			flowFactoryBean.setProperty("flowBuilder", flowBuilder);
			beanReferences.add(SpringConfigUtils.createBeanReference(pageFlow.getFlowId(), flowFactoryBean));

			if (pageFlow.getAccessible()) {
				Bean flowExecutionManager = SpringConfigUtils.createSingletonBean(GrailsServletFlowExecutionManager.class);
				flowExecutionManager.setProperty("flow", SpringConfigUtils.createBeanReference(pageFlow.getFlowId()));
				
				Bean flowController = SpringConfigUtils.createSingletonBean(FlowController.class);
				flowController.setProperty("flowExecutionManager", flowExecutionManager);
				beanReferences.add(SpringConfigUtils.createBeanReference(pageFlow.getFullName() + "Controller", flowController));
				
				urlMappings.put(pageFlow.getUri(), pageFlow.getFullName() + "Controller");
			}
			
		}
	}
}
