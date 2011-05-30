/* Copyright (C) 2011 SpringSource
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

package org.codehaus.groovy.grails.orm.hibernate

import grails.orm.HibernateCriteriaBuilder
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.metaclass.StaticMethodInvocation
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.hibernate.*
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.engine.EntityEntry
import org.hibernate.engine.SessionImplementor
import org.hibernate.proxy.HibernateProxy
import org.springframework.beans.SimpleTypeConverter
import org.springframework.core.convert.ConversionService
import org.springframework.dao.DataAccessException
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.orm.hibernate3.HibernateCallback
import org.springframework.orm.hibernate3.HibernateTemplate
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Extended GORM Enhancer that fills out the remaining GORM for Hibernate methods
 * and implements string-based query support via HQL.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class HibernateGormEnhancer extends GormEnhancer {

    private ClassLoader classLoader
    private GrailsApplication grailsApplication

    HibernateGormEnhancer(HibernateDatastore datastore,
            PlatformTransactionManager transactionManager,
            GrailsApplication grailsApplication) {
        super(datastore, transactionManager);
        this.grailsApplication = grailsApplication
        this.classLoader = grailsApplication.classLoader

        def sessionFactory = datastore.sessionFactory

        finders = Collections.unmodifiableList([
            new FindAllByPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new FindAllByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new FindOrCreateByPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new FindOrSaveByPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new FindByPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new FindByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new CountByPersistentMethod(grailsApplication, sessionFactory, classLoader),
            new ListOrderByPersistentMethod(grailsApplication, sessionFactory, classLoader) ])
    }

    @SuppressWarnings("unchecked")
    protected GormValidationApi getValidationApi(Class cls) {
        def validateApi = new HibernateGormValidationApi(cls, datastore)
        validateApi.classLoader = classLoader
        return validateApi
    }

    @Override
    @SuppressWarnings("unchecked")
    protected GormStaticApi getStaticApi(Class cls) {
        def staticApi = new HibernateGormStaticApi(cls, datastore, finders)
        staticApi.classLoader = classLoader
        return staticApi
    }

    @Override
    @SuppressWarnings("unchecked")
    protected GormInstanceApi getInstanceApi(Class cls) {
        def instanceApi = new HibernateGormInstanceApi(cls, datastore)
        instanceApi.classLoader = classLoader
        return instanceApi
    }

    @Override
    protected void registerNamedQueries(PersistentEntity entity, Object namedQueries) {
        if (grailsApplication == null) {
            return
        }

        def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.name)
        if (domainClass != null) {
            new HibernateNamedQueriesBuilder(domainClass, finders).evaluate((Closure)namedQueries)
        }
    }
}

/**
 * The implementation of the GORM static method contract for Hibernate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class HibernateGormStaticApi extends GormStaticApi {
    private static final EMPTY_ARRAY = [] as Object[]

    private HibernateTemplate hibernateTemplate
    private SessionFactory sessionFactory
    private ConversionService conversionService
    private Class identityType
    private ListPersistentMethod listMethod
    private FindAllPersistentMethod findAllMethod
    private FindPersistentMethod findMethod
    private ExecuteQueryPersistentMethod executeQueryMethod
    private ExecuteUpdatePersistentMethod executeUpdateMethod
    private MergePersistentMethod mergeMethod
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader()

    HibernateGormStaticApi(Class persistentClass, HibernateDatastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders);
        this.sessionFactory = datastore.getSessionFactory()
        this.hibernateTemplate = new HibernateTemplate(sessionFactory)
        this.conversionService = datastore.mappingContext.conversionService

        this.executeQueryMethod = new ExecuteQueryPersistentMethod(sessionFactory, classLoader)
        this.executeUpdateMethod = new ExecuteUpdatePersistentMethod(sessionFactory, classLoader)
        this.findMethod = new FindPersistentMethod(sessionFactory, classLoader)
        this.findAllMethod = new FindAllPersistentMethod(sessionFactory, classLoader)
        identityType = persistentEntity.identity.type

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext) {
            org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()

            findAllMethod.grailsApplication = grailsApplication
            GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            identityType = domainClass.identifier.type

            this.mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass)
            this.listMethod = new ListPersistentMethod(grailsApplication, sessionFactory, classLoader)
        }
    }

    @Override
    Object get(Serializable id) {
        if (id || (id instanceof Number)) {
            id = convertIdentifier(id)
            final Object result = hibernateTemplate.get(persistentClass, id)
            return GrailsHibernateUtil.unwrapIfProxy(result)
        }
    }

    private SimpleTypeConverter typeConverter = new SimpleTypeConverter()

    private convertIdentifier(Serializable id) {
        final idType = identityType
        if (id != null && !idType.isAssignableFrom(id.class)) {
            try {
                if (id instanceof Number && Long.equals(idType)) {
                    id = id.toLong()
                }
                else {
                    id = typeConverter.convertIfNecessary(id, idType)
                }
            } catch (e) {
                // ignore
            }
        }

        return id
    }

    @Override
    Object read(Serializable id) {
        if (id == null) {
            return null
        }

        hibernateTemplate.execute({ Session session ->
            def o = get(id)
            if (o && session.contains(o)) {
                session.setReadOnly(o, true)
            }
            return o
        } as HibernateCallback)
    }

    @Override
    Object load(Serializable id) {
        id = convertIdentifier(id)
        if (id != null) {
            return hibernateTemplate.load(persistentClass, id)
        }
    }

    @Override
    List getAll() {
        hibernateTemplate.execute({ Session session ->
            session.createCriteria(persistentClass).list()
        } as HibernateCallback)
    }

    List getAll(List ids) {
        getAllInternal(ids)
    }

    @Override
    List getAll(Serializable... ids) {
        getAllInternal(ids)
    }

    private List getAllInternal(ids) {
        hibernateTemplate.execute({Session session ->
            ids = ids.collect { convertIdentifier(it) }
            def criteria = session.createCriteria(persistentClass)
            def identityName = persistentEntity.identity.name
            criteria.add(Restrictions.'in'(identityName, ids))
            def results = criteria.list()
            def idsMap = [:]
            for (object in results) {
                idsMap[object[identityName]] = object
            }
            results.clear()
            for (id in ids) {
                results << idsMap[id]
            }
            results
        } as HibernateCallback)
    }

    @Override
    Object createCriteria() {
        return new HibernateCriteriaBuilder(persistentClass, sessionFactory)
    }

    @Override
    Object lock(Serializable id) {
        id = convertIdentifier(id)
        hibernateTemplate.get(persistentClass, id, LockMode.UPGRADE)
    }

    @Override
    Object merge(Object o) {
        mergeMethod.invoke(o, "merge", [] as Object[])
    }

    @Override
    Integer count() {
        hibernateTemplate.execute({Session session ->
            def criteria = session.createCriteria(persistentClass)
            criteria.setProjection(Projections.rowCount())
            def num = criteria.uniqueResult()
            num == null ? 0 : num
        } as HibernateCallback)
    }

    @Override
    boolean exists(Serializable id) {
        id = convertIdentifier(id)
        hibernateTemplate.execute({ Session session ->
            session.createCriteria(persistentEntity.javaClass)
                .add(Restrictions.idEq(id))
                .setProjection(Projections.rowCount())
                .uniqueResult()
        } as HibernateCallback) == 1
    }

    @Override
    List list(Map params) {
        listMethod.invoke persistentClass, "list", [params] as Object[]
    }

    @Override
    List list() {
        listMethod.invoke persistentClass, "list", EMPTY_ARRAY
    }

    @Override
    List findAll(Object example, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [example, args] as Object[])
    }

    @Override
    Object find(Object example, Map args) {
        findMethod.invoke(persistentClass, "find", [example, args] as Object[])
    }

    /**
     * Finds a single result for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @return A single result or null
     *
     *
     * @deprecated Use Book.find('..', [foo:'bar], [max:10]) instead
     */
    Object find(String query, Map args, Integer max) {
        findMethod.invoke(persistentClass, "find", [query, args, max] as Object[])
    }

    /**
     * Finds a single result for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @param offset The offset
     * @return A single result or null
     *
     *
     * @deprecated Use Book.find('..', [foo:'bar], [max:10, offset:5]) instead
     */
    Object find(String query, Map args, Integer max, Integer offset) {
        findMethod.invoke(persistentClass, "find", [query, args, max, offset] as Object[])
    }

    /**
     * Finds a single result for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A single result or null
     *
     *
     * @deprecated Use Book.find('..', [max:10]) instead
     */
    Object find(String query, Integer max) {
        findMethod.invoke(persistentClass, "find", [query, max] as Object[])
    }

    /**
     * Finds a single result for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @param offset The offset to use
     * @return A single result or null
     *
     * @deprecated Use Book.find('..', [max:10, offset:5]) instead
     */
    Object find(String query, Integer max, Integer offset) {
        findMethod.invoke(persistentClass, "find", [query, max, offset] as Object[])
    }

    /**
     * Finds a list of results for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [foo:'bar], [max:10]) instead
     */
    Object findAll(String query, Map args, Integer max) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args, max] as Object[])
    }

    /**
     * Finds a list of results for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @param offset The offset
     *
     * @return A list of results
     *
     * @deprecated Use findAll('..', [foo:'bar], [max:10, offset:5]) instead
     */
    Object findAll(String query, Map args, Integer max, Integer offset) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args, max, offset] as Object[])
    }

    /**
     * Finds a list of results for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [max:10]) instead
     */
    Object findAll(String query, Integer max) {
        findAllMethod.invoke(persistentClass, "findAll", [query, max] as Object[])
    }

    /**
     * Finds a list of results for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [max:10, offset:5]) instead
     */
    Object findAll(String query, Integer max, Integer offset) {
        findAllMethod.invoke(persistentClass, "findAll", [query, max, offset] as Object[])
    }

    @Override
    List findAllWhere(Map queryMap, Map args) {
        if (!queryMap) return null
        hibernateTemplate.execute({Session session ->
            Map queryArgs = filterQueryArgumentMap(queryMap)
            List<String> nullNames = removeNullNames(queryArgs)
            Criteria criteria = session.createCriteria(persistentClass)
            criteria.add(Restrictions.allEq(queryArgs))
            for (name in nullNames) {
                criteria.add Restrictions.isNull(name)
            }
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            criteria.list()
        } as HibernateCallback)
    }

    @Override
    def findWhere(Map queryMap, Map args) {
        if (!queryMap) return null
        hibernateTemplate.execute({Session session ->
            Map queryArgs = filterQueryArgumentMap(queryMap)
            List<String> nullNames = removeNullNames(queryArgs)
            Criteria criteria = session.createCriteria(persistentClass)
            criteria.add(Restrictions.allEq(queryArgs))
            for (name in nullNames) {
                criteria.add Restrictions.isNull(name)
            }
            criteria.setMaxResults(1)
            GrailsHibernateUtil.unwrapIfProxy(criteria.uniqueResult())
        } as HibernateCallback)
    }

    private Map filterQueryArgumentMap(Map query) {
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

    private List<String> removeNullNames(Map query) {
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


    @Override
    Object withSession(Closure callable) {
        new HibernateTemplate(sessionFactory).execute({ session ->
            callable(session)
        } as HibernateCallback)
    }

    @Override
    Object withNewSession(Closure callable) {
        HibernateTemplate template = new HibernateTemplate(sessionFactory)
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        Session previousSession = sessionHolder?.session
        try {
            template.alwaysUseNewSession = true
            template.execute({ Session session ->
                if (sessionHolder == null) {
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

    @Override
    Object executeQuery(String query) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query] as Object[])
    }

    Object executeQuery(String query, Object arg) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, arg] as Object[])
    }

    @Override
    Object executeQuery(String query, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, args] as Object[])
    }

    @Override
    Object executeQuery(String query, Map params, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params, args] as Object[])
    }

    @Override
    Object executeQuery(String query, Collection params) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params] as Object[])
    }

    @Override
    Object executeQuery(String query, Collection params, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Map params, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Collection params) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Collection params, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params, args] as Object[])
    }

    @Override
    Object find(String query) {
        findMethod.invoke(persistentClass, "find", [query] as Object[])
    }

    Object find(String query, Object[] params) {
        findMethod.invoke(persistentClass, "find", [query, params] as Object[])
    }

    @Override
    Object find(String query, Map args) {
        findMethod.invoke(persistentClass, "find", [query, args] as Object[])
    }

    @Override
    Object find(String query, Map params, Map args) {
        findMethod.invoke(persistentClass, "find", [query, params, args] as Object[])
    }

    @Override
    Object find(String query, Collection params) {
        findMethod.invoke(persistentClass, "find", [query, params] as Object[])
    }

    @Override
    Object find(String query, Collection params, Map args) {
        findMethod.invoke(persistentClass, "find", [query, params, args] as Object[])
    }

    @Override
    List findAll(String query) {
        findAllMethod.invoke(persistentClass, "findAll", [query] as Object[])
    }

    @Override
    List findAll(String query, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args] as Object[])
    }

    @Override
    List findAll(String query, Map params, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params, args] as Object[])
    }

    @Override
    List findAll(String query, Collection params) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params] as Object[])
    }

    @Override
    List findAll(String query, Collection params, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params, args] as Object[])
    }

    @Override
    Object create() {
        return super.create()    //To change body of overridden methods use File | Settings | File Templates.
    }
}

class HibernateGormValidationApi extends GormValidationApi {

    ValidatePersistentMethod validateMethod
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader()

    HibernateGormValidationApi(Class persistentClass, HibernateDatastore datastore) {
        super(persistentClass, datastore);

        def sessionFactory = datastore.getSessionFactory()

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext) {
            org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def validator = mappingContext.getEntityValidator(mappingContext.getPersistentEntity(persistentClass.name))
            this.validateMethod = new ValidatePersistentMethod(sessionFactory,
                                                               classLoader,
                                                               grailsApplication,
                                                               validator)
        }
    }

    @Override
    boolean validate(Object instance) {
        if (validateMethod != null) {
            return validateMethod.invoke(instance, "validate", [] as Object[])
        }
        return super.validate(instance);
    }

    @Override
    boolean validate(Object instance, boolean evict) {
        if (validateMethod != null) {
            return validateMethod.invoke(instance, "validate", [evict] as Object[])
        }
        return super.validate(instance, evict);
    }

    @Override
    boolean validate(Object instance, Map arguments) {
        if (validateMethod != null) {
            return validateMethod.invoke(instance, "validate", [arguments] as Object[])
        }
        return super.validate(instance, arguments);
    }

    @Override
    boolean validate(Object instance, List fields) {
        if (validateMethod != null) {
            return validateMethod.invoke(instance, "validate", [fields] as Object[])
        }
        return super.validate(instance, arguments);
    }
}

/**
 * The implementation of the GORM instance API contract for Hibernate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class HibernateGormInstanceApi extends GormInstanceApi {
    private static final EMPTY_ARRAY = [] as Object[]

    private SavePersistentMethod saveMethod
    private MergePersistentMethod mergeMethod
    private HibernateTemplate hibernateTemplate
    private SessionFactory sessionFactory
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader()

    private config = Collections.emptyMap()

    HibernateGormInstanceApi(Class persistentClass, HibernateDatastore datastore) {
        super(persistentClass, datastore);

        sessionFactory = datastore.getSessionFactory()

        hibernateTemplate = new HibernateTemplate(sessionFactory)

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext) {
            org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            this.config = grailsApplication.config?.grails?.gorm
            this.saveMethod = new SavePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass)
            this.mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass)
        }
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return True if the field is dirty
     */
    boolean isDirty(Object instance, String fieldName) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex in dirtyProperties
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return True if it is dirty
     */
    boolean isDirty(Object instance) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        def dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        return dirtyProperties != null
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    List getDirtyPropertyNames(Object instance) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry) {
            return []
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        def names = []
        for (index in dirtyProperties) {
            names << entry.persister.propertyNames[index]
        }
        names
    }

    /**
     * Gets the original persisted value of a field
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    Object getPersistentValue(Object instance, String fieldName) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session, false)
        if (!entry) {
            return null
        }

        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex == -1 ? null : entry.loadedState[fieldIndex]
    }

    @Override
    Object lock(Object instance) {
        hibernateTemplate.lock(instance, LockMode.UPGRADE)
    }

    @Override
    Object refresh(Object instance) {
        hibernateTemplate.refresh(instance)
        return instance
    }

    @Override
    Object save(Object instance) {
        if (saveMethod != null) {
            return saveMethod.invoke(instance, "save", EMPTY_ARRAY)
        }
        return super.save(instance);
    }

    Object save(Object instance, boolean validate) {
        if (saveMethod != null) {
            return saveMethod.invoke(instance, "save", [validate] as Object[])
        }
        return super.save(instance);
    }

    @Override
    Object merge(Object instance) {
        if (mergeMethod != null) {
            mergeMethod.invoke(instance, "merge", EMPTY_ARRAY)
        }
        else {
            return super.merge(instance);
        }
    }

    @Override
    Object merge(Object instance, Map params) {
        if (mergeMethod != null) {
            mergeMethod.invoke(instance, "merge", [params] as Object[])
        }
        else {
            return super.merge(instance, params);
        }
    }

    @Override
    Object save(Object instance, Map params) {
        if (saveMethod != null) {
            return saveMethod.invoke(instance, "save", [params] as Object[])
        }
        return super.save(instance, params);
    }

    @Override
    Object attach(Object instance) {
        hibernateTemplate.lock(instance, LockMode.NONE)
        return instance
    }

    @Override
    void discard(Object instance) {
        hibernateTemplate.evict instance
    }

    @Override
    void delete(Object instance) {
        def obj = instance
        try {
            hibernateTemplate.execute({Session session ->
                session.delete obj
                if (shouldFlush()) {
                    session.flush()
                }
            } as HibernateCallback)
        }
        catch (DataAccessException e) {
            handleDataAccessException(hibernateTemplate, e)
        }
    }

    @Override
    void delete(Object instance, Map params) {
        def obj = instance
        hibernateTemplate.delete obj
        if (shouldFlush(params)) {
            try {
                hibernateTemplate.flush()
            }
            catch (DataAccessException e) {
                handleDataAccessException(hibernateTemplate, e)
            }
        }
    }

    @Override
    boolean instanceOf(Object instance, Class cls) {
        if (instance instanceof HibernateProxy) {
            def o = GrailsHibernateUtil.unwrapProxy(instance)
            return cls.isInstance(o)
        }
        return cls.isInstance(instance)
    }

    @Override
    boolean isAttached(Object instance) {
        hibernateTemplate.contains instance
    }

    private EntityEntry findEntityEntry(instance, SessionImplementor session, boolean forDirtyCheck = true) {
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
   private void handleDataAccessException(HibernateTemplate template, DataAccessException e) {
       try {
           template.execute({Session session ->
               session.setFlushMode(FlushMode.MANUAL)
           } as HibernateCallback)
       }
       finally {
           throw e
       }
   }

   private boolean shouldFlush(Map map = [:]) {
       def shouldFlush

       if (map?.containsKey('flush')) {
           shouldFlush = Boolean.TRUE == map.flush
       } else {
           shouldFlush = config.autoFlush instanceof Boolean ? config.autoFlush : false
       }
       return shouldFlush
   }
}
