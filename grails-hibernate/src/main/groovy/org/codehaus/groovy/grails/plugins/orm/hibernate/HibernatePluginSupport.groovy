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
package org.codehaus.groovy.grails.plugins.orm.hibernate

import grails.artefact.Enhanced
import grails.util.GrailsNameUtils

import org.apache.commons.beanutils.PropertyUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.orm.hibernate.*
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.hibernate.EmptyInterceptor
import org.hibernate.FlushMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Environment
import org.hibernate.cfg.ImprovedNamingStrategy
import org.hibernate.proxy.HibernateProxy
import org.springframework.beans.SimpleTypeConverter
import org.springframework.beans.TypeMismatchException
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.dao.DataAccessException
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor
import org.springframework.orm.hibernate3.HibernateAccessor
import org.springframework.orm.hibernate3.HibernateCallback
import org.springframework.orm.hibernate3.HibernateTemplate
import org.springframework.orm.hibernate3.HibernateTransactionManager
import org.springframework.validation.Validator
import org.springframework.transaction.PlatformTransactionManager
import org.codehaus.groovy.grails.domain.GrailsDomainClassPersistentEntity

/**
 * Used by HibernateGrailsPlugin to implement the core parts of GORM.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class HibernatePluginSupport {

    static final Log LOG = LogFactory.getLog(this)

    static doWithSpring = {

        if (getSpringConfig().containsBean(ConstraintsEvaluator.BEAN_NAME)) {
            delegate."${ConstraintsEvaluator.BEAN_NAME}".constraintsEvaluatorClass = HibernateConstraintsEvaluator
        }

        def vendorToDialect = new Properties()
        def hibernateDialects = application.classLoader.getResource("hibernate-dialects.properties")
        if (hibernateDialects) {
            def p = new Properties()
            p.load(hibernateDialects.openStream())
            for (entry in p) {
                vendorToDialect[entry.value] = "org.hibernate.dialect.${entry.key}".toString()
            }
        }

        def datasourceNames = []
        if (getSpringConfig().containsBean('dataSource')) {
            datasourceNames << GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
        }

        for (name in application.config.keySet()) {
            if (name.startsWith('dataSource_')) {
                datasourceNames << name - 'dataSource_'
            }
        }

        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT,
            new PersistentConstraintFactory(getSpringConfig().getUnrefreshedApplicationContext(),
                UniqueConstraint))

        proxyHandler(HibernateProxyHandler)

        eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)

        nativeJdbcExtractor(CommonsDbcpNativeJdbcExtractor)

        hibernateEventListeners(HibernateEventListeners)

        persistenceInterceptor(AggregatePersistenceContextInterceptor) {
            dataSourceNames = datasourceNames
        }

        for (String datasourceName in datasourceNames) {
            LOG.debug "processing DataSource $datasourceName"
            boolean isDefault = datasourceName == GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
            String suffix = isDefault ? '' : '_' + datasourceName
            String prefix = isDefault ? '' : datasourceName + '_'

            for (GrailsDomainClass dc in application.domainClasses) {

                if (!dc.abstract && dc.getMappingStrategy() == GrailsDomainClass.GORM && GrailsHibernateUtil.usesDatasource(dc, datasourceName)) {
                    "${dc.fullName}Validator$suffix"(HibernateDomainClassValidator) {
                        messageSource = ref("messageSource")
                        domainClass = ref("${dc.fullName}DomainClass")
                        grailsApplication = ref("grailsApplication", true)
                        sessionFactory = ref("sessionFactory$suffix")
                    }
                }
            }

            def ds = application.config["dataSource$suffix"]
            if (isDefault) {
                BeanDefinition externalDefinition = checkExternalBeans(application)
                if (externalDefinition && !ds) {
                    ds = new ConfigObject()
                    application.config.dataSource = ds
                }
            }

            def hibConfig = application.config["hibernate$suffix"] ?: application.config.hibernate

            def hibConfigClass = ds?.configClass
            def hibProps = [:]

            if (ds.loggingSql || ds.logSql) {
                hibProps."hibernate.show_sql" = "true"
            }
            if (ds.formatSql) {
                hibProps."hibernate.format_sql" = "true"
            }

            if (ds.dialect) {
                if (ds.dialect instanceof Class) {
                    hibProps."hibernate.dialect" = ds.dialect.name
                }
                else {
                    hibProps."hibernate.dialect" = ds.dialect.toString()
                }
            }
            else {
                "dialectDetector$suffix"(HibernateDialectDetectorFactoryBean) {
                    dataSource = ref("dataSource$suffix")
                    vendorNameDialectMappings = vendorToDialect
                }
                hibProps."hibernate.dialect" = ref("dialectDetector$suffix")
            }

            hibProps."hibernate.hbm2ddl.auto" = ds.dbCreate ?: ''

            LOG.info "Set db generation strategy to '${hibProps.'hibernate.hbm2ddl.auto'}' for datasource $datasourceName"

            if (hibConfig) {
                def cacheProvider = hibConfig.cache?.provider_class
                if (cacheProvider) {
                    if (cacheProvider.contains('OSCacheProvider')) {
                        try {
                            def cacheClass = getClass().classLoader.loadClass(cacheProvider)
                        }
                        catch (Throwable t) {
                            hibConfig.cache.region.factory_class='net.sf.ehcache.hibernate.EhCacheRegionFactory'
                            log.error """WARNING: Your cache provider is set to '${cacheProvider}' in DataSource.groovy, however the class for this provider cannot be found.
    Using Grails' default cache region factory: 'net.sf.ehcache.hibernate.EhCacheRegionFactory'"""
                        }
                    } else if (!(hibConfig.cache.useCacheProvider) && (cacheProvider=='org.hibernate.cache.EhCacheProvider' || cacheProvider=='net.sf.ehcache.hibernate.EhCacheProvider')) {
                        hibConfig.cache.region.factory_class='net.sf.ehcache.hibernate.EhCacheRegionFactory'
                        hibConfig.cache.remove('provider_class')
                        if (hibConfig.cache.provider_configuration_file_resource_path) {
                            hibProps.'net.sf.ehcache.configurationResourceName' = hibConfig.cache.provider_configuration_file_resource_path
                            hibConfig.cache.remove('provider_configuration_file_resource_path')
                        }
                    }
                }

                def namingStrategy = hibConfig.naming_strategy ?: ImprovedNamingStrategy
                try {
                    GrailsDomainBinder.configureNamingStrategy datasourceName, namingStrategy
                }
                catch (Throwable t) {
                    log.error """WARNING: You've configured a custom Hibernate naming strategy '$namingStrategy' in DataSource.groovy, however the class cannot be found.
Using Grails' default naming strategy: '${ImprovedNamingStrategy.name}'"""
                }
				
				// allow adding hibernate properties that don't start with "hibernate."
				if(hibConfig.get('properties') instanceof ConfigObject) {
					def hibernateProperties = hibConfig.remove('properties')
					hibProps.putAll(hibernateProperties.flatten().toProperties())
				}

                hibProps.putAll(hibConfig.flatten().toProperties('hibernate'))

				// move net.sf.ehcache.configurationResourceName to "top level"	if it exists			
				if(hibProps.'hibernate.net.sf.ehcache.configurationResourceName') {
					hibProps.'net.sf.ehcache.configurationResourceName' = hibProps.remove('hibernate.net.sf.ehcache.configurationResourceName')
				}
            }

            "hibernateProperties$suffix"(PropertiesFactoryBean) { bean ->
                bean.scope = "prototype"
                properties = hibProps
            }

            "lobHandlerDetector$suffix"(SpringLobHandlerDetectorFactoryBean) {
                dataSource = ref("dataSource$suffix")
                pooledConnection =  ds.pooled ?: false
                nativeJdbcExtractor = ref("nativeJdbcExtractor")
            }

            "entityInterceptor$suffix"(EmptyInterceptor)

            "abstractSessionFactoryBeanConfig$suffix" {
                dataSource = ref("dataSource$suffix")
                dataSourceName = datasourceName
                sessionFactoryBeanName = "sessionFactory$suffix"

                List hibConfigLocations = []
                if (application.classLoader.getResource(prefix + 'hibernate.cfg.xml')) {
                   hibConfigLocations << 'classpath:' + prefix + 'hibernate.cfg.xml'
                }
                def explicitLocations = hibConfig?.config?.location
                if (explicitLocations) {
                    if (explicitLocations instanceof Collection) {
                        hibConfigLocations.addAll(explicitLocations.collect { it.toString() })
                    }
                    else {
                        hibConfigLocations << hibConfig.config.location.toString()
                    }
                }
                configLocations = hibConfigLocations

                if (hibConfigClass) {
                    configClass = ds.configClass
                }

                hibernateProperties = ref("hibernateProperties$suffix")

                grailsApplication = ref("grailsApplication", true)

                lobHandler = ref("lobHandlerDetector$suffix")

                entityInterceptor = ref("entityInterceptor$suffix")

                eventListeners = ['flush':       new PatchedDefaultFlushEventListener(),
                                  'save':        eventTriggeringInterceptor,
                                  'save-update': eventTriggeringInterceptor,
                                  'pre-load':    eventTriggeringInterceptor,
                                  'post-load':   eventTriggeringInterceptor,
                                  'pre-insert':  eventTriggeringInterceptor,
                                  'post-insert': eventTriggeringInterceptor,
                                  'pre-update':  eventTriggeringInterceptor,
                                  'post-update': eventTriggeringInterceptor,
                                  'pre-delete':  eventTriggeringInterceptor,
                                  'post-delete': eventTriggeringInterceptor]

                hibernateEventListeners = ref('hibernateEventListeners')
            }

            if (grails.util.Environment.current.isReloadEnabled()) {
                "${SessionFactoryHolder.BEAN_ID}$suffix"(SessionFactoryHolder)
            }
            "sessionFactory$suffix"(ConfigurableLocalSessionFactoryBean) { bean ->
                bean.parent = 'abstractSessionFactoryBeanConfig' + suffix
            }

            "transactionManager$suffix"(GrailsHibernateTransactionManager) {
                sessionFactory = ref("sessionFactory$suffix")
            }

            if (manager?.hasGrailsPlugin("controllers")) {
                "flushingRedirectEventListener$suffix"(FlushOnRedirectEventListener, ref("sessionFactory$suffix"))

                "openSessionInViewInterceptor$suffix"(GrailsOpenSessionInViewInterceptor) {

                    if (Boolean.TRUE.equals(ds.readOnly)) {
                        flushMode = HibernateAccessor.FLUSH_NEVER
                    }
                    else if (hibConfig.flush.mode instanceof String) {
                        switch(hibConfig.flush.mode) {
                            case "manual": flushMode = HibernateAccessor.FLUSH_NEVER;  break
                            case "always": flushMode = HibernateAccessor.FLUSH_ALWAYS; break
                            case "commit": flushMode = HibernateAccessor.FLUSH_COMMIT; break
                            default:       flushMode = HibernateAccessor.FLUSH_AUTO
                        }
                    }
                    else {
                        flushMode = HibernateAccessor.FLUSH_AUTO
                    }
                    sessionFactory = ref("sessionFactory$suffix")
                }

                if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                    controllerHandlerMappings.interceptors << ref("openSessionInViewInterceptor$suffix")
                }
                if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                    if (annotationHandlerMapping.interceptors) {
                        annotationHandlerMapping.interceptors << ref("openSessionInViewInterceptor$suffix")
                    }
                    else {
                        annotationHandlerMapping.interceptors = [ref("openSessionInViewInterceptor$suffix")]
                    }
                }
            }
        }
    }

    static final onChange = { event ->

        def datasourceNames = [GrailsDomainClassProperty.DEFAULT_DATA_SOURCE]
        for (name in application.config.keySet()) {
            if (name.startsWith('dataSource_')) {
                datasourceNames << name - 'dataSource_'
            }
        }

        def beans = beans {
            for (String datasourceName in datasourceNames) {
                LOG.debug "processing DataSource $datasourceName"
                boolean isDefault = datasourceName == GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
                String suffix = isDefault ? '' : '_' + datasourceName
                String prefix = isDefault ? '' : datasourceName + '_'

                "${SessionFactoryHolder.BEAN_ID}$suffix"(SessionFactoryHolder) {
                   sessionFactory = bean(ConfigurableLocalSessionFactoryBean) { bean ->
                       bean.parent = ref("abstractSessionFactoryBeanConfig$suffix")
                       proxyIfReloadEnabled = false
                   }
                }
                for (GrailsDomainClass dc in application.domainClasses) {
                    if (!dc.abstract && GrailsHibernateUtil.usesDatasource(dc, datasourceName)) {
                        "${dc.fullName}Validator$suffix"(HibernateDomainClassValidator) {
                            messageSource = ref("messageSource")
                            domainClass = ref("${dc.fullName}DomainClass")
                            sessionFactory = ref("sessionFactory$suffix")
                            grailsApplication = ref("grailsApplication", true)
                        }
                    }
                }
            }
        }

        ApplicationContext ctx = event.ctx
        beans.registerBeans(ctx)
        if (event.source instanceof Class) {
            def mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
            def entity = mappingContext.addPersistentEntity(event.source)

            def dc = application.getDomainClass(event.source.name)
            for (String datasourceName in datasourceNames) {
                if (GrailsHibernateUtil.usesDatasource(dc, datasourceName)) {
                    boolean isDefault = datasourceName == GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
                    String suffix = isDefault ? '' : '_' + datasourceName
                    final validator = ctx.getBean("${entity.name}Validator$suffix", Validator)
                    mappingContext.addEntityValidator(entity, validator)
                    if(isDefault) {
                        GrailsDomainClass domainClass = application.getDomainClass(event.source.name)
                        domainClass.setValidator(validator)
                    }
                }
            }
        }

        enhanceSessionFactories(ctx, event.application)

        // Verify that the reload worked by executing a GORM method. If it failed try again
        try {
            event.source.count()
        } catch (MissingMethodException mme) {

           MappingContext mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
           final sessionFactory = ctx.getBean("sessionFactory", SessionFactory)
           final txMgr = ctx.getBean("transactionManager", HibernateTransactionManager)
           final datastore = new HibernateDatastore(mappingContext, sessionFactory, ctx, application.config)
           def enhancer = new HibernateGormEnhancer(datastore, txMgr, application)
           def entity = mappingContext.getPersistentEntity(event.source.name)
           if (entity.javaClass.getAnnotation(Enhanced) == null) {
              enhancer.enhance entity
           }
           else {
              enhancer.enhance entity, true
           }

        }
    }

    static final doWithDynamicMethods = { ApplicationContext ctx ->
        def grailsApplication = application
        enhanceSessionFactories(ctx, grailsApplication)
    }

    static void enhanceSessionFactories(ApplicationContext ctx, grailsApplication) {

        Map<SessionFactory, HibernateDatastore> datastores = [:]

        for (entry in ctx.getBeansOfType(SessionFactory)) {
            SessionFactory sessionFactory = entry.value
            String beanName = entry.key
            String suffix = beanName - 'sessionFactory'
            enhanceSessionFactory sessionFactory, grailsApplication, ctx, suffix, datastores
        }

        ctx.eventTriggeringInterceptor.datastores = datastores
    }

    static void enhanceProxyClass(Class proxyClass) {
        def mc = proxyClass.metaClass
        if (mc.pickMethod('grailsEnhanced', GrailsHibernateUtil.EMPTY_CLASS_ARRAY)) {
            return
        }

        // hasProperty
        def originalHasProperty = mc.getMetaMethod("hasProperty", String)
        mc.hasProperty = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).hasProperty(name)
            }
            return originalHasProperty.invoke(delegate, name)
        }
        // respondsTo
        def originalRespondsTo = mc.getMetaMethod("respondsTo", String)
        mc.respondsTo = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).respondsTo(name)
            }
            return originalRespondsTo.invoke(delegate, name)
        }
        def originalRespondsToTwoArgs = mc.getMetaMethod("respondsTo", String, Object[])
        mc.respondsTo = { String name, Object[] args ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).respondsTo(name, args)
            }
            return originalRespondsToTwoArgs.invoke(delegate, name, args)
        }
        // getter
        mc.propertyMissing = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate)."$name"
            }
            throw new MissingPropertyException(name, delegate.getClass())
        }

        // setter
        mc.propertyMissing = { String name, val ->
            if (delegate instanceof HibernateProxy) {
                GrailsHibernateUtil.unwrapProxy(delegate)."$name" = val
            }
            else {
                throw new MissingPropertyException(name, delegate.getClass())
            }
        }

        mc.methodMissing = { String name, args ->
            if (delegate instanceof HibernateProxy) {
                def obj = GrailsHibernateUtil.unwrapProxy(delegate)
                return obj."$name"(*args)
            }
            throw new MissingPropertyException(name, delegate.getClass())
        }

        mc.grailsEnhanced = { true }
    }

    static void enhanceProxy(HibernateProxy proxy) {
        proxy.metaClass = GroovySystem.metaClassRegistry.getMetaClass(proxy.getClass())
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application,
              ApplicationContext ctx) {
        enhanceSessionFactory(sessionFactory, application, ctx, '', [:])
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application,
            ApplicationContext ctx, String suffix, Map<SessionFactory, HibernateDatastore> datastores) {

        MappingContext mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
        PlatformTransactionManager transactionManager = ctx.getBean("transactionManager$suffix", PlatformTransactionManager)
        final datastore = new HibernateDatastore(mappingContext, sessionFactory, ctx, application.config)
        datastores[sessionFactory] = datastore
        String datasourceName = suffix ? suffix[1..-1] : GrailsDomainClassProperty.DEFAULT_DATA_SOURCE

        HibernateGormEnhancer enhancer = new HibernateGormEnhancer(datastore, transactionManager, application)
        for (PersistentEntity entity in mappingContext.getPersistentEntities()) {
            GrailsDomainClass dc = application.getDomainClass(entity.javaClass.name)
            if (dc.getMappingStrategy() != GrailsDomainClass.GORM || !GrailsHibernateUtil.usesDatasource(dc, datasourceName)) {
                continue
            }

            if (!datasourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE)) {
                LOG.debug "Registering namespace methods for $dc.clazz.name in DataSource '$datasourceName'"
                registerNamespaceMethods dc, datastore, datasourceName, transactionManager, application
            }

            if (datasourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE) ||
                    datasourceName.equals(GrailsHibernateUtil.getDefaultDataSource(dc))) {
                if (entity.javaClass.getAnnotation(Enhanced) == null) {
                    enhancer.enhance entity
                }
                else {
                    enhancer.enhance entity, true
                }
            }
        }
    }

    private static void registerNamespaceMethods(GrailsDomainClass dc, HibernateDatastore datastore,
            String datasourceName, PlatformTransactionManager  transactionManager,
            GrailsApplication application) {

        String getter = GrailsNameUtils.getGetterName(datasourceName)
        if (dc.metaClass.methods.any { it.name == getter && it.parameterTypes.size() == 0 }) {
            LOG.warn "The $dc.clazz.name domain class has a method '$getter' - unable to add namespaced methods for datasource '$datasourceName'"
            return
        }

        def classLoader = application.classLoader

        def finders = HibernateGormEnhancer.createPersistentMethods(application, classLoader, datastore)
        def staticApi = new HibernateGormStaticApi(dc.clazz, datastore, finders, classLoader, transactionManager)
        dc.metaClass.static."$getter" = { -> staticApi }

        def validateApi = new HibernateGormValidationApi(dc.clazz, datastore, classLoader)
        def instanceApi = new HibernateGormInstanceApi(dc.clazz, datastore, classLoader)
        dc.metaClass."$getter" = { -> new InstanceProxy(delegate, instanceApi, validateApi) }
    }

    static final LAZY_PROPERTY_HANDLER = { String propertyName ->
        def propertyValue = PropertyUtils.getProperty(delegate, propertyName)
        if (propertyValue instanceof HibernateProxy) {
            return GrailsHibernateUtil.unwrapProxy(propertyValue)
        }
        return propertyValue
    }

    /**
     * This method overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell.
     */
    static void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty property) {
        String propertyName = property.name
        def getterName = GrailsClassUtils.getGetterName(propertyName)
        def setterName = GrailsClassUtils.getSetterName(propertyName)
        domainClass.metaClass."${getterName}" = LAZY_PROPERTY_HANDLER.clone().curry(propertyName)
        domainClass.metaClass."${setterName}" = { PropertyUtils.setProperty(delegate, propertyName, it) }

        for (GrailsDomainClass sub in domainClass.subClasses) {
            handleLazyProxy(sub, sub.getPropertyByName(property.name))
        }
    }

    static Map filterQueryArgumentMap(Map query) {
        def queryArgs = [:]
        for (entry in query) {
            if (entry.value instanceof CharSequence) {
                queryArgs[entry.key] = entry.value.toString()
            }
            else {
                queryArgs[entry.key] = entry.value
            }
        }
        return queryArgs
    }

    private static List<String> removeNullNames(Map query) {
        List<String> nullNames = []
        Set<String> allNames = new HashSet(query.keySet())
        for (String name in allNames) {
            if (query[name] == null) {
                query.remove name
                nullNames << name
            }
        }
        nullNames
    }

    /**
     * Session should no longer be flushed after a data access exception occurs (such a constriant violation)
     */
    static void handleDataAccessException(HibernateTemplate template, DataAccessException e) {
        try {
            template.execute({Session session ->
                session.setFlushMode(FlushMode.MANUAL)
            } as HibernateCallback)
        }
        finally {
            throw e
        }
    }

    /**
     * Converts an id value to the appropriate type for a domain class.
     *
     * @param grailsDomainClass a GrailsDomainClass
     * @param idValue an value to be converted
     * @return the idValue parameter converted to the type that grailsDomainClass expects
     * its identifiers to be
     */
    static convertValueToIdentifierType(grailsDomainClass, idValue) {
        convertToType(idValue, grailsDomainClass.identifier.type)
    }

    private static convertToType(value, targetType) {
        SimpleTypeConverter typeConverter = new SimpleTypeConverter()

        if (value != null && !targetType.isAssignableFrom(value.getClass())) {
            if (value instanceof Number && Long.equals(targetType)) {
                value = value.toLong()
            }
            else {
                try {
                    value = typeConverter.convertIfNecessary(value, targetType)
                } catch (TypeMismatchException e) {
                    // ignore
                }
            }
        }
        return value
    }

    static shouldFlush(GrailsApplication application, Map map = [:]) {
        def shouldFlush

        if (map?.containsKey('flush')) {
            shouldFlush = Boolean.TRUE == map.flush
        } else {
            def config = application.flatConfig
            shouldFlush = Boolean.TRUE == config.get('grails.gorm.autoFlush')
        }
        return shouldFlush
    }

    private static checkExternalBeans(GrailsApplication application) {
        ApplicationContext parent = application.parentContext
        try {
            def resourcesXml = parent?.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML)
            if (resourcesXml && resourcesXml.exists()) {
                def xmlBeans = new XmlBeanFactory(resourcesXml)
                if (xmlBeans.containsBean("dataSource")) {
                    LOG.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_XML}")
                    return xmlBeans.getMergedBeanDefinition("dataSource")
                }
            }
        } catch (FileNotFoundException fnfe) {
            // that's ok external resources file not required
        }

        // Check resources.groovy
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(parent,application.classLoader)
        GrailsRuntimeConfigurator.loadExternalSpringConfig(springConfig, application)
        if (springConfig.containsBean("dataSource")) {
            LOG.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY}")
            return springConfig.getBeanDefinition("dataSource")
        }
        return null
    }
}

class InstanceProxy {
    private instance
    private HibernateGormValidationApi validateApi
    private HibernateGormInstanceApi instanceApi

    private final Set<String> validateMethods

    InstanceProxy(instance, HibernateGormInstanceApi instanceApi, HibernateGormValidationApi validateApi) {
        this.instance = instance
        this.instanceApi = instanceApi
        this.validateApi = validateApi
        validateMethods = validateApi.methods*.name
        validateMethods.remove 'getValidator'
        validateMethods.remove 'setValidator'
        validateMethods.remove 'getBeforeValidateHelper'
        validateMethods.remove 'setBeforeValidateHelper'
        validateMethods.remove 'getValidateMethod'
        validateMethods.remove 'setValidateMethod'
    }

    def invokeMethod(String name, args) {
        if (validateMethods.contains(name)) {
            validateApi."$name"(instance, *args)
        }
        else {
            instanceApi."$name"(instance, *args)
        }
    }

    void setProperty(String name, val) {
        instanceApi."$name" = val
    }

    def getProperty(String name) {
        instanceApi."$name"
    }

    void putAt(String name, val) {
        instanceApi."$name" = val
    }

    def getAt(String name) {
        instanceApi."$name"
    }
}
