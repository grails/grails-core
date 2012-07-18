/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.DirtyCheckEventListener;
import org.hibernate.event.EventListeners;
import org.hibernate.event.EvictEventListener;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.InitializeCollectionEventListener;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.LockEventListener;
import org.hibernate.event.MergeEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreCollectionRecreateEventListener;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.RefreshEventListener;
import org.hibernate.event.ReplicateEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * A SessionFactory bean that allows the configuration class to
 * be changed and customise for usage within Grails.
 *
 * @author Graeme Rocher
 * @since 07-Jul-2005
 */
public class ConfigurableLocalSessionFactoryBean extends
        LocalSessionFactoryBean implements ApplicationContextAware {

    protected static final Log LOG = LogFactory.getLog(ConfigurableLocalSessionFactoryBean.class);
    protected ClassLoader classLoader;
    protected GrailsApplication grailsApplication;
    protected Class<?> configClass;
    protected Class<?> currentSessionContextClass;
    protected HibernateEventListeners hibernateEventListeners;
    protected ApplicationContext applicationContext;
    protected boolean proxyIfReloadEnabled = true;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;

    /**
     * @param proxyIfReloadEnabled Sets whether a proxy should be created if reload is enabled
     */
    public void setProxyIfReloadEnabled(boolean proxyIfReloadEnabled) {
        this.proxyIfReloadEnabled = proxyIfReloadEnabled;
    }

    /**
     * Sets class to be used for the Hibernate CurrentSessionContext.
     *
     * @param currentSessionContextClass An implementation of the CurrentSessionContext interface
     */
    public void setCurrentSessionContextClass(Class<?> currentSessionContextClass) {
        this.currentSessionContextClass = currentSessionContextClass;
    }

    /**
     * Sets the class to be used for Hibernate Configuration.
     * @param configClass A subclass of the Hibernate Configuration class
     */
    public void setConfigClass(Class<?> configClass) {
        this.configClass = configClass;
    }

    /**
     * @return Returns the grailsApplication.
     */
    public GrailsApplication getGrailsApplication() {
        return grailsApplication;
    }

    /**
     * @param grailsApplication The grailsApplication to set.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    /**
     * Overrides default behaviour to allow for a configurable configuration class.
     */
     @Override
    protected Configuration newConfiguration() {
        ClassLoader cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        if (configClass == null) {
            try {
                configClass = cl.loadClass("org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration");
            }
            catch (Throwable e) {
                // probably not Java 5 or missing some annotation jars, use default
                configClass = DefaultGrailsDomainConfiguration.class;
            }
        }
        Object config = BeanUtils.instantiateClass(configClass);
        if (config instanceof GrailsDomainConfiguration) {
            GrailsDomainConfiguration grailsConfig = (GrailsDomainConfiguration)config;
            grailsConfig.setGrailsApplication(grailsApplication);
            grailsConfig.setSessionFactoryBeanName(sessionFactoryBeanName);
            grailsConfig.setDataSourceName(dataSourceName);
        }
        if (currentSessionContextClass != null) {
            ((Configuration)config).setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, currentSessionContextClass.getName());
            // don't allow Spring's LocaalSessionFactoryBean to override setting
            setExposeTransactionAwareSessionFactory(false);
        }
        return (Configuration)config;
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        classLoader = beanClassLoader;
        super.setBeanClassLoader(beanClassLoader);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            super.afterPropertiesSet();
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

    @Override
    protected SessionFactory newSessionFactory(Configuration configuration) throws HibernateException {
        try {

            SessionFactory sf = super.newSessionFactory(configuration);

            if (!grails.util.Environment.getCurrent().isReloadEnabled() || !proxyIfReloadEnabled) {
                return sf;
            }

            // if reloading is enabled in this environment then we need to use a SessionFactoryProxy instance
            SessionFactoryProxy sfp = new SessionFactoryProxy();
            String suffix = dataSourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE) ? "" : '_' + dataSourceName;
            SessionFactoryHolder sessionFactoryHolder = applicationContext.getBean(
                    SessionFactoryHolder.BEAN_ID + suffix, SessionFactoryHolder.class);
            sessionFactoryHolder.setSessionFactory(sf);
            sfp.setApplicationContext(applicationContext);
            sfp.setCurrentSessionContextClass(currentSessionContextClass);
            sfp.setTargetBean(SessionFactoryHolder.BEAN_ID + suffix);
            sfp.afterPropertiesSet();
            return sfp;
        }
        catch (HibernateException e) {
            Throwable cause = e.getCause();
            if (isCacheConfigurationError(cause)) {
                LOG.fatal("There was an error configuring the Hibernate second level cache: " + getCauseMessage(e));
                LOG.fatal("This is normally due to one of two reasons. Either you have incorrectly specified the cache provider class name in [DataSource.groovy] or you do not have the cache provider on your classpath (eg. runtime (\"net.sf.ehcache:ehcache:1.6.1\"))");
                if (grails.util.Environment.isDevelopmentMode()) {
                    System.exit(1);
                }
            }
            throw e;
        }
    }

    protected String getCauseMessage(HibernateException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause.getMessage();
    }

    protected boolean isCacheConfigurationError(Throwable cause) {
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause != null && (cause instanceof CacheException);
    }

    @Override
    public void destroy() throws HibernateException {
        if (grailsApplication.isWarDeployed()) {
            MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
            Map<?, ?> classMetaData = getSessionFactory().getAllClassMetadata();
            for (Object o : classMetaData.values()) {
                ClassMetadata classMetadata = (ClassMetadata) o;
                Class<?> mappedClass = classMetadata.getMappedClass(EntityMode.POJO);
                registry.removeMetaClass(mappedClass);
            }
        }

        try {
            super.destroy();
        } catch (HibernateException e) {
            if (e.getCause() instanceof NameNotFoundException) {
                LOG.debug(e.getCause().getMessage(), e);
            }
            else {
                throw e;
            }
        }
    }

    @Override
    protected void postProcessConfiguration(Configuration config) throws HibernateException {
        EventListeners listeners = config.getEventListeners();
        if (hibernateEventListeners != null && hibernateEventListeners.getListenerMap() != null) {
            Map<String,Object> listenerMap = hibernateEventListeners.getListenerMap();
            addNewListenerToConfiguration(config, "auto-flush", AutoFlushEventListener.class,
                    listeners.getAutoFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "merge", MergeEventListener.class,
                    listeners.getMergeEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "create", PersistEventListener.class,
                    listeners.getPersistEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "create-onflush", PersistEventListener.class,
                    listeners.getPersistOnFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "delete", DeleteEventListener.class,
                    listeners.getDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "dirty-check", DirtyCheckEventListener.class,
                    listeners.getDirtyCheckEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "evict", EvictEventListener.class,
                    listeners.getEvictEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "flush", FlushEventListener.class,
                    listeners.getFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "flush-entity", FlushEntityEventListener.class,
                    listeners.getFlushEntityEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "load", LoadEventListener.class,
                    listeners.getLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "load-collection", InitializeCollectionEventListener.class,
                    listeners.getInitializeCollectionEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "lock", LockEventListener.class,
                    listeners.getLockEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "refresh", RefreshEventListener.class,
                    listeners.getRefreshEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "replicate", ReplicateEventListener.class,
                    listeners.getReplicateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "save-update", SaveOrUpdateEventListener.class,
                    listeners.getSaveOrUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "save", SaveOrUpdateEventListener.class,
                    listeners.getSaveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "update", SaveOrUpdateEventListener.class,
                    listeners.getUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-load", PreLoadEventListener.class,
                    listeners.getPreLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-update", PreUpdateEventListener.class,
                    listeners.getPreUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-delete", PreDeleteEventListener.class,
                    listeners.getPreDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-insert", PreInsertEventListener.class,
                    listeners.getPreInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-recreate", PreCollectionRecreateEventListener.class,
                    listeners.getPreCollectionRecreateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-remove", PreCollectionRemoveEventListener.class,
                    listeners.getPreCollectionRemoveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-update", PreCollectionUpdateEventListener.class,
                    listeners.getPreCollectionUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-load", PostLoadEventListener.class,
                    listeners.getPostLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-update", PostUpdateEventListener.class,
                    listeners.getPostUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-delete", PostDeleteEventListener.class,
                    listeners.getPostDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-insert", PostInsertEventListener.class,
                    listeners.getPostInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-update", PostUpdateEventListener.class,
                    listeners.getPostCommitUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-delete", PostDeleteEventListener.class,
                    listeners.getPostCommitDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-insert", PostInsertEventListener.class,
                    listeners.getPostCommitInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-recreate", PostCollectionRecreateEventListener.class,
                    listeners.getPostCollectionRecreateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-remove", PostCollectionRemoveEventListener.class,
                    listeners.getPostCollectionRemoveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-update", PostCollectionUpdateEventListener.class,
                    listeners.getPostCollectionUpdateEventListeners(), listenerMap);
        }
        // register workaround for GRAILS-8988 (do nullability checks for inserts in last PreInsertEventListener)
        ClosureEventTriggeringInterceptor.addNullabilityCheckerPreInsertEventListener(listeners);
    }

    @SuppressWarnings("unchecked")
    protected <T> void addNewListenerToConfiguration(final Configuration config, final String listenerType,
            final Class<? extends T> klass, final T[] currentListeners, final Map<String,Object> newlistenerMap) {

        Object newListener = newlistenerMap.get(listenerType);
        if (newListener == null) return;

        if (currentListeners != null && currentListeners.length > 0) {
            T[] newListeners = (T[])Array.newInstance(klass, currentListeners.length + 1);
            System.arraycopy(currentListeners, 0, newListeners, 0, currentListeners.length);
            newListeners[currentListeners.length] = (T)newListener;
            config.setListeners(listenerType, newListeners);
        }
        else {
            config.setListener(listenerType, newListener);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setHibernateEventListeners(final HibernateEventListeners listeners) {
        hibernateEventListeners = listeners;
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }
}
