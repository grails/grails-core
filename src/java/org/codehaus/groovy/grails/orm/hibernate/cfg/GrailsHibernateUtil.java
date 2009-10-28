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
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.hibernate.*;
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
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.AbstractComponentType;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class containing utility methods for configuring Hibernate inside Grails
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 6:21:01 PM
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
    public static final Class[] EMPTY_CLASS_ARRAY=new Class[0];


    public static void configureHibernateDomainClasses(SessionFactory sessionFactory, GrailsApplication application) {
        Map hibernateDomainClassMap = new HashMap();
        for (Object o : sessionFactory.getAllClassMetadata().values()) {
            ClassMetadata classMetadata = (ClassMetadata) o;
            configureDomainClass(sessionFactory, application, classMetadata, classMetadata.getMappedClass(EntityMode.POJO), hibernateDomainClassMap);
        }
        configureInheritanceMappings(hibernateDomainClassMap);
    }

    public static void configureInheritanceMappings(Map hibernateDomainClassMap) {
        // now get through all domainclasses, and add all subclasses to root class
        for (Object o : hibernateDomainClassMap.values()) {
            GrailsDomainClass baseClass = (GrailsDomainClass) o;
            if (!baseClass.isRoot()) {
                Class superClass = baseClass
                        .getClazz().getSuperclass();


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

    private static void configureDomainClass(SessionFactory sessionFactory, GrailsApplication application, ClassMetadata cmd, Class persistentClass, Map hibernateDomainClassMap) {
        if (!Modifier.isAbstract(persistentClass.getModifiers())) {
            LOG.trace("Configuring domain class [" + persistentClass + "]");
            GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.getName());
            if (dc == null) {
                // a patch to add inheritance to this system
                GrailsHibernateDomainClass ghdc = new
                        GrailsHibernateDomainClass(persistentClass, sessionFactory, cmd);

                hibernateDomainClassMap.put(persistentClass.getName(),
                        ghdc);

                dc = (GrailsDomainClass) application.addArtefact(DomainClassArtefactHandler.TYPE, ghdc);
            }
        }
    }

    public static void populateArgumentsForCriteria(Class targetClass, Criteria c, Map argMap) {
       Integer maxParam = null;
        Integer offsetParam = null;
        if(argMap.containsKey(ARGUMENT_MAX)) {
            maxParam = (Integer)converter.convertIfNecessary(argMap.get(ARGUMENT_MAX),Integer.class);
        }
        if(argMap.containsKey(ARGUMENT_OFFSET)) {
            offsetParam = (Integer)converter.convertIfNecessary(argMap.get(ARGUMENT_OFFSET),Integer.class);
        }
        String orderParam = (String)argMap.get(ARGUMENT_ORDER);
        Object fetchObj = argMap.get(ARGUMENT_FETCH);
        if(fetchObj instanceof Map) {
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
        if(max > -1)
            c.setMaxResults(max);
        if(offset > -1)
            c.setFirstResult(offset);
        if(GrailsClassUtils.getBooleanFromMap(ARGUMENT_CACHE, argMap)) {
            c.setCacheable(true);
        }
        if(GrailsClassUtils.getBooleanFromMap(ARGUMENT_LOCK, argMap)) {
            c.setLockMode(LockMode.UPGRADE);
        }
        else {
            if(argMap.get(ARGUMENT_CACHE) == null) {
                cacheCriteriaByMapping(targetClass, c);
            }
        }
        if(sort != null) {
            boolean ignoreCase = true;
            Object caseArg = argMap.get(ARGUMENT_IGNORE_CASE);
            if(caseArg instanceof Boolean) {
                ignoreCase = (Boolean) caseArg;
            }
            if(ORDER_DESC.equals(order)) {
                c.addOrder( ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
            }
            else {
                c.addOrder( ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort) );
            }
        }
        else {
            Mapping m = GrailsDomainBinder.getMapping(targetClass);
            if(m!=null&&!StringUtils.isBlank(m.getSort())) {
                if(ORDER_DESC.equalsIgnoreCase(m.getOrder())) {
                    c.addOrder(Order.desc(m.getSort()));
                }
                else {
                    c.addOrder(Order.asc(m.getSort()));
                }
            }
        }

    }

    /**
     * Configures the criteria instance to cache based on the configured mapping
     *
     * @param targetClass The target class
     * @param criteria The criteria
     */
    public static void cacheCriteriaByMapping(Class targetClass, Criteria criteria) {
        Mapping m = GrailsDomainBinder.getMapping(targetClass);
        if(m!=null && m.getCache()!=null) {
            if(m.getCache().getEnabled()) {
                criteria.setCacheable(true);
            }
        }
    }

    public static void populateArgumentsForCriteria(Criteria c, Map argMap) {
        populateArgumentsForCriteria(null,c, argMap);
    }

    /**
	 * Will retrieve the fetch mode for the specified instance other wise return the
     * default FetchMode
     *
     * @param object The object, converted to a string
     * @return The FetchMode
     */
    public static FetchMode getFetchMode(Object object) {
        String name = object != null ? object.toString() : "default";
        if(name.equalsIgnoreCase(FetchMode.JOIN.toString()) || name.equalsIgnoreCase("eager")) {
            return FetchMode.JOIN;
        }
        else if(name.equalsIgnoreCase(FetchMode.SELECT.toString()) || name.equalsIgnoreCase("lazy")) {
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
        if(canModifyReadWriteState(session, target)) {
             if(target instanceof HibernateProxy) {
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
     * Sets the target object to read-write, allowing Hibernate to dirty check it and auto-flush
     * changes
     *
     * @see #setObjectToReadyOnly(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadWrite(final Object target, SessionFactory sessionFactory) {
        HibernateTemplate template = new HibernateTemplate(sessionFactory);
        template.setExposeNativeSession(true);
        template.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                if(canModifyReadWriteState(session, target)) {
                    SessionImplementor sessionImpl = (SessionImplementor) session;
                    EntityEntry ee = sessionImpl.getPersistenceContext().getEntry(target);

                    if(ee != null && ee.getStatus() == Status.READ_ONLY) {
                        Object actualTarget = target;
                        if(target instanceof HibernateProxy) {
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
        if(metaClass.hasProperty(target, GrailsDomainClassProperty.VERSION)!=null) {
            Object version = metaClass.getProperty(target, GrailsDomainClassProperty.VERSION);
            if(version instanceof Long) {
                Long newVersion = (Long) version + 1;
                metaClass.setProperty(target, GrailsDomainClassProperty.VERSION, newVersion);
            }
        }
    }

    /**
     * Unwraps and initializes a HibernateProxy
     * @param proxy The proxy
     */
    public static Object unwrapProxy(HibernateProxy proxy) {
        LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
        if(lazyInitializer.isUninitialized()) {
            lazyInitializer.initialize();
        }
        return lazyInitializer.getImplementation();
    }


    /**
     * Returns the proxy for a given association or null if it is not proxied
     *
     * @param obj The object
     * @param associationName The named assoication
     * @return A proxy
     */
    public static HibernateProxy getAssociationProxy(Object obj, String associationName) {
        try {
            Object proxy = PropertyUtils.getProperty(obj, associationName);
            if(proxy instanceof HibernateProxy) return (HibernateProxy) proxy;
            else return null;
        }
        catch (IllegalAccessException e) {
            return null;
        }
        catch (InvocationTargetException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Checks whether an associated property is initialized and returns true if it is
     *
     * @param obj The name of the object
     * @param associationName The name of the association
     * @return True if is initialized
     */
    public static boolean isInitialized(Object obj, String associationName) {
        try {
            Object proxy = PropertyUtils.getProperty(obj, associationName);
            return Hibernate.isInitialized(proxy);
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static boolean isCacheQueriesByDefault() {
        Object o = ConfigurationHolder.getFlatConfig().get(CONFIG_PROPERTY_CACHE_QUERIES);
        return (o != null && o instanceof Boolean)?((Boolean)o).booleanValue():false;
    }

    public static GroovyAwareJavassistProxyFactory buildProxyFactory(PersistentClass persistentClass) {
        GroovyAwareJavassistProxyFactory proxyFactory = new GroovyAwareJavassistProxyFactory();


        Set<Class> proxyInterfaces = new HashSet<Class>() {{
            add(HibernateProxy.class);
          }
        };


        final Class javaClass = persistentClass.getMappedClass();
        final Property identifierProperty = persistentClass.getIdentifierProperty();
        final Getter idGetter = identifierProperty!=null?  identifierProperty.getGetter(javaClass) : null;
        final Setter idSetter =identifierProperty!=null? identifierProperty.getSetter(javaClass) : null;

        if(idGetter == null ||  idSetter==null) return null;
        try {
            proxyFactory.postInstantiate(persistentClass.getEntityName(),
                                    javaClass,
                                     proxyInterfaces,
                                     idGetter.getMethod(),
                                     idSetter.getMethod(),
                                     persistentClass.hasEmbeddedIdentifier() ?
                                                 (AbstractComponentType) persistentClass.getIdentifier().getType() :
                                                        null
                                            );
        }
        catch (HibernateException e) {

            LOG.warn("Cannot instantiate proxy factory: " + e.getMessage());
            return null;
        }

        return proxyFactory;
    }

    public static Object unwrapIfProxy(Object instance) {
        if(instance instanceof HibernateProxy) {
            return unwrapProxy((HibernateProxy)instance);
        }
        else {
            return instance;
        }
    }
}
