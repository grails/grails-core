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
package org.codehaus.groovy.grails.plugins.orm.hibernate;
                                              
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils 
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.validation.*
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springmodules.beans.factory.config.MapToPropertiesFactoryBean;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.codehaus.groovy.runtime.InvokerHelper;   
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.orm.hibernate.metaclass.SavePersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.MergePersistentMethod
import org.hibernate.SessionFactory
import org.springframework.beans.SimpleTypeConverter
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import grails.orm.HibernateCriteriaBuilder
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ListPersistentMethod
import org.springframework.beans.BeanWrapperImpl
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ExecuteQueryPersistentMethod
import org.springframework.orm.hibernate3.HibernateTemplate
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindPersistentMethod
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod
import org.codehaus.groovy.grails.metaclass.DomainClassMethods



/**
* A plug-in that handles the configuration of Hibernate within Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class HibernateGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [dataSource:version,	                 
	                 i18n:version,
	                 core: version]
	                 
	def loadAfter = ['controllers']	                 
	
	def watchedResources = ["file:./grails-app/domain/**/*.groovy", "file:./hibernate/**.xml"]
	def hibProps = [:]  
	def hibConfigClass

	def doWithSpring = {
	        application.domainClasses.each { dc ->
                "${dc.fullName}Validator"(HibernateDomainClassValidator) {
                    messageSource = ref("messageSource")
                    domainClass = ref("${dc.fullName}DomainClass")
                }
	        }
			def vendorToDialect = new Properties()
			def hibernateDialects = application.classLoader.getResource("hibernate-dialects.properties")
			if(hibernateDialects) {
				def p = new Properties()
				p.load(hibernateDialects.openStream())
				p.each { entry ->
					vendorToDialect[entry.value] = "org.hibernate.dialect.${entry.key}".toString()
				}
			}
			def ds = application.config.dataSource
			if(ds || application.domainClasses.size() > 0) {  
				hibConfigClass = ds?.configClass
				
                if(ds && ds.loggingSql) {
                    hibProps."hibernate.show_sql" = "true"
                    hibProps."hibernate.format_sql" = "true"
                }
                if(ds && ds.dialect) {
                    hibProps."hibernate.dialect" = ds.dialect.name
                }
                else {
                    dialectDetector(HibernateDialectDetectorFactoryBean) {
                        dataSource = dataSource
                        vendorNameDialectMappings = vendorToDialect
                    }
                    hibProps."hibernate.dialect" = dialectDetector
                }
                if(!ds) {
                    hibProps."hibernate.hbm2ddl.auto" = "create-drop"
                }
                else if(ds.dbCreate) {
                    hibProps."hibernate.hbm2ddl.auto" = ds.dbCreate
                }

                hibernateProperties(MapToPropertiesFactoryBean) {
                    map = hibProps
                }
                sessionFactory(ConfigurableLocalSessionFactoryBean) {
                    dataSource = dataSource
                    if(application.classLoader.getResource("hibernate.cfg.xml")) {
                        configLocation = "classpath:hibernate.cfg.xml"
                    }
                    if(hibConfigClass) {
                        configClass = ds.configClass
                    }
                    hibernateProperties = hibernateProperties
                    grailsApplication = ref("grailsApplication", true)
                }
                transactionManager(HibernateTransactionManager) {
                    sessionFactory = sessionFactory
                }
                persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                    sessionFactory = sessionFactory
                }

                if(manager?.hasGrailsPlugin("controllers")) {
                    openSessionInViewInterceptor(GrailsOpenSessionInViewInterceptor) {
                        flushMode = HibernateAccessor.FLUSH_AUTO
                        sessionFactory = sessionFactory
                    }
                    grailsUrlHandlerMapping.interceptors << openSessionInViewInterceptor
                }

            }


	}
	
	def doWithApplicationContext = { ctx ->
	    if(ctx.containsBean('sessionFactory')) {
            def factory = new PersistentConstraintFactory(ctx.sessionFactory, UniqueConstraint.class)
            ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, factory);
            GrailsHibernateUtil.configureDynamicMethods(ctx, ctx.grailsApplication);            
        }
	}   
	
	def doWithDynamicMethods = { ctx->

        SessionFactory sessionFactory = ctx.sessionFactory
        if(sessionFactory)
            GrailsHibernateUtil.configureHibernateDomainClasses(sessionFactory, application)

		for(dc in application.domainClasses) {
		    addRelationshipManagementMethods(dc)
		    addBasicPersistenceMethods(dc, application, ctx)
		    addQueryMethods(dc, application, ctx)
		    addTransactionalMethods(dc, application, ctx)
		    addValidationMethods(dc, application, ctx)
		   // addDynamicFinderSupport(dc, application, ctx)
		}

	}
/*
    private addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ctx ) {
        def dynamicMethods = new DomainClassMethods(application, dc.clazz, ctx.sessionFactory, application.classLoader)
        dc.metaClass.'static'.invokeMethod = { String methodName, args ->


        }
    }
*/
    private addValidationMethods(GrailsDomainClass dc, GrailsApplication application, ctx ) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory

        def validateMethod = new ValidatePersistentMethod(sessionFactory, application.classLoader, application)
        metaClass.validate = {-> validateMethod.invoke(delegate, "validate", [] as Object[]) }
        metaClass.validate = {Map args-> validateMethod.invoke(delegate, "validate", [args] as Object[]) }
        metaClass.validate = {Boolean b-> validateMethod.invoke(delegate, "validate", [b] as Object[]) }        
    }

    private addTransactionalMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory
        def Class domainClassType = dc.clazz

        def GroovyClassLoader classLoader = application.classLoader

        metaClass.'static'.withTransaction = { Closure callable ->
            new TransactionTemplate(ctx.transactionManager).execute( { status ->
                    callable.call(status)
            } as TransactionCallback )
        }

    }

    private addQueryMethods(GrailsDomainClass dc, GrailsApplication application, ctx) {
        def metaClass = dc.metaClass
        SessionFactory sessionFactory = ctx.sessionFactory
        def template = new HibernateTemplate(sessionFactory)
        def Class domainClassType = dc.clazz

        def GroovyClassLoader classLoader = application.classLoader
        FindAllPersistentMethod findAllMethod = new FindAllPersistentMethod(sessionFactory, classLoader)
        metaClass.createCriteria = {-> new HibernateCriteriaBuilder(domainClassType,sessionFactory) }

        metaClass.'static'.findAll = {-> findAllMethod.invoke(domainClassType,"findAll", [] as Object[]) }
        metaClass.'static'.findAll = { String query -> findAllMethod.invoke(domainClassType,"findAll", [query] as Object[]) }
        metaClass.'static'.findAll = { String query, Integer max ->
            findAllMethod.invoke(domainClassType,"findAll", [query, max] as Object[])
        }
        metaClass.'static'.findAll = { String query, Integer max, Integer offset ->
            findAllMethod.invoke(domainClassType,"findAll", [query, max, offset] as Object[]) 
        }

        metaClass.'static'.findAll = { String query, Collection positionalParams ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams] as Object[])
        }
        metaClass.'static'.findAll = { String query, Collection positionalParams, Integer max ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.'static'.findAll = { String query, Collection positionalParams, Integer max, Integer offset ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, max, offset] as Object[])
        }

        metaClass.'static'.findAll = { String query, Object[] positionalParams ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams] as Object[]) 
        }
        metaClass.'static'.findAll = { String query, Object[] positionalParams, Integer max ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, max] as Object[])
        }
        metaClass.'static'.findAll = { String query, Object[] positionalParams, Integer max, Integer offset ->
            findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, max, offset] as Object[])
        }
        
        metaClass.'static'.findAll = { String query, Collection positionalParams, Map args -> findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, args] as Object[]) }
        metaClass.'static'.findAll = { String query, Object[] positionalParams, Map args -> findAllMethod.invoke(domainClassType,"findAll", [query, positionalParams, args] as Object[]) }
        metaClass.'static'.findAll = { String query, Map namedArgs ->
               findAllMethod.invoke(domainClassType,"findAll", [query, namedArgs] as Object[])
        }
        metaClass.'static'.findAll = { String query, Map namedArgs, Integer max ->
               findAllMethod.invoke(domainClassType,"findAll", [query, namedArgs, max] as Object[])
        }
        metaClass.'static'.findAll = { String query, Map namedArgs, Integer max, Integer offset ->
               findAllMethod.invoke(domainClassType,"findAll", [query, namedArgs, max, offset] as Object[])
        }
        metaClass.'static'.findAll = { String query, Map namedArgs, Map args ->
            findAllMethod.invoke(domainClassType,"findAll", [query, namedArgs, args] as Object[])
        }
        metaClass.'static'.findAll = { Object example -> findAllMethod.invoke(domainClassType,"findAll", [example] as Object[]) }

        def findMethod = new FindPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.find = { String query -> findMethod.invoke(domainClassType, "find", [query] as Object[] ) }
        metaClass.'static'.find = { Object example -> findMethod.invoke(domainClassType, "find", [example] as Object[] ) }
        metaClass.'static'.find = { String query, Collection args -> findMethod.invoke(domainClassType, "find", [query, args] as Object[] ) }
        metaClass.'static'.find = { String query, Object[] args -> findMethod.invoke(domainClassType, "find", [query, args] as Object[] ) }
        metaClass.'static'.find = { String query, Map namedArgs -> findMethod.invoke(domainClassType, "find", [query, namedArgs] as Object[] ) }


        def listMethod = new ListPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.list = {-> listMethod.invoke(domainClassType, "list", [] as Object[]) }
        metaClass.'static'.list = { Map args -> listMethod.invoke(domainClassType, "list", [args] as Object[]) }
        metaClass.'static'.findWhere = { Map query ->
            def criteria = sessionFactory.currentSession.createCriteria(domainClassType)
            criteria.add( org.hibernate.criterion.Expression.allEq(query))
            criteria.setMaxResults(1)
            criteria.uniqueResult()
        }
        metaClass.'static'.findAllWhere = { Map query ->
            def criteria = sessionFactory.currentSession.createCriteria(domainClassType)
            criteria.add( org.hibernate.criterion.Expression.allEq(query))
            criteria.list()        
        }
        metaClass.'static'.getAll = {-> sessionFactory.currentSession.createCriteria(domainClassType).list() }
        metaClass.'static'.getAll = { List ids ->
            def identityType = dc.identifier.type
            ids = ids.collect { convertToType(it, identityType) }
            def criteria = sessionFactory.currentSession.createCriteria(domainClassType)
            criteria.add(org.hibernate.criterion.Restrictions.'in'(dc.identifier.name, ids))
            def results = criteria.list()
            def idsMap = [:]
            for(object in results) {
                idsMap[object[dc.identifier.name]] = object
            }
            results.clear()
            for(id in ids) {
                results << idsMap[id]
            }
            results
        }
        metaClass.'static'.exists = { id ->
            def identityType = dc.identifier.type
            id = convertToType(it, identityType )
            template.get(domainClassType, id) != null
        }
        metaClass.'static'.withCriteria = { Closure callable ->
            new HibernateCriteriaBuilder(domainClassType, sessionFactory).invokeMethod("doCall", callable)
        }
        metaClass.'static'.withCriteria = { Map builderArgs, Closure callable ->
            def builder = new HibernateCriteriaBuilder(domainClassType, sessionFactory)
            def builderBean = new BeanWrapperImpl(builder)
            for(entry in builderArgs) {
                if(builderBean.isWritableProperty(entry.key) ) {
                    builderBean.setPropertyValue(entry.key,entry.value)
                }
            }
            builder.invokeMethod("doCall", callable)
        }


        def executeQueryMethod = new ExecuteQueryPersistentMethod(sessionFactory, classLoader)
        metaClass.'static'.executeQuery = { String query -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query] as Object[]) }
        metaClass.'static'.executeQuery = { String query, Collection positionalParams -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])}
        metaClass.'static'.executeQuery = { String query, Object[] positionalParams -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams] as Object[])}
        metaClass.'static'.executeQuery = { String query, Collection positionalParams, Map args -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, args] as Object[]) }
        metaClass.'static'.executeQuery = { String query, Object[] positionalParams, Map args -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, positionalParams, args] as Object[]) }
        metaClass.'static'.executeQuery = { String query, Map namedParams -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams] as Object[]) }
        metaClass.'static'.executeQuery = { String query, Map namedParams, Map args -> executeQueryMethod.invoke(domainClassType, "executeQuery", [query, namedParams, args] as Object[]) }


        

        metaClass.'static'.executeUpdate = { String query ->
            template.bulkUpdate(query)
        }
        metaClass.'static'.executeUpdate = { String query, Object[] args ->
            template.bulkUpdate(query, args)
        }
        metaClass.'static'.executeUpdate = { String query, Collection args ->
            template.bulkUpdate(query, GrailsClassUtils.collectionToObjectArray(args))
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

        metaClass.save = { args -> saveMethod.invoke(delegate, "save", args ) }
        metaClass.save = {-> saveMethod.invoke(delegate, "save", [] as Object[]) }

        def mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, application)
        metaClass.merge = { args -> mergeMethod.invoke(delegate, "merge", args) }
        metaClass.merge = {-> mergeMethod.invoke(delegate, "merge", [] as Object[]) }

        metaClass.delete = { template.delete(delegate) }
        metaClass.refresh = { template.refresh(delegate) }
        metaClass.discard = { template.evict(delegate) }
        metaClass.'static'.get = { id ->
            def identityType = dc.identifier.type

            id = convertToType(id, identityType)
            template.get(dc.clazz, id)
        }

        metaClass.'static'.count = {->
            def criteria = sessionFactory.currentSession.createCriteria(dc.clazz)
            criteria.setProjection(org.hibernate.criterion.Projections.rowCount())
            criteria.uniqueResult()
        }
    }

    SimpleTypeConverter typeConverter = new SimpleTypeConverter()
    private convertToType(value, targetType) {

        if(!targetType.isAssignableFrom(value.class)) {
            if(value instanceof Number && Long.class.equals(targetType)) {
                value = value.toLong()
            }
            else {
                value = typeConverter.convertIfNecessary(id, targetType);
            }
        }
        return value
    }

	private addRelationshipManagementMethods(GrailsDomainClass dc) {
        dc.persistantProperties.each { prop ->
            if(prop.oneToMany || prop.manyToMany) {
                if(dc.metaClass instanceof ExpandoMetaClass) {
                    def collectionName = "${prop.name[0].toUpperCase()}${prop.name[1..-1]}"
                    def otherDomainClass = prop.referencedDomainClass

                    dc.metaClass."addTo${collectionName}" = { Object arg ->
                        Object obj
                        if(delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        if(arg instanceof Map) {
                              obj = otherDomainClass.newInstance()
                              obj.properties = arg
                              delegate[prop.name].add(obj)
                        }
                        else if(otherDomainClass.clazz.isInstance(arg)) {
                              obj = arg
                              delegate[prop.name].add(obj)
                        }
                        else {
                            throw new MissingMethodException(dc.clazz, "addTo${collectionName}", [arg] as Object[])
                        }
                        if(prop.bidirectional) {
                            if(prop.manyToMany) {
                                String name = prop.otherSide.name
                                if(!obj[name]) {
                                    obj[name] = GrailsClassUtils.createConcreteCollection(prop.otherSide.type)
                                }
                                obj[prop.otherSide.name].add(delegate)
                            }
                            else {
                                  obj[prop.otherSide.name] = delegate
                            }
                        }
                        delegate
                    }
                    dc.metaClass."removeFrom${collectionName}" = { Object arg ->
                         if(otherDomainClass.clazz.isInstance(arg)) {
                             delegate[prop.name]?.remove(arg)
                            if(prop.bidirectional) {
                                 if(prop.manyToMany) {
                                    String name = prop.otherSide.name
                                       arg[name]?.remove(delegate)
                                 }
                                 else {
                                    arg[prop.otherSide.name] = null
                                 }
                            }
                         }
                        else {
                           throw new MissingMethodException(dc.clazz, "removeFrom${collectionName}", [arg] as Object[])
                        }
                        delegate
                    }
                }
            }
        }

    }
	
	def onChange = {  event -> 
		if(event.source instanceof Class) {
			def configurator = event.ctx.grailsConfigurator
 			def application = event.application
   		    def manager = event.manager
			MetaClassRegistry registry = GrailsMetaClassUtils.registry


			assert configurator
			assert application
			assert manager
			
			if(application.isArtefactOfType(DomainClassArtefactHandler.TYPE, event.source)) { 				
					application.domainClasses.each { dc ->
							registry.removeMetaClass(dc.getClazz())
					}
				
					// refresh whole application
					application.rebuild()

					// rebuild context
					configurator.reconfigure(event.ctx, manager.servletContext, false)					
					// refresh constraints
					application.refreshConstraints()    
					if(event.ctx.containsBean("groovyPagesTemplateEngine")) {
						event.ctx.groovyPagesTemplateEngine.clearPageCache()
					}
			}			
		}   
		else {         
			 new AntBuilder().copy(todir:"./web-app/WEB-INF/classes") { 
				 fileset(dir:"./hibernate", includes:"**/**")
			 }     
			 def beanDefs = beans {
                sessionFactory(ConfigurableLocalSessionFactoryBean) {
                    dataSource = ref("dataSource")
                    configLocation = "classpath:hibernate.cfg.xml"

                    if(hibConfigClass) {
                        configClass = hibConfigClass
                    }
                    hibernateProperties = ref("hibernateProperties")
                    grailsApplication = ref("grailsApplication", true)
                }
                transactionManager(HibernateTransactionManager) {
                    sessionFactory = sessionFactory
                }
                persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                    sessionFactory = sessionFactory
                }				
			} 
			event.ctx.registerBeanDefinition("sessionFactory", beanDefs.getBeanDefinition("sessionFactory")) 
			event.ctx.registerBeanDefinition("transactionManager", beanDefs.getBeanDefinition("transactionManager")) 			
			event.ctx.registerBeanDefinition("persistenceInterceptor", beanDefs.getBeanDefinition("persistenceInterceptor")) 									
		}
	}

}