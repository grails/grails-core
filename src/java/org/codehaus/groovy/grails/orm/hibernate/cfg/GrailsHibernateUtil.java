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

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
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
import org.hibernate.type.AbstractComponentType;
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
    public static SimpleTypeConverter converter = new SimpleTypeConverter();
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
    public static void configureHibernateDomainClasses(SessionFactory sessionFactory, GrailsApplication application) {
        Map<String, GrailsDomainClass> hibernateDomainClassMap = new HashMap<String, GrailsDomainClass>();
        ArtefactHandler artefactHandler = application.getArtefactHandler(DomainClassArtefactHandler.TYPE);
        Map defaultContraints = Collections.emptyMap();
        if (artefactHandler instanceof DomainClassArtefactHandler) {
            final Map map = ((DomainClassArtefactHandler) artefactHandler).getDefaultConstraints();
            if (map != null) {
                defaultContraints = map;
            }
        }
        for (Object o : sessionFactory.getAllClassMetadata().values()) {
            ClassMetadata classMetadata = (ClassMetadata) o;
            configureDomainClass(sessionFactory, application, classMetadata,
                    classMetadata.getMappedClass(EntityMode.POJO),
                    hibernateDomainClassMap, defaultContraints);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void configureDomainClass(SessionFactory sessionFactory, GrailsApplication application,
            ClassMetadata cmd, Class<?> persistentClass, Map<String, GrailsDomainClass> hibernateDomainClassMap,
            Map defaultContraints) {

        if (Modifier.isAbstract(persistentClass.getModifiers())) {
            return;
        }

        LOG.trace("Configuring domain class [" + persistentClass + "]");
        GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.getName());
        if (dc == null) {
            // a patch to add inheritance to this system
            GrailsHibernateDomainClass ghdc = new GrailsHibernateDomainClass(
                    persistentClass, sessionFactory, application, cmd, defaultContraints);

            hibernateDomainClassMap.put(persistentClass.getName(), ghdc);
            dc = (GrailsDomainClass) application.addArtefact(DomainClassArtefactHandler.TYPE, ghdc);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(Class<?> targetClass, Criteria c, Map argMap) {
        Integer maxParam = null;
        Integer offsetParam = null;
        if (argMap.containsKey(ARGUMENT_MAX)) {
            maxParam = converter.convertIfNecessary(argMap.get(ARGUMENT_MAX),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_OFFSET)) {
            offsetParam = converter.convertIfNecessary(argMap.get(ARGUMENT_OFFSET),Integer.class);
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
            c.setLockMode(LockMode.UPGRADE);
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
            addOrder(c, sort, order, ignoreCase);
        }
        else {
            Mapping m = GrailsDomainBinder.getMapping(targetClass);
            if (m != null && !StringUtils.isBlank(m.getSort())) {
                if (ORDER_DESC.equalsIgnoreCase(m.getOrder())) {
                    c.addOrder(Order.desc(m.getSort()));
                }
                else {
                    c.addOrder(Order.asc(m.getSort()));
                }
            }
        }
    }
    
    /*
     * Add order to criteria, creating necessary subCriteria if nested sort property (ie. sort:'nested.property').
     */
    private static void addOrder(Criteria c, String sort, String order, boolean ignoreCase) {
    	int firstDotPos = sort.indexOf(".");
    	if (firstDotPos == -1) {
            if (ORDER_DESC.equals(order)) {
                c.addOrder( ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
            }
            else {
                c.addOrder( ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort) );
            }
    	} else {
    		Criteria subCriteria = c.createCriteria(sort.substring(0,firstDotPos));
    		String remainingSort = sort.substring(firstDotPos+1);
    		addOrder(subCriteria, remainingSort, order, ignoreCase); // Recurse on nested sort
    	}
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
        populateArgumentsForCriteria(null, c, argMap);
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
        Object o = ConfigurationHolder.getFlatConfig().get(CONFIG_PROPERTY_CACHE_QUERIES);
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
                            (AbstractComponentType) persistentClass.getIdentifier().getType() :
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
