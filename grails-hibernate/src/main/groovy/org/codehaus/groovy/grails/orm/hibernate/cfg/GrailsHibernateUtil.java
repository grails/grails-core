/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import grails.util.GrailsWebUtil;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CompositeType;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Utility methods for configuring Hibernate inside Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHibernateUtil {
    private static final Log LOG = LogFactory.getLog(GrailsHibernateUtil.class);

    private static final String DYNAMIC_FILTER_ENABLER = "dynamicFilterEnabler";

    public static SimpleTypeConverter converter = new SimpleTypeConverter();
    public static final String ARGUMENT_FETCH_SIZE = "fetchSize";
    public static final String ARGUMENT_TIMEOUT = "timeout";
    public static final String ARGUMENT_READ_ONLY = "readOnly";
    public static final String ARGUMENT_FLUSH_MODE = "flushMode";
    public static final String ARGUMENT_MAX = "max";
    public static final String ARGUMENT_OFFSET = "offset";
    public static final String ARGUMENT_ORDER = "order";
    public static final String ARGUMENT_SORT = "sort";
    public static final String ORDER_DESC = "desc";
    public static final String ORDER_ASC = "asc";
    public static final String ARGUMENT_FETCH = "fetch";
    public static final String ARGUMENT_IGNORE_CASE = "ignoreCase";
    public static final String ARGUMENT_CACHE = "cache";
    public static final String ARGUMENT_LOCK = "lock";
    public static final String CONFIG_PROPERTY_CACHE_QUERIES="grails.hibernate.cache.queries";
    public static final Class<?>[] EMPTY_CLASS_ARRAY=new Class<?>[0];

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    @SuppressWarnings("rawtypes")
    public static void enableDynamicFilterEnablerIfPresent(SessionFactory sessionFactory, Session session) {
        if (sessionFactory != null && session != null) {
            final Set definedFilterNames = sessionFactory.getDefinedFilterNames();
            if (definedFilterNames != null && definedFilterNames.contains(DYNAMIC_FILTER_ENABLER))
                session.enableFilter(DYNAMIC_FILTER_ENABLER); // work around for HHH-2624
        }
    }

    public static void configureHibernateDomainClasses(SessionFactory sessionFactory, GrailsApplication application) {
        Map<String, GrailsDomainClass> hibernateDomainClassMap = new HashMap<String, GrailsDomainClass>();
        for (Object o : sessionFactory.getAllClassMetadata().values()) {
            ClassMetadata classMetadata = (ClassMetadata) o;
            configureDomainClass(sessionFactory, application, classMetadata,
                    classMetadata.getMappedClass(EntityMode.POJO),
                    hibernateDomainClassMap);
        }
        configureInheritanceMappings(hibernateDomainClassMap);
    }

    @SuppressWarnings("rawtypes")
    public static void configureInheritanceMappings(Map hibernateDomainClassMap) {
        // now get through all domainclasses, and add all subclasses to root class
        for (Object o : hibernateDomainClassMap.values()) {
            GrailsDomainClass baseClass = (GrailsDomainClass) o;
            if (!baseClass.isRoot()) {
                Class<?> superClass = baseClass.getClazz().getSuperclass();

                while (!superClass.equals(Object.class) && !superClass.equals(GroovyObject.class)) {
                    GrailsDomainClass gdc = (GrailsDomainClass) hibernateDomainClassMap.get(superClass.getName());

                    if (gdc == null || gdc.getSubClasses() == null) {
                        LOG.debug("did not find superclass names when mapping inheritance....");
                        break;
                    }
                    gdc.getSubClasses().add(baseClass);
                    superClass = superClass.getSuperclass();
                }
            }
        }
    }

    private static void configureDomainClass(SessionFactory sessionFactory, GrailsApplication application,
                                             ClassMetadata cmd, Class<?> persistentClass, Map<String, GrailsDomainClass> hibernateDomainClassMap) {

        if (Modifier.isAbstract(persistentClass.getModifiers())) {
            return;
        }

        LOG.trace("Configuring domain class [" + persistentClass + "]");
        GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.getName());
        if (dc == null) {
            // a patch to add inheritance to this system
            GrailsHibernateDomainClass ghdc = new GrailsHibernateDomainClass(
                    persistentClass, sessionFactory, application, cmd);

            hibernateDomainClassMap.put(persistentClass.getName(), ghdc);
            dc = (GrailsDomainClass) application.addArtefact(DomainClassArtefactHandler.TYPE, ghdc);
        }
    }

    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param grailsApplication the GrailsApplication instance
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     *

     */
    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(GrailsApplication grailsApplication, Class<?> targetClass, Criteria c, Map argMap) {
        Integer maxParam = null;
        Integer offsetParam = null;
        if (argMap.containsKey(ARGUMENT_MAX)) {
            maxParam = converter.convertIfNecessary(argMap.get(ARGUMENT_MAX),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_OFFSET)) {
            offsetParam = converter.convertIfNecessary(argMap.get(ARGUMENT_OFFSET),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_FETCH_SIZE)) {
            c.setFetchSize(converter.convertIfNecessary(argMap.get(ARGUMENT_FETCH_SIZE),Integer.class));
        }
        if (argMap.containsKey(ARGUMENT_TIMEOUT)) {
            c.setTimeout(converter.convertIfNecessary(argMap.get(ARGUMENT_TIMEOUT),Integer.class));
        }
        if (argMap.containsKey(ARGUMENT_FLUSH_MODE)) {
            c.setFlushMode(converter.convertIfNecessary(argMap.get(ARGUMENT_FLUSH_MODE),FlushMode.class));
        }
        if (argMap.containsKey(ARGUMENT_READ_ONLY)) {
            c.setReadOnly(GrailsClassUtils.getBooleanFromMap(ARGUMENT_READ_ONLY, argMap));
        }
        String orderParam = (String)argMap.get(ARGUMENT_ORDER);
        Object fetchObj = argMap.get(ARGUMENT_FETCH);
        if (fetchObj instanceof Map) {
            Map fetch = (Map)fetchObj;
            for (Object o : fetch.keySet()) {
                String associationName = (String) o;
                c.setFetchMode(associationName, getFetchMode(fetch.get(associationName)));
            }
        }

        final String sort = (String)argMap.get(ARGUMENT_SORT);
        final String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC;
        final int max = maxParam == null ? -1 : maxParam;
        final int offset = offsetParam == null ? -1 : offsetParam;
        if (max > -1) {
            c.setMaxResults(max);
        }
        if (offset > -1) {
            c.setFirstResult(offset);
        }
        if (GrailsClassUtils.getBooleanFromMap(ARGUMENT_CACHE, argMap)) {
            c.setCacheable(true);
        }
        if (GrailsClassUtils.getBooleanFromMap(ARGUMENT_LOCK, argMap)) {
            c.setLockMode(LockMode.PESSIMISTIC_WRITE);
        }
        else {
            if (argMap.get(ARGUMENT_CACHE) == null) {
                cacheCriteriaByMapping(targetClass, c);
            }
        }
        if (sort != null) {
            boolean ignoreCase = true;
            Object caseArg = argMap.get(ARGUMENT_IGNORE_CASE);
            if (caseArg instanceof Boolean) {
                ignoreCase = (Boolean) caseArg;
            }
            addOrderPossiblyNested(grailsApplication,c, targetClass, sort, order, ignoreCase);
        }
        else {
            Mapping m = GrailsDomainBinder.getMapping(targetClass);
            if (m != null && !StringUtils.isBlank(m.getSort())) {
                addOrderPossiblyNested(grailsApplication, c, targetClass, m.getSort(), m.getOrder(), true);
            }
        }
    }
    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     *
     * @deprecated Use {@link #populateArgumentsForCriteria(org.codehaus.groovy.grails.commons.GrailsApplication, Class, org.hibernate.Criteria, java.util.Map)} instead
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(Class<?> targetClass, Criteria c, Map argMap) {
        populateArgumentsForCriteria(null, targetClass, c, argMap);
    }

    /**
     * Add order to criteria, creating necessary subCriteria if nested sort property (ie. sort:'nested.property').
     */
    private static void addOrderPossiblyNested(GrailsApplication grailsApplication, Criteria c, Class<?> targetClass, String sort, String order, boolean ignoreCase) {
        int firstDotPos = sort.indexOf(".");
        if (firstDotPos == -1) {
            addOrder(c, sort, order, ignoreCase);
        } else { // nested property
            String sortHead = sort.substring(0,firstDotPos);
            String sortTail = sort.substring(firstDotPos+1);
            GrailsDomainClassProperty property = getGrailsDomainClassProperty(grailsApplication, targetClass, sortHead);
            if (property.isEmbedded()) {
                // embedded objects cannot reference entities (at time of writing), so no more recursion needed
                addOrder(c, sort, order, ignoreCase);
            } else {
                Criteria subCriteria = c.createCriteria(sortHead);
                Class<?> propertyTargetClass = property.getReferencedDomainClass().getClazz();
                addOrderPossiblyNested(grailsApplication, subCriteria, propertyTargetClass, sortTail, order, ignoreCase); // Recurse on nested sort
            }
        }
    }

    /**
     * Add order directly to criteria.
     */
    private static void addOrder(Criteria c, String sort, String order, boolean ignoreCase) {
        if (ORDER_DESC.equals(order)) {
            c.addOrder(ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
        }
        else {
            c.addOrder(ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort));
        }
    }

    /**
     * Get hold of the GrailsDomainClassProperty represented by the targetClass' propertyName,
     * assuming targetClass corresponds to a GrailsDomainClass.
     */
    private static GrailsDomainClassProperty getGrailsDomainClassProperty(GrailsApplication grailsApplication, Class<?> targetClass, String propertyName) {
        GrailsClass grailsClass = grailsApplication != null ? grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, targetClass.getName()) : null;
        if (!(grailsClass instanceof GrailsDomainClass)) {
            throw new IllegalArgumentException("Unexpected: class is not a domain class:"+targetClass.getName());
        }
        GrailsDomainClass domainClass = (GrailsDomainClass) grailsClass;
        return domainClass.getPropertyByName(propertyName);
    }

    /**
     * Configures the criteria instance to cache based on the configured mapping.
     *
     * @param targetClass The target class
     * @param criteria The criteria
     */
    public static void cacheCriteriaByMapping(Class<?> targetClass, Criteria criteria) {
        Mapping m = GrailsDomainBinder.getMapping(targetClass);
        if (m != null && m.getCache() != null && m.getCache().getEnabled()) {
            criteria.setCacheable(true);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(Criteria c, Map argMap) {
        populateArgumentsForCriteria(null,null, c, argMap);
    }

    /**
     * Retrieves the fetch mode for the specified instance; otherwise returns the default FetchMode.
     *
     * @param object The object, converted to a string
     * @return The FetchMode
     */
    public static FetchMode getFetchMode(Object object) {
        String name = object != null ? object.toString() : "default";
        if (name.equalsIgnoreCase(FetchMode.JOIN.toString()) || name.equalsIgnoreCase("eager")) {
            return FetchMode.JOIN;
        }
        if (name.equalsIgnoreCase(FetchMode.SELECT.toString()) || name.equalsIgnoreCase("lazy")) {
            return FetchMode.SELECT;
        }
        return FetchMode.DEFAULT;
    }

    /**
     * Sets the target object to read-only using the given SessionFactory instance. This
     * avoids Hibernate performing any dirty checking on the object
     *
     * @see #setObjectToReadWrite(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadyOnly(Object target, SessionFactory sessionFactory) {
        Session session = sessionFactory.getCurrentSession();
        if (canModifyReadWriteState(session, target)) {
            if (target instanceof HibernateProxy) {
                target = ((HibernateProxy)target).getHibernateLazyInitializer().getImplementation();
            }
            session.setReadOnly(target, true);
            session.setFlushMode(FlushMode.MANUAL);
        }
    }

    private static boolean canModifyReadWriteState(Session session, Object target) {
        return session.contains(target) && Hibernate.isInitialized(target);
    }

    /**
     * Sets the target object to read-write, allowing Hibernate to dirty check it and auto-flush changes.
     *
     * @see #setObjectToReadyOnly(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadWrite(final Object target, SessionFactory sessionFactory) {
        HibernateTemplate template = new HibernateTemplate(sessionFactory);
        template.setExposeNativeSession(true);
        template.execute(new HibernateCallback<Void>() {
            public Void doInHibernate(Session session) throws HibernateException, SQLException {
                if (canModifyReadWriteState(session, target)) {
                    SessionImplementor sessionImpl = (SessionImplementor) session;
                    EntityEntry ee = sessionImpl.getPersistenceContext().getEntry(target);

                    if (ee != null && ee.getStatus() == Status.READ_ONLY) {
                        Object actualTarget = target;
                        if (target instanceof HibernateProxy) {
                            actualTarget = ((HibernateProxy)target).getHibernateLazyInitializer().getImplementation();
                        }

                        session.setReadOnly(actualTarget, false);
                        session.setFlushMode(FlushMode.AUTO);
                        incrementVersion(target);
                    }
                }
                return null;
            }
        });
    }

    /**
     * Increments the entities version number in order to force an update
     * @param target The target entity
     */
    public static void incrementVersion(Object target) {
        MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
        if (metaClass.hasProperty(target, GrailsDomainClassProperty.VERSION)!=null) {
            Object version = metaClass.getProperty(target, GrailsDomainClassProperty.VERSION);
            if (version instanceof Long) {
                Long newVersion = (Long) version + 1;
                metaClass.setProperty(target, GrailsDomainClassProperty.VERSION, newVersion);
            }
        }
    }

    /**
     * Ensures the meta class is correct for a given class
     *
     * @param target The GroovyObject
     * @param persistentClass The persistent class
     */
    public static void ensureCorrectGroovyMetaClass(Object target, Class<?> persistentClass) {
        if (target instanceof GroovyObject) {
            GroovyObject go = ((GroovyObject)target);
            if (!go.getMetaClass().getTheClass().equals(persistentClass)) {
                go.setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(persistentClass));
            }
        }
    }

    /**
     * Unwraps and initializes a HibernateProxy.
     * @param proxy The proxy
     * @return the unproxied instance
     */
    public static Object unwrapProxy(HibernateProxy proxy) {
        return proxyHandler.unwrapProxy(proxy);
    }

    /**
     * Returns the proxy for a given association or null if it is not proxied
     *
     * @param obj The object
     * @param associationName The named assoication
     * @return A proxy
     */
    public static HibernateProxy getAssociationProxy(Object obj, String associationName) {
        return proxyHandler.getAssociationProxy(obj, associationName);
    }

    /**
     * Checks whether an associated property is initialized and returns true if it is
     *
     * @param obj The name of the object
     * @param associationName The name of the association
     * @return True if is initialized
     */
    public static boolean isInitialized(Object obj, String associationName) {
        return proxyHandler.isInitialized(obj, associationName);
    }

    public static boolean isCacheQueriesByDefault() {
        Object o = GrailsWebUtil.currentFlatConfiguration().get(CONFIG_PROPERTY_CACHE_QUERIES);
        return (o != null && o instanceof Boolean)?((Boolean)o).booleanValue():false;
    }

    @SuppressWarnings("serial")
    public static GroovyAwareJavassistProxyFactory buildProxyFactory(PersistentClass persistentClass) {
        GroovyAwareJavassistProxyFactory proxyFactory = new GroovyAwareJavassistProxyFactory();

        Set<Class<?>> proxyInterfaces = new HashSet<Class<?>>() {{
            add(HibernateProxy.class);
        }};

        final Class<?> javaClass = persistentClass.getMappedClass();
        final Property identifierProperty = persistentClass.getIdentifierProperty();
        final Getter idGetter = identifierProperty!=null?  identifierProperty.getGetter(javaClass) : null;
        final Setter idSetter =identifierProperty!=null? identifierProperty.getSetter(javaClass) : null;

        if (idGetter == null ||  idSetter==null) return null;

        try {
            proxyFactory.postInstantiate(persistentClass.getEntityName(), javaClass, proxyInterfaces,
                    idGetter.getMethod(), idSetter.getMethod(),
                    persistentClass.hasEmbeddedIdentifier() ?
                            (CompositeType) persistentClass.getIdentifier().getType() :
                            null);
        }
        catch (HibernateException e) {
            LOG.warn("Cannot instantiate proxy factory: " + e.getMessage());
            return null;
        }

        return proxyFactory;
    }

    public static Object unwrapIfProxy(Object instance) {
        return proxyHandler.unwrapIfProxy(instance);
    }
}
