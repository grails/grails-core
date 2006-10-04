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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.metaclass.AddRelatedDynamicMethod;
import org.codehaus.groovy.grails.metaclass.DomainClassMethods;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.TypeFactory;

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
                        GrailsDomainClassProperty[] referencedProperties =  referenced.getPersistantProperties();
                        for (int k = 0; k < referencedProperties.length; k++) {
                        	// for circular dependencies we don't want the other side 
                        	// to be equal to self 
                        	if(prop.equals(referencedProperties[k]))
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
    /**
     * Configures dynamic methods on all Hibernate mapped domain classes
     *
     * @param sf The session factory instance
     * @param application The grails application instance
     */
    public static void configureDynamicMethods(SessionFactory sf, GrailsApplication application) {
        Collection classMetaData = sf.getAllClassMetadata().values();
        for (Iterator i = classMetaData.iterator(); i.hasNext();) {
            ClassMetadata cmd = (ClassMetadata) i.next();

            Class persistentClass = cmd.getMappedClass(EntityMode.POJO);

            // if its not a grails domain class and one written in java then add it
            // to grails
            Map hibernateDomainClassMap = new HashMap();
            
            if(application != null && persistentClass != null) {
                GrailsDomainClass dc = application.getGrailsDomainClass(persistentClass.getName());
                if( dc == null) {
                	// a patch to add inheritance to this system
                	GrailsHibernateDomainClass ghdc = new
                		GrailsHibernateDomainClass(persistentClass, sf,cmd);
                
                	hibernateDomainClassMap.put(persistentClass
                									.getClass()
                									.getName(),
                								ghdc);
                	
                	dc = application.addDomainClass(ghdc);
	            }
	            LOG.info("[GrailsDomainConfiguration] Registering dynamic methods on class ["+persistentClass+"]");
	            try {
	                DynamicMethods dm = new DomainClassMethods(application,persistentClass,sf,application.getClassLoader());
                    for (int j = 0; j < dc.getPersistantProperties().length; j++) {
                          GrailsDomainClassProperty p = dc.getPersistantProperties()[j];
                          if(p.isOneToMany() || p.isManyToMany()) {
                              dm.addDynamicMethodInvocation(new AddRelatedDynamicMethod(p));
                          }
                    }
                } catch (IntrospectionException e) {
	                LOG.warn("[GrailsDomainConfiguration] Introspection exception registering dynamic methods for ["+persistentClass+"]:" + e.getMessage(), e);
	            }            	
            }
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
	 * Establish whether its a basic type
	 * 
	 * @param prop The domain class property
	 * @return True if it is basic
	 */
	public static boolean isBasicType(GrailsDomainClassProperty prop) {
		return TypeFactory.basic(prop.getType().getName()) != null;
	}
	
	
}
