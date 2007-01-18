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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import groovy.lang.GroovyObject;

import java.beans.IntrospectionException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.metaclass.AddRelatedDynamicMethod;
import org.codehaus.groovy.grails.metaclass.AddToRelatedDynamicMethod;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.TypeFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Utility methods used in configuring the Grails Hibernate integration
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsDomainConfigurationUtil {
    private static final Log LOG = LogFactory.getLog(GrailsDomainConfigurationUtil.class);

    /**
     * Configures the relationships between domain classes after they have been all loaded.
     * 
     * @param domainClasses
     * @param domainMap
     */
    public static void configureDomainClassRelationships(GrailsDomainClass[] domainClasses, Map domainMap) {

    	// configure super/sub class relationships
    	// and configure how domain class properties reference each other
        for (int i = 0; i < domainClasses.length; i++) {
        	
        	if(!domainClasses[i].isRoot()) {
        		Class superClass = domainClasses[i].getClazz().getSuperclass();
        		while(!superClass.equals(Object.class)&&!superClass.equals(GroovyObject.class)) {
            		GrailsDomainClass gdc = (GrailsDomainClass)domainMap.get(superClass.getName());
                    if (gdc == null || gdc.getSubClasses()==null)
                        break;
                    
                    gdc.getSubClasses().add(domainClasses[i]);
            		superClass = superClass.getSuperclass();
        		}
        	}        	
            GrailsDomainClassProperty[] props = domainClasses[i].getPersistantProperties();

            for (int j = 0; j < props.length; j++) {
                if(props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referencedGrailsDomainClass = (GrailsDomainClass)domainMap.get( props[j].getReferencedPropertyType().getName() );
                    prop.setReferencedDomainClass(referencedGrailsDomainClass);

                }
            }

        }

        // now configure so that the 'other side' of a property can be resolved by the property itself
        for (int i = 0; i < domainClasses.length; i++) {
            GrailsDomainClassProperty[] props = domainClasses[i].getPersistantProperties();

            for (int j = 0; j < props.length; j++) {
                if(props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referenced = prop.getReferencedDomainClass();
                    if(referenced != null) {
                    	String refPropertyName = prop.getReferencedPropertyName();
                    	if(!StringUtils.isBlank(refPropertyName)) {
                    		prop.setOtherSide(referenced.getPropertyByName(refPropertyName));
                    	}
                    	else {
                            GrailsDomainClassProperty[] referencedProperties =  referenced.getPersistantProperties();
                            for (int k = 0; k < referencedProperties.length; k++) {
                            	// for bi-directional circular dependencies we don't want the other side 
                            	// to be equal to self 
                            	if(prop.equals(referencedProperties[k]) && prop.isBidirectional())
                            		continue;
                                if(domainClasses[i].getClazz().equals(referencedProperties[k].getReferencedPropertyType())) {
                                    prop.setOtherSide(referencedProperties[k]);
                                    break;
                                }
                            }                    		
                    	}                    
                    }
                }
            }

        }
    }
    /**
     * Configures dynamic methods on all Hibernate mapped domain classes that are found in the application context
     *
     * @param applicationContext The session factory instance
     * @param application The grails application instance
     */
    public static void configureDynamicMethods(ApplicationContext applicationContext, GrailsApplication application) {
    	if(applicationContext == null)
    		throw new IllegalArgumentException("Cannot configure dynamic methods for null ApplicationContext");
    	
    	SessionFactory sessionFactory = (SessionFactory)applicationContext.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN);
    	
        Collection dynamicMethods = configureDynamicMethods(sessionFactory, application);
        
        for (Iterator i = dynamicMethods.iterator(); i.hasNext();) {
			DomainClassMethods methods = (DomainClassMethods) i.next();
			boolean isTransactionAware = applicationContext.containsBean(GrailsRuntimeConfigurator.TRANSACTION_MANAGER_BEAN) &&
						   (methods instanceof TransactionManagerAware);
			if(isTransactionAware) {
				PlatformTransactionManager ptm = (PlatformTransactionManager)applicationContext.getBean(GrailsRuntimeConfigurator.TRANSACTION_MANAGER_BEAN);
				
				((TransactionManagerAware)methods).setTransactionManager(ptm);
			}
		}
    }
	public static Collection configureDynamicMethods(SessionFactory sessionFactory, GrailsApplication application) {
        // if its not a grails domain class and one written in java then add it
        // to grails
        Map hibernateDomainClassMap = new HashMap();
		Collection dynamicMethods = new ArrayList();
		Collection classMetaData = sessionFactory.getAllClassMetadata().values();
        for (Iterator i = classMetaData.iterator(); i.hasNext();) {
            ClassMetadata cmd = (ClassMetadata) i.next();

            Class persistentClass = cmd.getMappedClass(EntityMode.POJO);
            GrailsDomainClass dc = null;
            if(application != null && persistentClass != null) {
                dc = configureDomainClass(sessionFactory, application, cmd, persistentClass, hibernateDomainClassMap);            	
            }
            if(dc != null) {
            	dynamicMethods.add( configureDynamicMethodsFor(sessionFactory, application, persistentClass, dc) );                        	
            }
        }
        configureInheritanceMappings(hibernateDomainClassMap);  
        
        
        return dynamicMethods;
	}
	private static DynamicMethods configureDynamicMethodsFor(SessionFactory sessionFactory, GrailsApplication application, Class persistentClass, GrailsDomainClass dc) {
		LOG.debug("[GrailsDomainConfiguration] Registering dynamic methods on class ["+persistentClass+"]");
		DynamicMethods dm = null;
		try {
			dm = new DomainClassMethods(application,persistentClass,sessionFactory,application.getClassLoader());
		    dm.addDynamicMethodInvocation(new AddToRelatedDynamicMethod(dc));
		    for (int j = 0; j < dc.getPersistantProperties().length; j++) {
		          GrailsDomainClassProperty p = dc.getPersistantProperties()[j];
		          if(p.isOneToMany() || p.isManyToMany()) {
		              dm.addDynamicMethodInvocation(new AddRelatedDynamicMethod(p));                               
		          }
		    }
		} catch (IntrospectionException e) {
		    LOG.warn("[GrailsDomainConfiguration] Introspection exception registering dynamic methods for ["+persistentClass+"]:" + e.getMessage(), e);
		}
		return dm;
	}
	private static void configureInheritanceMappings(Map hibernateDomainClassMap) {
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
	
	private static GrailsDomainClass configureDomainClass(SessionFactory sessionFactory, GrailsApplication application, ClassMetadata cmd, Class persistentClass, Map hibernateDomainClassMap) {
		GrailsDomainClass dc = application.getGrailsDomainClass(persistentClass.getName());
		if( dc == null) {
			// a patch to add inheritance to this system
			GrailsHibernateDomainClass ghdc = new
				GrailsHibernateDomainClass(persistentClass, sessionFactory,cmd);
		
			hibernateDomainClassMap.put(persistentClass
											.getClass()
											.getName(),
										ghdc);
			
			dc = application.addDomainClass(ghdc);
		}
		return dc;

	}
    
    /**
     * Returns the ORM frameworks mapping file name for the specified class name
     * 
     * @param className
     * @return The mapping file name
     */
	public static String getMappingFileName(String className) {
		String fileName = className.replaceAll("\\.", "/");
		return fileName+=".hbm.xml";
	}
	/**
	 * Returns the association map for the specified domain class
	 * @param domainClass the domain class
	 * @return The association map
	 */
	public static Map getAssociationMap(Class domainClass) {
		Map associationMap =  (Map)GrailsClassUtils.getPropertyValueOfNewInstance( domainClass, GrailsDomainClassProperty.RELATES_TO_MANY, Map.class );
		if(associationMap == null) {
			associationMap = (Map)GrailsClassUtils.getPropertyValueOfNewInstance(domainClass, GrailsDomainClassProperty.HAS_MANY, Map.class);
			if(associationMap == null) {
				associationMap = Collections.EMPTY_MAP;
			}
		}
		return associationMap;
	}
	
	/**
	 * Retrieves the mappedBy map for the specified class
	 * @param domainClass The domain class
	 * @return The mappedBy map
	 */
	public static Map getMappedByMap(Class domainClass) {
		Map mappedByMap = (Map)GrailsClassUtils.getPropertyValueOfNewInstance(domainClass, GrailsDomainClassProperty.MAPPED_BY, Map.class);
		if(mappedByMap == null) {
			return Collections.EMPTY_MAP;
		}
		return mappedByMap;
	}	
	/**
	 * Establish whether its a basic type
	 * 
	 * @param prop The domain class property
	 * @return True if it is basic
	 */
	public static boolean isBasicType(GrailsDomainClassProperty prop) {
		if(prop == null)return false;
		Class propType = prop.getType();
		return TypeFactory.basic(propType.getName()) != null || 
										propType == URL.class || 
										propType == URI.class;
	}

	
	
}
