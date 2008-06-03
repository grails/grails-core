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
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.metaclass.StaticMethodInvocation
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.HibernateDomainClassValidator
import org.hibernate.LockMode
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.SimpleTypeConverter
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.orm.hibernate3.*
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate;

/**
* A plug-in that handles the configuration of Hibernate within Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class HibernateGrailsPlugin {

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [dataSource: version,
                     i18n: version,
                     core: version,
                     domainClass: version]

    def loadAfter = ['controllers']

    def watchedResources = ["file:./grails-app/conf/hibernate/**.xml"]
    def hibProps = [:]
    def hibConfigClass

    def doWithSpring = {        
        def factory = new PersistentConstraintFactory(getSpringConfig().getUnrefreshedApplicationContext(), UniqueConstraint.class)
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, factory);

        application.domainClasses.each {dc ->
            "${dc.fullName}Validator"(HibernateDomainClassValidator) {
                messageSource = ref("messageSource")
                domainClass = ref("${dc.fullName}DomainClass")
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

        BeanDefinition externalDefinition = checkExternalBeans(application, log)
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
            log.info "Set db generation strategy to '${hibProps.'hibernate.hbm2ddl.auto'}'"

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
			lobHandlerDetector(SpringLobHandlerDetectorFactoryBean) {
				dataSource = dataSource
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
				entityInterceptor = eventTriggeringInterceptor
            }

            transactionManager(HibernateTransactionManager) {
                sessionFactory = sessionFactory
            }
            persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                sessionFactory = sessionFactory
            }

            if (manager?.hasGrailsPlugin("controllers")) {
                openSessionInViewInterceptor(GrailsOpenSessionInViewInterceptor) {
                    flushMode = HibernateAccessor.FLUSH_AUTO
                    sessionFactory = sessionFactory
                }
                grailsUrlHandlerMapping.interceptors << openSessionInViewInterceptor
            }

        }

    }


    def doWithDynamicMethods = {ctx ->

        SessionFactory sessionFactory = ctx.sessionFactory
        // we're going to configure Grails to lazily initialise the dynamic methods on domain classes to avoid
        // the overhead of doing so at start-up time
        def lazyInit = {GrailsDomainClass dc ->
            registerDynamicMethods(dc, application, ctx)
            for (subClass in dc.subClasses) {
                registerDynamicMethods(subClass, application, ctx)
            }
            MetaClass emc = GroovySystem.metaClassRegistry.getMetaClass(dc.clazz)
        }

        for (dc in application.domainClasses) {
            //    registerDynamicMethods(dc, application, ctx)
            MetaClass mc = dc.metaClass
            def initDomainClass = lazyInit.curry(dc)
            // these need to be eagerly initialised here, otherwise Groovy's findAll from the DGM is called
            def findAllMethod = new FindAllPersistentMethod(sessionFactory, application.classLoader)
            mc.'static'.findAll = {->
                findAllMethod.invoke(mc.javaClass, "findAll", [] as Object[])
            }
            mc.'static'.findAll = {Object example -> findAllMethod.invoke(mc.javaClass, "findAll", [example] as Object[])}
            mc.'static'.findAll = {Object example, Map args -> findAllMethod.invoke(mc.javaClass, "findAll", [example, args] as Object[])}
                        
            mc.methodMissing = { String name, args ->
                initDomainClass()
                mc.invokeMethod(delegate, name, args)
            }
            mc.'static'.methodMissing = {String name, args ->
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
            addValidationMethods(dc, application, ctx) 
        }
    }

    private registerDynamicMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        dc.metaClass.methodMissing = { String name, args ->
            throw new MissingMethodException(name, dc.clazz, args, true)
        }
        addBasicPersistenceMethods(dc, application, ctx)
        addQueryMethods(dc, application, ctx)
        addTransactionalMethods(dc, application, ctx)
        addValidationMethods(dc, application, ctx)
        addDynamicFinderSupport(dc, application, ctx)
    }

    private addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def mc = dc.metaClass
        def GroovyClassLoader classLoader = application.classLoader
        def sessionFactory = ctx.sessionFactory

        def dynamicMethods = [new FindAllByPersistentMethod(application, sessionFactory, classLoader),
        new FindByPersistentMethod(application, sessionFactory, classLoader),
        new CountByPersistentMethod(application, sessionFactory, classLoader),
        new ListOrderByPersistentMethod(sessionFactory, classLoader)]

        // This is the code that deals with dynamic finders. It looks up a static method, if it exists it invokes it
        // otherwise it trys to match the method invocation to one of the dynamic methods. If it matches it will
        // register a new method with the ExpandoMetaClass so the next time it is invoked it doesn't have this overhead.
        mc.'static'.methodMissing = {String methodName, args ->
            def result = null
            StaticMethodInvocation method = dynamicMethods.find {it.isMethodMatch(methodName)}
            if (method) {
                // register the method invocation for next time
                synchronized(this) {
                    mc.'static'."$methodName" = {List varArgs ->
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

    private addValidationMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory

        def validateMethod = new ValidatePersistentMethod(sessionFactory, application.classLoader, application)

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

    private addTransactionalMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory
        def Class domainClassType = dc.clazz

        def GroovyClassLoader classLoader = application.classLoader

        metaClass.'static'.withTransaction = {Closure callable ->
            new TransactionTemplate(ctx.transactionManager).execute({status ->
                callable.call(status)
            } as TransactionCallback)
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

    private addQueryMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory
        def template = new HibernateTemplate(sessionFactory)
        def Class domainClassType = dc.clazz

        def GroovyClassLoader classLoader = application.classLoader
        def findAllMethod = new FindAllPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.findAll = {String query ->
            findAllMethod.invoke(domainClassType, "findAll", [query] as Object[])
        }
        metaClass.'static'.findAll = {String query, Collection positionalParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams] as Object[])
        }
        metaClass.'static'.findAll = {String query, Collection positionalParams, Map paginateParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, paginateParams] as Object[])
        }
        metaClass.'static'.findAll = {String query, Map namedArgs ->
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs] as Object[])
        }
        metaClass.'static'.findAll = {String query, Map namedArgs, Map paginateParams ->
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, paginateParams] as Object[])
        }

        def findMethod = new FindPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.find = {String query ->
            findMethod.invoke(domainClassType, "find", [query] as Object[])
        }
        metaClass.'static'.find = {String query, Collection args ->
            findMethod.invoke(domainClassType, "find", [query, args] as Object[])
        }
        metaClass.'static'.find = {String query, Map namedArgs ->
            findMethod.invoke(domainClassType, "find", [query, namedArgs] as Object[])
        }
        metaClass.'static'.find = {Object example ->
            findMethod.invoke(domainClassType, "find", [example] as Object[])
        }

        def executeQueryMethod = new ExecuteQueryPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.executeQuery = {String query ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query] as Object[])
        }
        metaClass.'static'.executeQuery = {String query, Collection positionalParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])
        }
        metaClass.'static'.executeQuery = {String query, Collection positionalParams, Map paginateParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, paginateParams] as Object[])
        }
        metaClass.'static'.executeQuery = {String query, Map namedParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams] as Object[])
        }
        metaClass.'static'.executeQuery = {String query, Map namedParams, Map paginateParams ->
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams, paginateParams] as Object[])
        }

        metaClass.'static'.executeUpdate = {String query ->
            template.bulkUpdate(query)
        }
        metaClass.'static'.executeUpdate = {String query, Collection args ->
            template.bulkUpdate(query, GrailsClassUtils.collectionToObjectArray(args))
        }
        metaClass.'static'.executeUpdate = {String query, Map argMap ->
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
        metaClass.'static'.list = {-> listMethod.invoke(domainClassType, "list", [] as Object[])}
        metaClass.'static'.list = {Map args -> listMethod.invoke(domainClassType, "list", [args] as Object[])}
        metaClass.'static'.findWhere = {Map query ->
            template.execute({Session session ->
                def criteria = session.createCriteria(domainClassType)
                criteria.add(org.hibernate.criterion.Expression.allEq(query))
                criteria.setMaxResults(1)
                criteria.uniqueResult()
            } as HibernateCallback)
        }
        metaClass.'static'.findAllWhere = {Map query ->
            template.execute({Session session ->
                def criteria = session.createCriteria(domainClassType)
                criteria.add(org.hibernate.criterion.Expression.allEq(query))
                criteria.list()
            } as HibernateCallback)

        }
        metaClass.'static'.getAll = {->
            template.execute({session ->
                session.createCriteria(domainClassType).list()
            } as HibernateCallback)
        }
        metaClass.'static'.getAll = {List ids ->
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
        metaClass.'static'.exists = {id ->
            def identityType = dc.identifier.type
            id = convertToType(id, identityType)
            template.get(domainClassType, id) != null
        }

        metaClass.'static'.createCriteria = {-> new HibernateCriteriaBuilder(domainClassType, sessionFactory)}
        metaClass.'static'.withCriteria = {Closure callable ->
            new HibernateCriteriaBuilder(domainClassType, sessionFactory).invokeMethod("doCall", callable)
        }
        metaClass.'static'.withCriteria = {Map builderArgs, Closure callable ->
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
        metaClass.'static'.findAll = {String query, Integer max ->
            deprecated("findAll(String query, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, max] as Object[])
        }
        metaClass.'static'.findAll = {String query, Integer max, Integer offset ->
            deprecated("findAll(String query, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, max, offset] as Object[])
        }

        metaClass.'static'.findAll = {String query, Collection positionalParams, Integer max ->
            deprecated("findAll(String query, Collection positionalParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.'static'.findAll = {String query, Collection positionalParams, Integer max, Integer offset ->
            deprecated("findAll(String query, Collection positionalParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max, offset] as Object[])
        }
        metaClass.'static'.findAll = {String query, Object[] positionalParams ->
            deprecated("findAll(String query, Object[] positionalParams)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams] as Object[])
        }
        metaClass.'static'.findAll = {String query, Object[] positionalParams, Integer max ->
            deprecated("findAll(String query, Object[] positionalParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.'static'.findAll = {String query, Object[] positionalParams, Integer max, Integer offset ->
            deprecated("findAll(String query, Object[] positionalParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, max, offset] as Object[])
        }
        metaClass.'static'.findAll = {String query, Object[] positionalParams, Map args ->
            deprecated("findAll(String query, Object[] positionalParams, Map namedArgs)")
            findAllMethod.invoke(domainClassType, "findAll", [query, positionalParams, args] as Object[])
        }
        metaClass.'static'.findAll = {String query, Map namedArgs, Integer max ->
            deprecated("findAll(String query, Map namedParams, int max)")
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, max] as Object[])
        }
        metaClass.'static'.findAll = {String query, Map namedArgs, Integer max, Integer offset ->
            deprecated("findAll(String query, Map namedParams, int max, int offset)")
            findAllMethod.invoke(domainClassType, "findAll", [query, namedArgs, max, offset] as Object[])
        }

        metaClass.'static'.find = {String query, Object[] args ->
            deprecated("find(String query, Object[] positionalParams)")
            findMethod.invoke(domainClassType, "find", [query, args] as Object[])
        }

        metaClass.'static'.executeQuery = {String query, Object[] positionalParams ->
            deprecated("executeQuery(String query, Object[] positionalParams)")
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])
        }
        metaClass.'static'.executeQuery = {String query, Object[] positionalParams, Map args ->
            deprecated("executeQuery(String query, Object[] positionalParams, Map namedParams)")
            executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, args] as Object[])
        }

        metaClass.'static'.executeUpdate = {String query, Object[] args ->
            deprecated("executeQuery(String query, Object[] positionalParams)")
            template.bulkUpdate(query, args)
        }
        //
    }
    /**
     * Adds the basic methods for performing persistence such as save, get, delete etc.
     */
    private addBasicPersistenceMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def classLoader = application.classLoader
        SessionFactory sessionFactory = ctx.sessionFactory
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
        metaClass.merge = {args ->
            mergeMethod.invoke(delegate, "merge", [args] as Object[])
        }
        metaClass.merge = {->
            mergeMethod.invoke(delegate, "merge", [] as Object[])
        }

        metaClass.delete = {->
            def obj = delegate
            template.execute({Session session ->
                session.delete obj
            } as HibernateCallback)
        }
        metaClass.delete = { Map args ->
            def obj = delegate
            template.execute({Session session ->
                session.delete obj
                if(args?.flush) {
                    session.flush()
                }
            } as HibernateCallback)
        }
        metaClass.refresh = {->template.refresh(delegate)}
        metaClass.discard = {->template.evict(delegate)}
        metaClass.'static'.get = {id ->
            def identityType = dc.identifier.type

            id = convertToType(id, identityType)
            if(id) {
                return template.get(dc.clazz, id)
            }
        }


        metaClass.'static'.count = {->
            template.execute({Session session ->
                def criteria = session.createCriteria(dc.clazz)
                criteria.setProjection(org.hibernate.criterion.Projections.rowCount())
                criteria.uniqueResult()
            } as HibernateCallback)
        }
    }

    private convertToType(value, targetType) {
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

    def onChange = {event ->
        if (event.source instanceof Resource) {
            restartContainer()
        }
    }

    private checkExternalBeans(application, log) {
        def parent = application.parentContext
        try {
            def resourcesXml = parent?.getResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML);
            if (resourcesXml && resourcesXml.exists()) {
                def xmlBeans = new org.springframework.beans.factory.xml.XmlBeanFactory(resourcesXml);
                if (xmlBeans.containsBean("dataSource")) {
                    log.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_XML}")
                    return xmlBeans.getBeanDefinition("dataSource");
                }
            }
        } catch (FileNotFoundException fnfe) {
            // that's ok external resources file not required
        }
        
        // Check resources.groovy
        def emptyConfig = new WebRuntimeSpringConfiguration(parent, application.classLoader)
        GrailsRuntimeConfigurator.loadSpringGroovyResources(emptyConfig, application.classLoader)
        if (emptyConfig.getBeanNames().contains("dataSource")) {
            log.info("Using dataSource bean definition from ${GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY}")
            return emptyConfig.getBeanConfig("dataSource").getBeanDefinition()
        }
        return null
    }

}
