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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.context.ApplicationContext;

import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    public static void configureDynamicMethods(SessionFactory sessionFactory, GrailsApplication application) {
        LOG.trace("Configuring dynamic methods");
        // if its not a grails domain class and one written in java then add it
        // to grails
		Collection classMetaData = sessionFactory.getAllClassMetadata().values();
        for (Iterator i = classMetaData.iterator(); i.hasNext();) {
            ClassMetadata cmd = (ClassMetadata) i.next();

            Class persistentClass = cmd.getMappedClass(EntityMode.POJO);
            configureDynamicMethodsFor(sessionFactory, application, persistentClass); 

        }
	}

    /**
     * Configures dynamic methods on all Hibernate mapped domain classes that are found in the application context
     *
     * @param applicationContext The session factory instance
     * @param application The grails application instance
     */
    public static void configureDynamicMethods(ApplicationContext applicationContext, GrailsApplication application) {
        LOG.trace("Configuring dynamic methods");
        if(applicationContext == null)
            throw new IllegalArgumentException("Cannot configure dynamic methods for null ApplicationContext");

        SessionFactory sessionFactory = (SessionFactory)applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN);

        configureDynamicMethods(sessionFactory, application);
    }

    public static DynamicMethods configureDynamicMethodsFor(SessionFactory sessionFactory, GrailsApplication application, Class persistentClass) {
		if (LOG.isTraceEnabled()) {
            LOG.trace("Registering dynamic methods on class ["+persistentClass+"]");
        }
		DynamicMethods dm = null;
		try {
			dm = new DomainClassMethods(application,persistentClass,sessionFactory,application.getClassLoader());
        } catch (IntrospectionException e) {
		    LOG.warn("Introspection exception registering dynamic methods for ["+persistentClass+"]:" + e.getMessage(), e);
		}
		return dm;
	}

    public static void configureHibernateDomainClasses(SessionFactory sessionFactory, GrailsApplication application) {
        Map hibernateDomainClassMap = new HashMap();
        for (Iterator i = sessionFactory.getAllClassMetadata().values().iterator(); i.hasNext();) {
            ClassMetadata classMetadata = (ClassMetadata) i.next();
            configureDomainClass(sessionFactory, application, classMetadata, classMetadata.getMappedClass(EntityMode.POJO),hibernateDomainClassMap);
        }
        configureInheritanceMappings(hibernateDomainClassMap);
    }
    public static void configureInheritanceMappings(Map hibernateDomainClassMap) {
        // now get through all domainclasses, and add all subclasses to root class
        for (Iterator it = hibernateDomainClassMap.values().iterator(); it.hasNext(); ) {
            GrailsDomainClass baseClass = (GrailsDomainClass) it.next();
            if (! baseClass.isRoot()) {
                Class superClass = baseClass
                                        .getClazz().getSuperclass();


            while(!superClass.equals(Object.class)&&!superClass.equals(GroovyObject.class)) {
               GrailsDomainClass gdc = (GrailsDomainClass)hibernateDomainClassMap.get(superClass.getName());

               if (gdc == null || gdc.getSubClasses()==null) {
                   LOG.error("did not find superclass names when mapping inheritance....");
                   break;
               }
               gdc.getSubClasses().add(baseClass);
               superClass = superClass.getSuperclass();
            }
        }
      }
    }

    public static GrailsDomainClass configureDomainClass(SessionFactory sessionFactory, GrailsApplication application, ClassMetadata cmd, Class persistentClass, Map hibernateDomainClassMap) {
        LOG.trace("Configuring domain class ["+persistentClass+"]");
        GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.getName());
		if( dc == null) {
			// a patch to add inheritance to this system
			GrailsHibernateDomainClass ghdc = new
				GrailsHibernateDomainClass(persistentClass, sessionFactory,cmd);

			hibernateDomainClassMap.put(persistentClass
											.getClass()
											.getName(),
										ghdc);

			dc = (GrailsDomainClass) application.addArtefact( DomainClassArtefactHandler.TYPE, ghdc);
		}
		return dc;

	}
}
