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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

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
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (int j = 0; j < props.length; j++) {
                if(props[j] != null && props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referencedGrailsDomainClass = (GrailsDomainClass)domainMap.get( props[j].getReferencedPropertyType().getName() );
                    prop.setReferencedDomainClass(referencedGrailsDomainClass);

                }
            }

        }

        // now configure so that the 'other side' of a property can be resolved by the property itself
        for (int i = 0; i < domainClasses.length; i++) {
            GrailsDomainClass domainClass = (GrailsDomainClass) domainClasses[i];
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (int j = 0; j < props.length; j++) {
                if(props[j] != null && props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referenced = prop.getReferencedDomainClass();
                    if(referenced != null) {
                        boolean isOwnedBy = referenced.isOwningClass(domainClass.getClazz());
                        prop.setOwningSide(isOwnedBy);
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
                            GrailsDomainClassProperty[] referencedProperties =  referenced.getPersistentProperties();
                            for (int k = 0; k < referencedProperties.length; k++) {
                            	// for bi-directional circular dependencies we don't want the other side 
                            	// to be equal to self 
                                GrailsDomainClassProperty referencedProp = referencedProperties[k];
                                if(prop.equals(referencedProp) && prop.isBidirectional())
                            		continue;
                                if(isCandidateForOtherSide(domainClass, prop, referencedProp)) {
                                    prop.setOtherSide(referencedProp);
                                    break;
                                }
                            }                    		
                    	}                    
                    }
                }
            }

        }
    }

    private static boolean isCandidateForOtherSide(GrailsDomainClass domainClass, GrailsDomainClassProperty prop, GrailsDomainClassProperty referencedProp) {
        boolean isTypeCompatible = domainClass.getClazz().equals(referencedProp.getReferencedPropertyType());
        Map mappedBy = domainClass.getMappedBy();

        Object propertyMapping = mappedBy.get(prop.getName());
        return !(propertyMapping != null && !propertyMapping.equals(referencedProp.getName())) && isTypeCompatible;

    }

    /**
     * Returns the ORM frameworks mapping file name for the specified class name
     * 
     * @param className The class name of the mapped file
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
        return isBasicType(propType);
    }

    private static final Set BASIC_TYPES;

    static {
		Set basics = new HashSet();
		basics.add( boolean.class.getName());
		basics.add( long.class.getName());
		basics.add( short.class.getName());
		basics.add( int.class.getName());
		basics.add( byte.class.getName());
		basics.add( float.class.getName());
		basics.add( double.class.getName());
		basics.add( char.class.getName());
		basics.add( Boolean.class.getName());
		basics.add( Long.class.getName());
		basics.add( Short.class.getName());
		basics.add( Integer.class.getName());
		basics.add( Byte.class.getName());
		basics.add( Float.class.getName());
		basics.add( Double.class.getName());
		basics.add( Character.class.getName());
		basics.add( String.class.getName());
		basics.add( java.util.Date.class.getName());
		basics.add( Time.class.getName());
		basics.add( Timestamp.class.getName());
		basics.add( java.sql.Date.class.getName());
		basics.add( BigDecimal.class.getName());
		basics.add( BigInteger.class.getName());
		basics.add( Locale.class.getName());
		basics.add( Calendar.class.getName());
		basics.add( GregorianCalendar.class.getName());
		basics.add( java.util.Currency.class.getName());
		basics.add( TimeZone.class.getName());
		basics.add( Object.class.getName());
		basics.add( Class.class.getName());
		basics.add( byte[].class.getName());
		basics.add( Byte[].class.getName());
		basics.add( char[].class.getName());
		basics.add( Character[].class.getName());
		basics.add( Blob.class.getName());
		basics.add( Clob.class.getName());
		basics.add( Serializable.class.getName() );
        basics.add( URI.class.getName() );
        basics.add( URL.class.getName() );

        BASIC_TYPES = Collections.unmodifiableSet( basics );
	}

    public static boolean isBasicType(Class propType) {
        if(propType.isArray()) {
            return isBasicType(propType.getComponentType());
        }
        return BASIC_TYPES.contains(propType.getName());
    }


}
