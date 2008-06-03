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

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.commons.metaclass.GroovyDynamicMethodsInterceptor;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility methods used in configuring the Grails Hibernate integration
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsDomainConfigurationUtil {


    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    public static Serializable getAssociationIdentifier(Object target, String propertyName, GrailsDomainClass referencedDomainClass) {
        String getterName = GrailsClassUtils.getGetterName(propertyName);

        try {
            Method m = target.getClass().getDeclaredMethod(getterName, EMPTY_CLASS_ARRAY);
            Object value = m.invoke(target, null);
            if(value != null && referencedDomainClass != null) {
                String identifierGetter = GrailsClassUtils.getGetterName(referencedDomainClass.getIdentifier().getName());
                m = value.getClass().getDeclaredMethod(identifierGetter, EMPTY_CLASS_ARRAY);
                return (Serializable)m.invoke(value, null);
            }
        } catch (NoSuchMethodException e) {
           // ignore
        } catch (IllegalAccessException e) {
           // ignore
        } catch (InvocationTargetException e) {
            // ignore
        }
        return null;
    }

    /**
     * Configures the relationships between domain classes after they have been all loaded.
     *
     * @param domainClasses The domain classes to configure relationships for
     * @param domainMap     The domain class map
     */
    public static void configureDomainClassRelationships(GrailsClass[] domainClasses, Map domainMap) {

        // configure super/sub class relationships
        // and configure how domain class properties reference each other
        for (int i = 0; i < domainClasses.length; i++) {
            GrailsDomainClass domainClass = (GrailsDomainClass) domainClasses[i];
            if (!domainClass.isRoot()) {
                Class superClass = domainClasses[i].getClazz().getSuperclass();
                while (!superClass.equals(Object.class) && !superClass.equals(GroovyObject.class)) {
                    GrailsDomainClass gdc = (GrailsDomainClass) domainMap.get(superClass.getName());
                    if (gdc == null || gdc.getSubClasses() == null)
                        break;

                    gdc.getSubClasses().add(domainClasses[i]);
                    superClass = superClass.getSuperclass();
                }
            }
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (int j = 0; j < props.length; j++) {
                if (props[j] != null && props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referencedGrailsDomainClass = (GrailsDomainClass) domainMap.get(props[j].getReferencedPropertyType().getName());
                    prop.setReferencedDomainClass(referencedGrailsDomainClass);

                }
            }

        }

        // now configure so that the 'other side' of a property can be resolved by the property itself
        for (int i = 0; i < domainClasses.length; i++) {
            GrailsDomainClass domainClass = (GrailsDomainClass) domainClasses[i];
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (int j = 0; j < props.length; j++) {
                if (props[j] != null && props[j].isAssociation()) {
                    GrailsDomainClassProperty prop = props[j];
                    GrailsDomainClass referenced = prop.getReferencedDomainClass();
                    if (referenced != null) {
                        boolean isOwnedBy = referenced.isOwningClass(domainClass.getClazz());
                        prop.setOwningSide(isOwnedBy);
                        String refPropertyName = null;
                        try {
                            refPropertyName = prop.getReferencedPropertyName();
                        } catch (UnsupportedOperationException e) {
                            // ignore (to support Hibernate entities)
                        }
                        if (!StringUtils.isBlank(refPropertyName)) {
                            GrailsDomainClassProperty otherSide = referenced.getPropertyByName(refPropertyName);
                            prop.setOtherSide(otherSide);
                            otherSide.setOtherSide(prop);
                        } else {
                            GrailsDomainClassProperty[] referencedProperties = referenced.getPersistentProperties();
                            for (int k = 0; k < referencedProperties.length; k++) {
                                // for bi-directional circular dependencies we don't want the other side
                                // to be equal to self
                                GrailsDomainClassProperty referencedProp = referencedProperties[k];
                                if (prop.equals(referencedProp) && prop.isBidirectional())
                                    continue;
                                if (isCandidateForOtherSide(domainClass, prop, referencedProp)) {
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

        if (prop.equals(referencedProp)) return false;
        if (prop.isOneToMany() && referencedProp.isOneToMany()) return false;
        Class referencedPropertyType = referencedProp.getReferencedPropertyType();
        if (referencedPropertyType == null || !referencedPropertyType.isAssignableFrom(domainClass.getClazz()))
            return false;
        Map mappedBy = domainClass.getMappedBy();

        Object propertyMapping = mappedBy.get(prop.getName());
        boolean mappedToDifferentProperty = propertyMapping != null && !propertyMapping.equals(referencedProp.getName());

        mappedBy = referencedProp.getDomainClass().getMappedBy();
        propertyMapping = mappedBy.get(referencedProp.getName());
        boolean mappedFromDifferentProperty = propertyMapping != null && !propertyMapping.equals(prop.getName());


        return !mappedToDifferentProperty && !mappedFromDifferentProperty;
    }

    /**
     * Returns the ORM frameworks mapping file name for the specified class name
     *
     * @param className The class name of the mapped file
     * @return The mapping file name
     */
    public static String getMappingFileName(String className) {
        String fileName = className.replaceAll("\\.", "/");
        return fileName += ".hbm.xml";
    }

    /**
     * Returns the association map for the specified domain class
     *
     * @param domainClass the domain class
     * @return The association map
     */
    public static Map getAssociationMap(Class domainClass) {
        Map associationMap = (Map) GrailsClassUtils.getPropertyValueOfNewInstance(domainClass, GrailsDomainClassProperty.RELATES_TO_MANY, Map.class);
        if (associationMap == null) {
            associationMap = (Map) GrailsClassUtils.getPropertyValueOfNewInstance(domainClass, GrailsDomainClassProperty.HAS_MANY, Map.class);
            if (associationMap == null) {
                associationMap = Collections.EMPTY_MAP;
            }
        }
        return associationMap;
    }

    /**
     * Retrieves the mappedBy map for the specified class
     *
     * @param domainClass The domain class
     * @return The mappedBy map
     */
    public static Map getMappedByMap(Class domainClass) {
        Map mappedByMap = (Map) GrailsClassUtils.getPropertyValueOfNewInstance(domainClass, GrailsDomainClassProperty.MAPPED_BY, Map.class);
        if (mappedByMap == null) {
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
        if (prop == null) return false;
        Class propType = prop.getType();
        return isBasicType(propType);
    }

    private static final Set BASIC_TYPES;

    static {
        Set basics = new HashSet();
        basics.add(boolean.class.getName());
        basics.add(long.class.getName());
        basics.add(short.class.getName());
        basics.add(int.class.getName());
        basics.add(byte.class.getName());
        basics.add(float.class.getName());
        basics.add(double.class.getName());
        basics.add(char.class.getName());
        basics.add(Boolean.class.getName());
        basics.add(Long.class.getName());
        basics.add(Short.class.getName());
        basics.add(Integer.class.getName());
        basics.add(Byte.class.getName());
        basics.add(Float.class.getName());
        basics.add(Double.class.getName());
        basics.add(Character.class.getName());
        basics.add(String.class.getName());
        basics.add(java.util.Date.class.getName());
        basics.add(Time.class.getName());
        basics.add(Timestamp.class.getName());
        basics.add(java.sql.Date.class.getName());
        basics.add(BigDecimal.class.getName());
        basics.add(BigInteger.class.getName());
        basics.add(Locale.class.getName());
        basics.add(Calendar.class.getName());
        basics.add(GregorianCalendar.class.getName());
        basics.add(java.util.Currency.class.getName());
        basics.add(TimeZone.class.getName());
        basics.add(Object.class.getName());
        basics.add(Class.class.getName());
        basics.add(byte[].class.getName());
        basics.add(Byte[].class.getName());
        basics.add(char[].class.getName());
        basics.add(Character[].class.getName());
        basics.add(Blob.class.getName());
        basics.add(Clob.class.getName());
        basics.add(Serializable.class.getName());
        basics.add(URI.class.getName());
        basics.add(URL.class.getName());

        BASIC_TYPES = Collections.unmodifiableSet(basics);
    }

    public static boolean isBasicType(Class propType) {
        if (propType.isArray()) {
            return isBasicType(propType.getComponentType());
        }
        return BASIC_TYPES.contains(propType.getName());
    }


    /**
     * Checks whether is property is configurational
     *
     * @param descriptor The descriptor
     * @return True if it is configurational
     */
    public static boolean isNotConfigurational(PropertyDescriptor descriptor) {
        return !descriptor.getName().equals(GrailsDomainClassProperty.META_CLASS) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.CLASS) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.TRANSIENT) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.RELATES_TO_MANY) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.HAS_MANY) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.EVANESCENT) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.CONSTRAINTS) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.MAPPING_STRATEGY) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.MAPPED_BY) &&
                !descriptor.getName().equals(GrailsDomainClassProperty.BELONGS_TO);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param instance   The instance to evaluate constraints for
     * @param properties The properties of the instance
     * @return A Map of constraints
     * @throws java.beans.IntrospectionException
     *          When the bean cannot be introspected
     */
    public static Map evaluateConstraints(Object instance, GrailsDomainClassProperty[] properties) throws IntrospectionException {
        Closure constraintsClosure = (Closure) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(instance, GrailsDomainClassProperty.CONSTRAINTS);
        Class objClass = instance.getClass();
        String fullName = objClass.getName();
        if (constraintsClosure != null && !GrailsClassUtils.isStaticProperty(objClass, GrailsDomainClassProperty.CONSTRAINTS)) {
            throw new GrailsConfigurationException(
                    "Domain class [" + fullName + "] has non-static constraints. Constraints must be " +
                            "declared static.");
        }


        GroovyObject go = (GroovyObject) instance;
        DynamicMethods interceptor = new GroovyDynamicMethodsInterceptor(go);
        interceptor.addDynamicProperty(new ConstraintsEvaluatingDynamicProperty(properties));

        return (Map) go.getProperty(GrailsDomainClassProperty.CONSTRAINTS);
    }
}
