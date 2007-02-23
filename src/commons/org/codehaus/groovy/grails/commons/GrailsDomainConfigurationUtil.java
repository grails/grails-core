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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringUtils;
import org.hibernate.type.TypeFactory;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Utility methods used in configuring the Grails Hibernate integration
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsDomainConfigurationUtil {


    /**
     * Configures the relationships between domain classes after they have been all loaded.
     * 
     * @param domainClasses The domain classes to configure relationships for
     * @param domainMap The domain class map
     */
    public static void configureDomainClassRelationships(GrailsClass[] domainClasses, Map domainMap) {

    	// configure super/sub class relationships
    	// and configure how domain class properties reference each other
        for (int i = 0; i < domainClasses.length; i++) {
        	GrailsDomainClass domainClass = (GrailsDomainClass) domainClasses[i];
        	if(!domainClass.isRoot()) {
        		Class superClass = domainClasses[i].getClazz().getSuperclass();
        		while(!superClass.equals(Object.class)&&!superClass.equals(GroovyObject.class)) {
            		GrailsDomainClass gdc = (GrailsDomainClass)domainMap.get(superClass.getName());
                    if (gdc == null || gdc.getSubClasses()==null)
                        break;
                    
                    gdc.getSubClasses().add(domainClasses[i]);
            		superClass = superClass.getSuperclass();
        		}
        	}        	
            GrailsDomainClassProperty[] props = domainClass.getPersistantProperties();

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
            GrailsDomainClass domainClass = (GrailsDomainClass) domainClasses[i];
            GrailsDomainClassProperty[] props = domainClass.getPersistantProperties();

            for (int j = 0; j < props.length; j++) {
                if(props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referenced = prop.getReferencedDomainClass();
                    if(referenced != null) {
                        String refPropertyName = null;
                        try {
                            refPropertyName = prop.getReferencedPropertyName();
                        } catch (UnsupportedOperationException e) {
                            // ignore (to support Hibernate entities)
                        }
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
