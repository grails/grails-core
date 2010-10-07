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

import grails.orm.HibernateCriteriaBuilder
import grails.util.GrailsUtil

import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.beanutils.PropertyUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.metaclass.StaticMethodInvocation
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.exceptions.GrailsDomainException
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.codehaus.groovy.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty

import org.hibernate.Criteria
import org.hibernate.EmptyInterceptor
import org.hibernate.FlushMode
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Environment
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.proxy.HibernateProxy

import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.SimpleTypeConverter
import org.springframework.beans.TypeMismatchException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor
import org.springframework.orm.hibernate3.*
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.validation.Validator

/**
 * Used by HibernateGrailsPlugin to implement the core parts of GORM.
 *
 * @author Graeme Rocher
 * @since 1.1
 *
 * Created: Jan 13, 2009
 */
class HibernatePluginSupport {

    static final Log LOG = LogFactory.getLog(HibernatePluginSupport)

    static hibProps = [(Environment.SESSION_FACTORY_NAME): ConfigurableLocalSessionFactoryBean.name]
    static hibConfigClass

    static doWithSpring = {
        def factory = new PersistentConstraintFactory(getSpringConfig().getUnrefreshedApplicationContext(), UniqueConstraint)
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, factory)

        for (GrailsDomainClass dc in application.domainClasses) {
            "${dc.fullName}Validator"(HibernateDomainClassValidator) {
                messageSource = ref("messageSource")
                domainClass = ref("${dc.fullName}DomainClass")
                grailsApplication = ref("grailsApplication", true)
            }
        }
        def vendorToDialect = new Properties()
        def hibernateDialects = application.classLoader.getResource("hibernate-dialects.properties")
        if (hibernateDialects) {
            def p = new Properties()
            p.load(hibernateDialects.openStream())
            p.each {entry ->
                vendorToDialect[entry.value] = "org.hibernate.dialect.${entry.key}".toString()
            }
        }
        def ds = application.config.dataSource
        def hibConfig = application.config.hibernate

        BeanDefinition externalDefinition = HibernatePluginSupport.checkExternalBeans(application)
        if (externalDefinition && !ds) {
            ds = new ConfigObject()
            application.config.dataSource = ds
        }
        if (ds || application.domainClasses.size() > 0) {
            hibConfigClass = ds?.configClass

            if (ds && ds.loggingSql || ds && ds.logSql) {
                hibProps."hibernate.show_sql" = "true"
                hibProps."hibernate.format_sql" = "true"
            }
            if (ds && ds.dialect) {
                if (ds.dialect instanceof Class) {
                    hibProps."hibernate.dialect" = ds.dialect.name
                }
                else {
                    hibProps."hibernate.dialect" = ds.dialect.toString()
                }
            }
            else {
                dialectDetector(HibernateDialectDetectorFactoryBean) {
                    dataSource = ref("dataSource")
                    vendorNameDialectMappings = vendorToDialect
                }
                hibProps."hibernate.dialect" = dialectDetector
            }
            if (!ds) {
                hibProps."hibernate.hbm2ddl.auto" = "create-drop"
            }
            else if (ds.dbCreate) {
                hibProps."hibernate.hbm2ddl.auto" = ds.dbCreate
            }
            LOG.info "Set db generation strategy to '${hibProps.'hibernate.hbm2ddl.auto'}'"

            if (hibConfig) {
                def cacheProvider = hibConfig.cache.provider_class ?: 'net.sf.ehcache.hibernate.EhCacheProvider'
                if (cacheProvider.contains('OSCacheProvider')) {
                    try {
                        def cacheClass = getClass().classLoader.loadClass(cacheProvider)
                    } catch (Throwable t) {
                        hibConfig.cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
                        log.error """WARNING: Your cache provider is set to '${cacheProvider}' in DataSource.groovy, however the class for this provider cannot be found.
Using Grails' default cache provider: 'net.sf.ehcache.hibernate.EhCacheProvider'"""
                    }
                }

                def namingStrategy = hibConfig.naming_strategy
                if (namingStrategy) {
                    try {
                        GrailsDomainBinder.configureNamingStrategy namingStrategy
                    }
                    catch (Throwable t) {
                        log.error """WARNING: You've configured a custom Hibernate naming strategy '$namingStrategy' in DataSource.groovy, however the class cannot be found.
Using Grails' default naming strategy: '${GrailsDomainBinder.namingStrategy.getClass().name}'"""
                    }
                }

                hibProps.putAll(hibConfig.flatten().toProperties('hibernate'))
            }

            hibernateProperties(PropertiesFactoryBean) { bean ->
                bean.scope = "prototype"
                properties = hibProps
            }
            nativeJdbcExtractor(CommonsDbcpNativeJdbcExtractor)
            lobHandlerDetector(SpringLobHandlerDetectorFactoryBean) {
                dataSource = dataSource
                pooledConnection =  ds && ds.pooled ?: false
                nativeJdbcExtractor = ref("nativeJdbcExtractor")
            }
            proxyHandler(HibernateProxyHandler)
            eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            hibernateEventListeners(HibernateEventListeners)
            entityInterceptor(EmptyInterceptor)
            sessionFactory(ConfigurableLocalSessionFactoryBean) {
                dataSource = dataSource
                List hibConfigLocations = []
                if (application.classLoader.getResource("hibernate.cfg.xml")) {
                    hibConfigLocations << "classpath:hibernate.cfg.xml"
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
                hibernateProperties = hibernateProperties
                grailsApplication = ref("grailsApplication", true)
                lobHandler = lobHandlerDetector
                entityInterceptor = entityInterceptor
                eventListeners = ['flush': new PatchedDefaultFlushEventListener(),
                                  'pre-load':eventTriggeringInterceptor,
                                  'post-load':eventTriggeringInterceptor,
                                  'save':eventTriggeringInterceptor,
                                  'save-update':eventTriggeringInterceptor,
                                  'post-insert':eventTriggeringInterceptor,
                                  'pre-update':eventTriggeringInterceptor,
                                  'post-update':eventTriggeringInterceptor,
                                  'pre-delete':eventTriggeringInterceptor,
                                  'post-delete':eventTriggeringInterceptor]
                hibernateEventListeners = hibernateEventListeners
            }

            transactionManager(GrailsHibernateTransactionManager) {
                sessionFactory = sessionFactory
            }
            persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                sessionFactory = sessionFactory
            }

            if (manager?.hasGrailsPlugin("controllers")) {
                flushingRedirectEventListener(FlushOnRedirectEventListener, sessionFactory)
                openSessionInViewInterceptor(GrailsOpenSessionInViewInterceptor) {

                    if (hibConfig.flush.mode instanceof String) {
                        switch(hibConfig.flush.mode) {
                            case "manual":
                                flushMode = HibernateAccessor.FLUSH_NEVER; break
                            case "always":
                                flushMode = HibernateAccessor.FLUSH_ALWAYS; break
                            case "commit":
                                flushMode = HibernateAccessor.FLUSH_COMMIT; break
                            default:
                                flushMode = HibernateAccessor.FLUSH_AUTO
                        }
                    }
                    else {
                        flushMode = HibernateAccessor.FLUSH_AUTO
                    }
                    sessionFactory = sessionFactory
                }
                if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                    controllerHandlerMappings.interceptors << openSessionInViewInterceptor
                }
                if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                    if (annotationHandlerMapping.interceptors) {
                        annotationHandlerMapping.interceptors << openSessionInViewInterceptor
                    }
                    else {
                        annotationHandlerMapping.interceptors = [openSessionInViewInterceptor]
                    }
                }
            }
        }
    }

    static final doWithDynamicMethods = {ApplicationContext ctx ->
        for (entry in ctx.getBeansOfType(SessionFactory)) {
            SessionFactory sessionFactory = entry.value
            enhanceSessionFactory(sessionFactory, application, ctx)
        }
    }

    static void enhanceProxyClass (Class proxyClass) {
        def mc = proxyClass.metaClass
        if (!mc.pickMethod('grailsEnhanced', GrailsHibernateUtil.EMPTY_CLASS_ARRAY)) {
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
                throw new MissingPropertyException(name, delegate.class)
            }

            // setter
            mc.propertyMissing = { String name, val ->
                if (delegate instanceof HibernateProxy) {
                    GrailsHibernateUtil.unwrapProxy(delegate)."$name" = val
                }
                else {
                    throw new MissingPropertyException(name, delegate.class)
                }
            }

            mc.methodMissing = { String name, args ->
                if (delegate instanceof HibernateProxy) {
                    def obj = GrailsHibernateUtil.unwrapProxy(delegate)
                    return obj."$name"(*args)
                }
                throw new MissingPropertyException(name, delegate.class)
            }

            mc.grailsEnhanced = { true }
        }
    }

    static void enhanceProxy(HibernateProxy proxy) {
        MetaClass emc = GroovySystem.metaClassRegistry.getMetaClass(proxy.getClass())
        proxy.metaClass = emc
    }

    private static DOMAIN_INITIALIZERS = new ConcurrentHashMap()
    static initializeDomain(Class c) {
        synchronized(c) {
             // enhance domain class only once, initializer is removed after calling
             DOMAIN_INITIALIZERS.remove(c)?.call()
        }
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application, ApplicationContext ctx) {
        // we're going to configure Grails to lazily initialise the dynamic methods on domain classes to avoid
        // the overhead of doing so at start-up time
        def lazyInit = {GrailsDomainClass dc ->
            registerDynamicMethods(dc, application, ctx, sessionFactory)
            for (subClass in dc.subClasses) {
                registerDynamicMethods(subClass, application, ctx, sessionFactory)
            }
            MetaClass emc = GroovySystem.metaClassRegistry.getMetaClass(dc.clazz)
        }
        def initializeDomainOnceClosure = {GrailsDomainClass dc ->
            initializeDomain(dc.clazz)
        }

        for (GrailsDomainClass dc in application.domainClasses) {
            //    registerDynamicMethods(dc, application, ctx)
            MetaClass mc = dc.metaClass
            def initDomainClass = lazyInit.curry(dc)
            DOMAIN_INITIALIZERS[dc.clazz] = initDomainClass
            def initDomainClassOnce = initializeDomainOnceClosure.curry(dc)
            // these need to be eagerly initialised here, otherwise Groovy's findAll from the DGM is called
            def findAllMethod = new FindAllPersistentMethod(sessionFactory, application.classLoader)
            mc.static.findAll = {->
                findAllMethod.invoke(mc.javaClass, "findAll", [] as Object[])
            }
            mc.static.findAll = {Object example -> findAllMethod.invoke(mc.javaClass, "findAll", [example] as Object[])}
            mc.static.findAll = {Object example, Map args -> findAllMethod.invoke(mc.javaClass, "findAll", [example, args] as Object[])}

            mc.methodMissing = { String name, args ->
                initDomainClassOnce()
                mc.invokeMethod(delegate, name, args)
            }
            mc.static.methodMissing = {String name, args ->
                initDomainClassOnce()
                def result
                if (delegate instanceof Class) {
                    result = mc.invokeStaticMethod(delegate, name, args)
                }
                else {
                    result = mc.invokeMethod(delegate, name, args)
                }
                result
            }
            addValidationMethods(dc, application, ctx, sessionFactory)
            addNamedQuerySupport(dc, application, ctx)
        }
    }

    static final LAZY_PROPERTY_HANDLER = { String propertyName ->
      def propertyValue = PropertyUtils.getProperty(delegate, propertyName)
      if (propertyValue instanceof HibernateProxy) {
          return GrailsHibernateUtil.unwrapProxy(propertyValue)
      }
      return propertyValue
    }

    /**
     * This method overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell
     */
    static void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty property) {
        String propertyName = property.name
        def getterName = GrailsClassUtils.getGetterName(propertyName)
        def setterName = GrailsClassUtils.getSetterName(propertyName)
        domainClass.metaClass."${getterName}" = LAZY_PROPERTY_HANDLER.curry(propertyName)
        domainClass.metaClass."${setterName}" = { PropertyUtils.setProperty(delegate, propertyName, it)  }

        for (GrailsDomainClass sub in domainClass.subClasses) {
            handleLazyProxy(sub, sub.getPropertyByName(property.name))
        }
    }

    private static registerDynamicMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx, SessionFactory sessionFactory) {
        dc.metaClass.methodMissing = { String name, args ->
            throw new MissingMethodException(name, dc.clazz, args, true)
        }
        addBasicPersistenceMethods(dc, application, ctx)
        addQueryMethods(dc, application, ctx)
        addTransactionalMethods(dc, application, ctx)
        addValidationMethods(dc, application, ctx,sessionFactory)
        addDynamicFinderSupport(dc, application, ctx)
    }

    private static addNamedQuerySupport(dc, application, ctx) {
        try {
            def property = GrailsClassUtils.getStaticPropertyValue(dc.clazz, GrailsDomainClassProperty.NAMED_QUERIES)
            if (property instanceof Closure) {
                def builder = new HibernateNamedQueriesBuilder(dc, application, ctx)
                builder.evaluate(property)
            }
        } catch (Exception e) {
            GrailsUtil.deepSanitize(e)
            throw new GrailsDomainException("Error evaluating named queries block for domain [${dc.fullName}]:  " + e.message, e)
        }
    }

    private static addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass
        ClassLoader classLoader = application.classLoader
        def sessionFactory = ctx.getBean('sessionFactory')

        def dynamicMethods = [new FindAllByPersistentMethod(application, sessionFactory, classLoader),
                              new FindAllByBooleanPropertyPersistentMethod(application, sessionFactory, classLoader),
                              new FindByPersistentMethod(application, sessionFactory, classLoader),
                              new FindByBooleanPropertyPersistentMethod(application, sessionFactory, classLoader),
                              new CountByPersistentMethod(application, sessionFactory, classLoader),
                              new ListOrderByPersistentMethod(sessionFactory, classLoader)]

        // This is the code that deals with dynamic finders. It looks up a static method, if it exists it invokes it
        // otherwise it trys to match the method invocation to one of the dynamic methods. If it matches it will
        // register a new method with the ExpandoMetaClass so the next time it is invoked it doesn't have this overhead.
        mc.static.methodMissing = {String methodName, args ->
            def result = null
            StaticMethodInvocation method = dynamicMethods.find {it.isMethodMatch(methodName)}
            if (method) {
                // register the method invocation for next time
                synchronized(this) {
                    mc.static."$methodName" = {List varArgs ->
                        method.invoke(dc.clazz, methodName, varArgs)
                    }
                }
                result = method.invoke(dc.clazz, methodName, args)
            }
            else {
                throw new MissingMethodException(methodName, delegate, args)
            }
            result
        }
    }

    private static addValidationMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx, SessionFactory sessionFactory) {
        def metaClass = dc.metaClass

        Validator validator = ctx.containsBean("${dc.fullName}Validator") ? ctx.getBean("${dc.fullName}Validator") : null
        def validateMethod = new ValidatePersistentMethod(sessionFactory, application.classLoader, application,validator)
        metaClass.validate = {->
            try {
                validateMethod.invoke(delegate, "validate", [] as Object[])
            }
            catch (Throwable e) {
                e.printStackTrace()
                throw e
            }
        }
        metaClass.validate = {Map args ->
            validateMethod.invoke(delegate, "validate", [args] as Object[])
        }
        metaClass.validate = {Boolean b ->
            validateMethod.invoke(delegate, "validate", [b] as Object[])
        }
        metaClass.validate = {List args ->
            validateMethod.invoke(delegate, "validate", [args] as Object[])
        }
    }

    private static addTransactionalMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.getBean('sessionFactory')
        def Class domainClassType = dc.clazz

        ClassLoader classLoader = application.classLoader

        metaClass.static.withTransaction = {Closure callable ->
            new TransactionTemplate(ctx.getBean('transactionManager')).execute({status ->
                callable.call(status)
            } as TransactionCallback)
        }
        metaClass.static.withSession = { Closure callable ->
            new HibernateTemplate(sessionFactory).execute({ session ->
                callable(session)
            } as HibernateCallback)
        }
        metaClass.static.withNewSession = { Closure callable ->
            HibernateTemplate template = new HibernateTemplate(sessionFactory)
            SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
            Session previousSession = sessionHolder?.session
            try {
                template.alwaysUseNewSession = true
                template.execute({ Session session ->
                    if(sessionHolder == null) {
                        sessionHolder = new SessionHolder(session)
                        TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder)
                    }
                    else {
                        sessionHolder.addSession(session)
                    }

                    callable(session)
                } as HibernateCallback)
            }
            finally {
                if (previousSession) {
                    sessionHolder?.addSession(previousSession)
                }
            }
        }
        // Initiates a pessimistic lock on the row represented by the object using
        // the dbs "SELECT FOR UPDATE" mechanism
        def template = new HibernateTemplate(sessionFactory)
        metaClass.lock = {->
            template.lock(delegate, LockMode.UPGRADE)
        }
        metaClass.static.lock = { Serializable id ->
            def identityType = dc.identifier.type
            id = convertToType(id, identityType)

            template.get(delegate, id, LockMode.UPGRADE)
        }
    }

    private static addQueryMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.getBean('sessionFactory')
        def template = new HibernateTemplate(sessionFactory)
        Class domainClassType = dc.clazz

        ClassLoader classLoader = application.classLoader
        def findAllMethod = new FindAllPersistentMethod(sessionFactory, classLoader)
        metaClass.static.findAll = {String query ->
            findAllMethod.invoke(domainClassType, "findAll", [query] as Object[])
        }
        metaClass.static.findAll = {String query, Collection positionalParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams] as Object[])
        }
        metaClass.static.findAll = {String query, Collection positionalParams, Map paginateParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, paginateParams] as Object[])
        }
        metaClass.static.findAll = {String query, Map namedArgs ->
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs] as Object[])
        }
        metaClass.static.findAll = {String query, Map namedArgs, Map paginateParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, paginateParams] as Object[])
        }

        def findMethod = new FindPersistentMethod(sessionFactory, classLoader)
        metaClass.static.find = {String query ->
            findMethod.invoke(domainClassType, "find", [query] as Object[])
        }
        metaClass.static.find = {String query, Collection args ->
            findMethod.invoke(domainClassType, "find", [query, args] as Object[])
        }
        metaClass.static.find = {String query, Map namedArgs ->
            findMethod.invoke(domainClassType, "find", [query, namedArgs] as Object[])
        }
        metaClass.static.find = {String query, Map namedArgs, Map queryParams ->
            findMethod.invoke(domainClassType, "find", [query, namedArgs, queryParams] as Object[])
        }
        metaClass.static.find = {Object example ->
            findMethod.invoke(domainClassType, "find", [example] as Object[])
        }

        def executeQueryMethod = new ExecuteQueryPersistentMethod(sessionFactory, classLoader)
        metaClass.static.executeQuery = {String query ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query] as Object[])
        }
        metaClass.static.executeQuery = {String query, Collection positionalParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])
        }
        metaClass.static.executeQuery = {String query, Collection positionalParams, Map paginateParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, paginateParams] as Object[])
        }
        metaClass.static.executeQuery = {String query, Map namedParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams] as Object[])
        }
        metaClass.static.executeQuery = {String query, Map namedParams, Map paginateParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams, paginateParams] as Object[])
        }

        def executeUpdateMethod = new ExecuteUpdatePersistentMethod(sessionFactory, classLoader)
        metaClass.static.executeUpdate = { String query ->
            executeUpdateMethod.invoke(domainClassType, "executeUpdate", [query] as Object[])
        }
        metaClass.static.executeUpdate = { String query, Collection args ->
            executeUpdateMethod.invoke(domainClassType, "executeUpdate", [query, args] as Object[])
        }
        metaClass.static.executeUpdate = { String query, Map argMap ->
            executeUpdateMethod.invoke(domainClassType, "executeUpdate", [query, argMap] as Object[])
        }

        def listMethod = new ListPersistentMethod(sessionFactory, classLoader)
        metaClass.static.list = {-> listMethod.invoke(domainClassType, "list", [] as Object[])}
        metaClass.static.list = {Map args -> listMethod.invoke(domainClassType, "list", [args] as Object[])}
        metaClass.static.findWhere = {Map query ->
            if (!query) return null
            template.execute({Session session ->
                Map queryArgs = filterQueryArgumentMap(query)
                List<String> nullNames = removeNullNames(queryArgs)
                Criteria criteria = session.createCriteria(domainClassType)
                criteria.add(Restrictions.allEq(queryArgs))
                for (name in nullNames) {
                    criteria.add Restrictions.isNull(name)
                }
                criteria.setMaxResults(1)
                GrailsHibernateUtil.unwrapIfProxy(criteria.uniqueResult())
            } as HibernateCallback)
        }
        metaClass.static.findAllWhere = {Map query ->
            if (!query) return null
            template.execute({Session session ->
                Map queryArgs = filterQueryArgumentMap(query)
                List<String> nullNames = removeNullNames(queryArgs)
                Criteria criteria = session.createCriteria(domainClassType)
                criteria.add(Restrictions.allEq(queryArgs))
                for (name in nullNames) {
                    criteria.add Restrictions.isNull(name)
                }
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                criteria.list()
            } as HibernateCallback)
        }
        metaClass.static.getAll = {->
            template.execute({session ->
                session.createCriteria(domainClassType).list()
            } as HibernateCallback)
        }
        metaClass.static.getAll = {List ids ->
            template.execute({Session session ->
                def identityType = dc.identifier.type
                ids = ids.collect {convertToType(it, identityType)}
                def criteria = session.createCriteria(domainClassType)
                criteria.add(Restrictions.'in'(dc.identifier.name, ids))
                def results = criteria.list()
                def idsMap = [:]
                for (object in results) {
                    idsMap[object[dc.identifier.name]] = object
                }
                results.clear()
                for (id in ids) {
                    results << idsMap[id]
                }
                results
            } as HibernateCallback)

        }
        metaClass.static.exists = {id ->
            def identityType = dc.identifier.type
            id = convertToType(id, identityType)
            template.execute({ Session session ->
                session.createCriteria(dc.clazz)
                    .add(Restrictions.idEq(id))
                    .setProjection(Projections.rowCount())
                    .uniqueResult()
            } as HibernateCallback) == 1
        }

        metaClass.static.createCriteria = {-> new HibernateCriteriaBuilder(domainClassType, sessionFactory)}
        metaClass.static.withCriteria = {Closure callable ->
            new HibernateCriteriaBuilder(domainClassType, sessionFactory).invokeMethod("doCall", callable)
        }
        metaClass.static.withCriteria = {Map builderArgs, Closure callable ->
            def builder = new HibernateCriteriaBuilder(domainClassType, sessionFactory)
            def builderBean = new BeanWrapperImpl(builder)
            for (entry in builderArgs) {
                if (builderBean.isWritableProperty(entry.key)) {
                    builderBean.setPropertyValue(entry.key, entry.value)
                }
            }
            builder.invokeMethod("doCall", callable)
        }

        // TODO: deprecated methods planned for removing from further releases

        def deprecated = {methodSignature ->
            GrailsUtil.deprecated("${methodSignature} domain class dynamic method is deprecated since 0.6. Check out docs at: http://grails.org/DomainClass+Dynamic+Methods")
        }
        metaClass.static.findAll = {String query, Integer max ->
            deprecated("findAll(String query, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, max] as Object[])
        }
        metaClass.static.findAll = {String query, Integer max, Integer offset ->
            deprecated("findAll(String query, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, max, offset] as Object[])
        }

        metaClass.static.findAll = {String query, Collection positionalParams, Integer max ->
            deprecated("findAll(String query, Collection positionalParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.static.findAll = {String query, Collection positionalParams, Integer max, Integer offset ->
            deprecated("findAll(String query, Collection positionalParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max, offset] as Object[])
        }
        metaClass.static.findAll = {String query, Object[] positionalParams ->
            deprecated("findAll(String query, Object[] positionalParams)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams] as Object[])
        }
        metaClass.static.findAll = {String query, Object[] positionalParams, Integer max ->
            deprecated("findAll(String query, Object[] positionalParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.static.findAll = {String query, Object[] positionalParams, Integer max, Integer offset ->
            deprecated("findAll(String query, Object[] positionalParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max, offset] as Object[])
        }
        metaClass.static.findAll = {String query, Object[] positionalParams, Map args ->
            deprecated("findAll(String query, Object[] positionalParams, Map namedArgs)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, args] as Object[])
        }
        metaClass.static.findAll = {String query, Map namedArgs, Integer max ->
            deprecated("findAll(String query, Map namedParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, max] as Object[])
        }
        metaClass.static.findAll = {String query, Map namedArgs, Integer max, Integer offset ->
            deprecated("findAll(String query, Map namedParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, max, offset] as Object[])
        }

        metaClass.static.find = {String query, Object[] args ->
            deprecated("find(String query, Object[] positionalParams)")
            findMethod.invoke(domainClassType, "find", [query, args] as Object[])
        }

        metaClass.static.executeQuery = {String query, Object[] positionalParams ->
            deprecated("executeQuery(String query, Object[] positionalParams)")
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])
        }
        metaClass.static.executeQuery = {String query, Object[] positionalParams, Map args ->
            deprecated("executeQuery(String query, Object[] positionalParams, Map namedParams)")
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, args] as Object[])
        }

        metaClass.static.executeUpdate = {String query, Object[] args ->
            deprecated("executeQuery(String query, Object[] positionalParams)")
            template.bulkUpdate(query, args)
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
     * Adds the basic methods for performing persistence such as save, get, delete etc.
     */
    private static addBasicPersistenceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def classLoader = application.classLoader
        SessionFactory sessionFactory = ctx.getBean("sessionFactory")
        def template = new HibernateTemplate(sessionFactory)

        def saveMethod = new SavePersistentMethod(sessionFactory, classLoader, application, dc)
        def metaClass = dc.metaClass

        metaClass.save = {Boolean validate ->
            saveMethod.invoke(delegate, "save", [validate] as Object[])
        }
        metaClass.save = {Map args ->
            saveMethod.invoke(delegate, "save", [args] as Object[])
        }
        metaClass.save = {->
            saveMethod.invoke(delegate, "save", [] as Object[])
        }

        def mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, application, dc)
        metaClass.merge = {Map args ->
            mergeMethod.invoke(delegate, "merge", [args] as Object[])
        }
        metaClass.merge = {->
            mergeMethod.invoke(delegate, "merge", [] as Object[])
        }
        metaClass.static.merge = { Object instance ->
            mergeMethod.invoke(instance, "merge", [] as Object[])
        }

        metaClass.instanceOf = { Class c ->
            if (delegate instanceof HibernateProxy) {
                def instance = GrailsHibernateUtil.unwrapProxy(delegate)
                return c.isInstance(instance)
            }
            return c.isInstance(delegate)
        }

        metaClass.delete = {->
            def obj = delegate
            try {
                template.execute({Session session ->
                    session.delete obj
                    if (this.shouldFlush()) {
                        session.flush()
                    }
                } as HibernateCallback)
            }
            catch (DataAccessException e) {
                handleDataAccessException(template, e)
            }
        }
        metaClass.delete = { Map args ->
            def obj = delegate
            template.delete obj
            if (shouldFlush(args)) {
                try {
                    template.flush()
                }
                catch (DataAccessException e) {
                    handleDataAccessException(template, e)
                }
            }
        }
        metaClass.refresh = {-> template.refresh(delegate); delegate }
        metaClass.discard = {->template.evict(delegate); delegate }
        metaClass.attach = {->template.lock(delegate, LockMode.NONE); delegate }
        metaClass.isAttached = {-> template.contains(delegate) }

        metaClass.static.get = {id ->
            def identityType = dc.identifier.type

            id = convertToType(id, identityType)
            if (id != null) {
                final Object result = template.get(dc.clazz, id)
                return GrailsHibernateUtil.unwrapIfProxy(result)
            }
        }

        metaClass.static.read = {id ->
            if (id == null) {
                return null
            }

            template.execute({ Session session ->
                def o = get(id)
                if (o && session.contains(o)) {
                    session.setReadOnly(o, true)
                }
                return o
            } as HibernateCallback)
        }

        metaClass.static.load = { id ->
            id = convertToType(id, dc.identifier.type)
            if (id != null) {
                return template.load(dc.clazz, id)
            }
        }

        metaClass.static.count = {->
            template.execute({Session session ->
                def criteria = session.createCriteria(dc.clazz)
                criteria.setProjection(Projections.rowCount())
                def num = criteria.uniqueResult()
                num == null ? 0 : num
            } as HibernateCallback)
        }

        metaClass.isDirty = { ->
            def session = sessionFactory.currentSession
            def entry = findEntityEntry(delegate, session)
            if (!entry) {
                return false
            }

            Object[] values = entry.persister.getPropertyValues(delegate, session.entityMode)
            def dirtyProperties = entry.persister.findDirty(values, entry.loadedState, delegate, session)
            return dirtyProperties != null
        }

        metaClass.isDirty = { String fieldName ->
            def session = sessionFactory.currentSession
            def entry = findEntityEntry(delegate, session)
            if (!entry) {
                return false
            }

            Object[] values = entry.persister.getPropertyValues(delegate, session.entityMode)
            int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, delegate, session)
            int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
            return fieldIndex in dirtyProperties
        }

        metaClass.getDirtyPropertyNames = { ->
            def session = sessionFactory.currentSession
            def entry = findEntityEntry(delegate, session)
            if (!entry) {
                return []
            }

            Object[] values = entry.persister.getPropertyValues(delegate, session.entityMode)
            int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, delegate, session)
            def names = []
            for (index in dirtyProperties) {
                names << entry.persister.propertyNames[index]
            }
            names
        }

        metaClass.getPersistentValue = { String fieldName ->
            def session = sessionFactory.currentSession
            def entry = findEntityEntry(delegate, session, false)
            if (!entry) {
                return null
            }

            int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
            return fieldIndex == -1 ? null : entry.loadedState[fieldIndex]
        }
    }

    private static findEntityEntry(instance, session, boolean forDirtyCheck = true) {
        def entry = session.persistenceContext.getEntry(instance)
        if (!entry) {
            return null
        }

        if (forDirtyCheck && !entry.requiresDirtyCheck(instance) && entry.loadedState) {
            return null
        }

        entry
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
     * Converts an id value to the appropriate type for a domain class
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

        if (value != null && !targetType.isAssignableFrom(value.class)) {
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

    static shouldFlush(Map map = [:]) {
        def shouldFlush

        if (map?.containsKey('flush')) {
            shouldFlush = Boolean.TRUE == map.flush
        } else {
            def config = ConfigurationHolder.flatConfig
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
