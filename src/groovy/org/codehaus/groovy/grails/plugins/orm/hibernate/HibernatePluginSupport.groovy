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
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.metaclass.StaticMethodInvocation
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.SimpleTypeConverter
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor
import org.springframework.orm.hibernate3.*
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.hibernate.LockMode
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.hibernate.Criteria
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.proxy.HibernateProxy
import org.hibernate.proxy.LazyInitializer
import org.apache.commons.beanutils.PropertyUtils
import org.hibernate.Hibernate
import org.springframework.validation.Validator
import org.springframework.dao.DataAccessException
import org.hibernate.FlushMode
import org.codehaus.groovy.grails.commons.ConfigurationHolder


/**
 * Used by HibernateGrailsPlugin to implement the core parts of GORM
 * 
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 13, 2009
 */

public class HibernatePluginSupport {

    static final Log LOG = LogFactory.getLog(HibernatePluginSupport)

    static hibProps = [:]
    static hibConfigClass

    static doWithSpring = {
        def factory = new PersistentConstraintFactory(getSpringConfig().getUnrefreshedApplicationContext(), UniqueConstraint.class)
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, factory);

        for(GrailsDomainClass dc in application.domainClasses) {
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
                if(ds.dialect instanceof Class)
                    hibProps."hibernate.dialect" = ds.dialect.name
                else
                    hibProps."hibernate.dialect" = ds.dialect.toString()
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

            if(hibConfig) {
                def cacheProvider = hibConfig.cache.provider_class
                if('org.hibernate.cache.EhCacheProvider' == cacheProvider) {
                    try {
                        def cacheClass = getClass().classLoader.loadClass('net.sf.ehcache.Cache')
                    } catch (Throwable t) {
                        hibConfig.remove('cache')
                        log.error """WARNING: Your cache provider is set to 'org.hibernate.cache.EhCacheProvider' in DataSource.groovy, however the classes for this provider cannot be found.
Try using Grails' default cache provider: 'org.hibernate.cache.OSCacheProvider'"""
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
			eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            sessionFactory(ConfigurableLocalSessionFactoryBean) {
                dataSource = dataSource
                if (application.classLoader.getResource("hibernate.cfg.xml")) {
                    configLocation = "classpath:hibernate.cfg.xml"
                }
                if (hibConfigClass) {
                    configClass = ds.configClass
                }
                hibernateProperties = hibernateProperties
                grailsApplication = ref("grailsApplication", true)
				lobHandler = lobHandlerDetector

				eventListeners = ['pre-load':eventTriggeringInterceptor,
                                  'post-load':eventTriggeringInterceptor,
                                  'save':eventTriggeringInterceptor,
                                  'save-update':eventTriggeringInterceptor,
                                  'post-insert':eventTriggeringInterceptor,
                                  'pre-update':eventTriggeringInterceptor,
                                  'post-update':eventTriggeringInterceptor,
                                  'pre-delete':eventTriggeringInterceptor,
                                  'post-delete':eventTriggeringInterceptor]
            }

            transactionManager(HibernateTransactionManager) {
                sessionFactory = sessionFactory
            }
            persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                sessionFactory = sessionFactory
            }

            if (manager?.hasGrailsPlugin("controllers")) {
                openSessionInViewInterceptor(GrailsOpenSessionInViewInterceptor) {

                    if(hibConfig.flush.mode instanceof String) {
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
                if(getSpringConfig().containsBean("grailsUrlHandlerMapping")) {                    
                    grailsUrlHandlerMapping.interceptors << openSessionInViewInterceptor
                }
            }

        }

    }

    static final doWithDynamicMethods = {ApplicationContext ctx ->

        if(ctx.containsBean("sessionFactory")) {            
            SessionFactory sessionFactory = ctx.getBean("sessionFactory")
            enhanceSessionFactory(sessionFactory, application, ctx)
        }
    }

    public static void enhanceProxy ( HibernateProxy proxy ) {
            MetaClass emc = GroovySystem.metaClassRegistry.getMetaClass(proxy.getClass())
            proxy.metaClass = emc
    }    

   public static void enhanceProxyClass ( Class proxyClass ) {
       def mc = proxyClass.metaClass
       if(! mc.pickMethod('grailsEnhanced', GrailsHibernateUtil.EMPTY_CLASS_ARRAY) ) {
           // getter
           mc.propertyMissing = { String name ->
               if(delegate instanceof HibernateProxy) {
                   return GrailsHibernateUtil.unwrapProxy(delegate)."$name"
               }
               else {
                   throw new MissingPropertyException(name, delegate.class)
               }
           }

           // setter
           mc.propertyMissing = { String name, val ->
               if(delegate instanceof HibernateProxy) {
                   GrailsHibernateUtil.unwrapProxy(delegate)."$name" = val
               }
               else {
                   throw new MissingPropertyException(name, delegate.class)
               }
             }

           mc.methodMissing = { String name, args ->
                if(delegate instanceof HibernateProxy) {
                    def obj = GrailsHibernateUtil.unwrapProxy(delegate)
                    return obj."$name"(*args)
                }
                else {
                    throw new MissingPropertyException(name, delegate.class)
                }

           }
           mc.grailsEnhanced = { true }
       }
    }

    private static DOMAIN_INITIALIZERS = [:]
    static initializeDomain(Class c) {
         DOMAIN_INITIALIZERS.get(c)?.call()
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

        for (GrailsDomainClass dc in application.domainClasses) {
            //    registerDynamicMethods(dc, application, ctx)
            MetaClass mc = dc.metaClass
            def initDomainClass = lazyInit.curry(dc)
            DOMAIN_INITIALIZERS[dc.clazz] = initDomainClass
            // these need to be eagerly initialised here, otherwise Groovy's findAll from the DGM is called
            def findAllMethod = new FindAllPersistentMethod(sessionFactory, application.classLoader)
            mc.static.findAll = {->
                findAllMethod.invoke(mc.javaClass, "findAll", [] as Object[])
            }
            mc.static.findAll = {Object example -> findAllMethod.invoke(mc.javaClass, "findAll", [example] as Object[])}
            mc.static.findAll = {Object example, Map args -> findAllMethod.invoke(mc.javaClass, "findAll", [example, args] as Object[])}

            mc.methodMissing = { String name, args ->
                initDomainClass()
                mc.invokeMethod(delegate, name, args)
            }
            mc.static.methodMissing = {String name, args ->
                initDomainClass()
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
        }

    }


    static final LAZY_PROPERTY_HANDLER = { String propertyName ->
      def propertyValue = PropertyUtils.getProperty(delegate, propertyName)
      if(propertyValue instanceof HibernateProxy) {          
          return GrailsHibernateUtil.unwrapProxy(propertyValue)
      }
      return propertyValue
    }

    /**
     * This method overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell
     */
    public static void handleLazyProxy ( GrailsDomainClass domainClass, org.codehaus.groovy.grails.commons.GrailsDomainClassProperty property) {
        String propertyName = property.name
        def getterName = GrailsClassUtils.getGetterName(propertyName)
        def setterName = GrailsClassUtils.getSetterName(propertyName)
        domainClass.metaClass."${getterName}" = LAZY_PROPERTY_HANDLER.curry(propertyName)
        domainClass.metaClass."${setterName}" = { PropertyUtils.setProperty(delegate, propertyName, it)  }

        for(GrailsDomainClass sub in domainClass.subClasses) {
            handleLazyProxy(sub, sub.getPropertyByName(property.name))
        }
    }


    private static registerDynamicMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx, SessionFactory sessionFactory) {
        dc.metaClass.methodMissing = { String name, args ->
//            println "METHOD MISSING $name"
            throw new MissingMethodException(name, dc.clazz, args, true)
        }
//        dc.metaClass.propertyMissing = { String name ->
//            println "PROPERTY MISSING HERE ! $name"
//
//        }
        addBasicPersistenceMethods(dc, application, ctx)
        addQueryMethods(dc, application, ctx)
        addTransactionalMethods(dc, application, ctx)
        addValidationMethods(dc, application, ctx,sessionFactory)
        addDynamicFinderSupport(dc, application, ctx)
    }

    private static addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass
        def GroovyClassLoader classLoader = application.classLoader
        def sessionFactory = ctx.getBean('sessionFactory')

        def dynamicMethods = [new FindAllByPersistentMethod(application, sessionFactory, classLoader),
        new FindByPersistentMethod(application, sessionFactory, classLoader),
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
            validateMethod.invoke(delegate, "validate", [] as Object[])
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

        def GroovyClassLoader classLoader = application.classLoader

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
        def Class domainClassType = dc.clazz

        def GroovyClassLoader classLoader = application.classLoader
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

        metaClass.static.executeUpdate = {String query ->
            template.bulkUpdate(query)
        }
        metaClass.static.executeUpdate = {String query, Collection args ->           
            template.bulkUpdate(query, GrailsClassUtils.collectionToObjectArray(args))
        }
        metaClass.static.executeUpdate = {String query, Map argMap ->
            template.execute(  { session ->
                                    Query queryObject = session.createQuery(query)
                                    SessionFactoryUtils.applyTransactionTimeout(queryObject, template.sessionFactory);
                                    for (entry in argMap) {
                                        queryObject.setParameter(entry.key, entry.value)
                                    }
                                    queryObject.executeUpdate()
                                } as HibernateCallback
                            , true);
        }


        def listMethod = new ListPersistentMethod(sessionFactory, classLoader)
        metaClass.static.list = {-> listMethod.invoke(domainClassType, "list", [] as Object[])}
        metaClass.static.list = {Map args -> listMethod.invoke(domainClassType, "list", [args] as Object[])}
        metaClass.static.findWhere = {Map query ->
            template.execute({Session session ->
                def criteria = session.createCriteria(domainClassType)
                criteria.add(org.hibernate.criterion.Expression.allEq(query))
                criteria.setMaxResults(1)
                criteria.uniqueResult()
            } as HibernateCallback)
        }
        metaClass.static.findAllWhere = {Map query ->
            template.execute({Session session ->
                def criteria = session.createCriteria(domainClassType)
                criteria.add(org.hibernate.criterion.Expression.allEq(query))
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
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
                criteria.add(org.hibernate.criterion.Restrictions.'in'(dc.identifier.name, ids))
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
            template.get(domainClassType, id) != null
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
        //
    }
    /**
     * Adds the basic methods for performing persistence such as save, get, delete etc.
     */
    private static addBasicPersistenceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def classLoader = application.classLoader
        SessionFactory sessionFactory = ctx.getBean("sessionFactory")
        def template = new HibernateTemplate(sessionFactory)


        def saveMethod = new SavePersistentMethod(sessionFactory, classLoader, application)
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

        def mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, application)
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
            if(delegate instanceof HibernateProxy) {
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
                    if(this.shouldFlush()) {
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
            if(shouldFlush(args)) {
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
            if(id != null) {
                return template.get(dc.clazz, id)
            }
        }

        metaClass.static.read = {id ->
            def identityType = dc.identifier.type

            id = convertToType(id, identityType)

            if(id != null) {
                return template.execute({ Session session ->
                    def o = session.get(dc.clazz, id)
                    if(o && session.contains(o))
                        session.setReadOnly( o, true )
                    return o
                } as HibernateCallback)
            }
        }


        metaClass.static.count = {->
            template.execute({Session session ->
                def criteria = session.createCriteria(dc.clazz)
                criteria.setProjection(org.hibernate.criterion.Projections.rowCount())
                criteria.uniqueResult()
            } as HibernateCallback)
        }
    }

    static shouldFlush(Map map = [:]) {
        def shouldFlush

        if(map?.containsKey('flush')) {
            shouldFlush = Boolean.TRUE == map.flush
        } else {
            def config = ConfigurationHolder.flatConfig
            shouldFlush = Boolean.TRUE == config?.get('grails.gorm.autoFlush')
        }
        return shouldFlush
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
    

    private static convertToType(value, targetType) {
        SimpleTypeConverter typeConverter = new SimpleTypeConverter()

        if (value != null && !targetType.isAssignableFrom(value.class)) {
            if (value instanceof Number && Long.class.equals(targetType)) {
                value = value.toLong()
            }
            else {
                try {
                    value = typeConverter.convertIfNecessary(value, targetType)
                } catch (org.springframework.beans.TypeMismatchException e) {
                    // ignore
                };
            }
        }
        return value
    }


    private static checkExternalBeans(GrailsApplication application) {
        ApplicationContext parent = application.parentContext
        try {
            def resourcesXml = parent?.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML);
            if (resourcesXml && resourcesXml.exists()) {
                def xmlBeans = new org.springframework.beans.factory.xml.XmlBeanFactory(resourcesXml);
                if (xmlBeans.containsBean("dataSource")) {
                    LOG.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_XML}")
                    return xmlBeans.getBeanDefinition("dataSource");
                }
            }
        } catch (FileNotFoundException fnfe) {
            // that's ok external resources file not required
        }

        // Check resources.groovy
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(parent,application.classLoader)
        GrailsRuntimeConfigurator.loadExternalSpringConfig(springConfig, application.classLoader)
        if (springConfig.containsBean("dataSource")) {
            LOG.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY}")
            return springConfig.getBeanDefinition("dataSource")
        }
        return null
    }


}