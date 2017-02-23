/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.core.support;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.util.CollectionUtils;
import grails.util.GrailsClassUtils;
import groovy.lang.GroovyObject;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import grails.validation.Constrained;
import org.grails.core.util.ClassPropertyFetcher;
import org.grails.core.io.support.GrailsFactoriesLoader;
import grails.validation.ConstraintsEvaluator;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

/**
 * Utility methods used in configuring the Grails Hibernate integration.
 *
 * @author Graeme Rocher
 * @deprecated Use the {@link org.grails.datastore.mapping.model.MappingContext} and {@link org.grails.datastore.mapping.model.MappingFactory} APIs instead
 */
@Deprecated
public class GrailsDomainConfigurationUtil {

    public static final String PROPERTY_NAME = "constraints";

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final String PROPERTIES_PROPERTY = "properties";

    public static Serializable getAssociationIdentifier(Object target, String propertyName,
            GrailsDomainClass referencedDomainClass) {

        String getterName = GrailsClassUtils.getGetterName(propertyName);

        try {
            Method m = target.getClass().getMethod(getterName, EMPTY_CLASS_ARRAY);
            Object value = m.invoke(target);
            if (value != null && referencedDomainClass != null) {
                String identifierGetter = GrailsClassUtils.getGetterName(referencedDomainClass.getIdentifier().getName());
                m = value.getClass().getDeclaredMethod(identifierGetter, EMPTY_CLASS_ARRAY);
                return (Serializable)m.invoke(value);
            }
        }
        catch (NoSuchMethodException e) {
            // ignore
        }
        catch (IllegalAccessException e) {
            // ignore
        }
        catch (InvocationTargetException e) {
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
    public static void configureDomainClassRelationships(GrailsClass[] domainClasses, Map<?, ?> domainMap) {

        // configure super/sub class relationships
        // and configure how domain class properties reference each other
        for (GrailsClass grailsClass : domainClasses) {
            GrailsDomainClass domainClass = (GrailsDomainClass) grailsClass;
            if (!domainClass.isRoot()) {
                Class<?> superClass = grailsClass.getClazz().getSuperclass();
                while (!superClass.equals(Object.class) && !superClass.equals(GroovyObject.class)) {
                    GrailsDomainClass gdc = (GrailsDomainClass) domainMap.get(superClass.getName());
                    if (gdc == null || gdc.getSubClasses() == null) {
                        break;
                    }

                    gdc.getSubClasses().add((GrailsDomainClass)grailsClass);
                    superClass = superClass.getSuperclass();
                }
            }
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (GrailsDomainClassProperty prop : props) {
                if (prop != null && prop.isAssociation()) {
                    GrailsDomainClass referencedGrailsDomainClass = (GrailsDomainClass) domainMap.get(prop.getReferencedPropertyType().getName());
                    prop.setReferencedDomainClass(referencedGrailsDomainClass);
                }
            }
        }

        // now configure so that the 'other side' of a property can be resolved by the property itself
        for (GrailsClass domainClass1 : domainClasses) {
            GrailsDomainClass domainClass = (GrailsDomainClass) domainClass1;
            GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();

            for (GrailsDomainClassProperty prop : props) {
                if (prop == null || !prop.isAssociation()) {
                    continue;
                }

                GrailsDomainClass referenced = prop.getReferencedDomainClass();
                if (referenced == null) {
                    continue;
                }

                boolean isOwnedBy = referenced.isOwningClass(domainClass.getClazz());
                prop.setOwningSide(isOwnedBy);
                String refPropertyName = null;
                try {
                    refPropertyName = prop.getReferencedPropertyName();
                }
                catch (UnsupportedOperationException e) {
                    // ignore (to support Hibernate entities)
                }

                if (!StringUtils.hasText(refPropertyName)) {
                    GrailsDomainClassProperty[] referencedProperties = referenced.getPersistentProperties();
                    for (GrailsDomainClassProperty referencedProp : referencedProperties) {
                        // for bi-directional circular dependencies we don't want the other side
                        // to be equal to self
                        if (prop.equals(referencedProp) && prop.isBidirectional()) {
                            continue;
                        }
                        if (isCandidateForOtherSide(domainClass, prop, referencedProp)) {
                            prop.setOtherSide(referencedProp);
                            break;
                        }
                    }
                }
                else {
                    GrailsDomainClassProperty otherSide = referenced.getPropertyByName(refPropertyName);
                    prop.setOtherSide(otherSide);
                    otherSide.setOtherSide(prop);
                }
            }
        }
    }

    private static boolean isCandidateForOtherSide(GrailsDomainClass domainClass,
            GrailsDomainClassProperty prop, GrailsDomainClassProperty referencedProp) {

        if (prop.equals(referencedProp)) return false;
        if (prop.isOneToMany() && referencedProp.isOneToMany()) return false;

        Class<?> referencedPropertyType = referencedProp.getReferencedPropertyType();
        if (referencedPropertyType == null || !referencedPropertyType.isAssignableFrom(domainClass.getClazz())) {
            return false;
        }

        Map<?, ?> mappedBy = domainClass.getMappedBy();

        Object propertyMapping = mappedBy.get(prop.getName());
        boolean mappedToDifferentProperty = propertyMapping != null && !propertyMapping.equals(referencedProp.getName());

        mappedBy = referencedProp.getDomainClass().getMappedBy();
        propertyMapping = mappedBy.get(referencedProp.getName());
        boolean mappedFromDifferentProperty = propertyMapping != null && !propertyMapping.equals(prop.getName());

        return !mappedToDifferentProperty && !mappedFromDifferentProperty;
    }

    /**
     * Returns the ORM framework's mapping file name for the specified class name.
     *
     * @param className The class name of the mapped file
     * @return The mapping file name
     */
    public static String getMappingFileName(String className) {
        return className.replaceAll("\\.", "/") + ".hbm.xml";
    }

    /**
     * Returns the association map for the specified domain class
     *
     * @param domainClass the domain class
     * @return The association map
     */
    public static Map<?, ?> getAssociationMap(Class<?> domainClass) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(domainClass);

        Map<?, ?> associationMap = cpf.getPropertyValue(GrailsDomainClassProperty.HAS_MANY, Map.class);
        if (associationMap == null) {
            associationMap = Collections.emptyMap();
        }
        return associationMap;
    }

    /**
     * Retrieves the mappedBy map for the specified class.
     *
     * @param domainClass The domain class
     * @return The mappedBy map
     */
    public static Map<?, ?> getMappedByMap(Class<?> domainClass) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(domainClass);

        Map<?, ?> mappedByMap = cpf.getPropertyValue(GrailsDomainClassProperty.MAPPED_BY, Map.class);
        if (mappedByMap == null) {
            return Collections.emptyMap();
        }
        return mappedByMap;
    }

    /**
     * Establish whether it's a basic type.
     *
     * @param prop The domain class property
     * @return true if it is basic
     */
    public static boolean isBasicType(GrailsDomainClassProperty prop) {
        return prop == null ? false : isBasicType(prop.getType());
    }

    private static final Set<String> BASIC_TYPES;
    static {
        Set<String> basics = CollectionUtils.newSet(
                boolean.class.getName(),
                long.class.getName(),
                short.class.getName(),
                int.class.getName(),
                byte.class.getName(),
                float.class.getName(),
                double.class.getName(),
                char.class.getName(),
                Boolean.class.getName(),
                Long.class.getName(),
                Short.class.getName(),
                Integer.class.getName(),
                Byte.class.getName(),
                Float.class.getName(),
                Double.class.getName(),
                Character.class.getName(),
                String.class.getName(),
                java.util.Date.class.getName(),
                Time.class.getName(),
                Timestamp.class.getName(),
                java.sql.Date.class.getName(),
                BigDecimal.class.getName(),
                BigInteger.class.getName(),
                Locale.class.getName(),
                Calendar.class.getName(),
                GregorianCalendar.class.getName(),
                java.util.Currency.class.getName(),
                TimeZone.class.getName(),
                Object.class.getName(),
                Class.class.getName(),
                byte[].class.getName(),
                Byte[].class.getName(),
                char[].class.getName(),
                Character[].class.getName(),
                Blob.class.getName(),
                Clob.class.getName(),
                Serializable.class.getName(),
                URI.class.getName(),
                URL.class.getName());
        BASIC_TYPES = Collections.unmodifiableSet(basics);
    }

    public static boolean isBasicType(Class<?> propType) {
        if (propType == null) return false;
        if (propType.isArray()) {
            return isBasicType(propType.getComponentType());
        }
        return BASIC_TYPES.contains(propType.getName());
    }

    /**
     * Checks whether is property is configurational.
     *
     * @param descriptor The descriptor
     * @return true if it is configurational
     */
    public static boolean isNotConfigurational(PropertyDescriptor descriptor) {
        final String name = descriptor.getName();
        Method readMethod = descriptor.getReadMethod();
        Method writeMethod = descriptor.getWriteMethod();

        if ((readMethod != null && Modifier.isStatic(readMethod.getModifiers()) ||
               (writeMethod != null && Modifier.isStatic(writeMethod.getModifiers())))) {
            return false;
        }

        return !Errors.class.isAssignableFrom(descriptor.getPropertyType())
                && isNotConfigurational(name);
    }

    private static final Set<String> CONFIGURATIONAL_PROPERTIES;
    static {
        Set<String> configurational = CollectionUtils.newSet(
                GrailsDomainClassProperty.META_CLASS,
                GrailsDomainClassProperty.CLASS, 
                GrailsDomainClassProperty.TRANSIENT,
                GrailsDomainClassProperty.ATTACHED, 
                GrailsDomainClassProperty.DIRTY,
                GrailsDomainClassProperty.DIRTY_PROPERTY_NAMES, 
                GrailsDomainClassProperty.HAS_MANY,
                GrailsDomainClassProperty.CONSTRAINTS,
                GrailsDomainClassProperty.MAPPING_STRATEGY,
                GrailsDomainClassProperty.MAPPED_BY, 
                GrailsDomainClassProperty.BELONGS_TO,
                GrailsDomainClassProperty.ERRORS,
                GrailsApplication.TRANSACTION_MANAGER_BEAN,
                GrailsApplication.DATA_SOURCE_BEAN,
                GrailsApplication.SESSION_FACTORY_BEAN,
                GrailsApplication.MESSAGE_SOURCE_BEAN,
                "applicationContext",
                PROPERTIES_PROPERTY);
        CONFIGURATIONAL_PROPERTIES = Collections.unmodifiableSet(configurational);
    }

    public static boolean isConfigurational(String name) {
        return CONFIGURATIONAL_PROPERTIES.contains(name);
    }

    public static boolean isNotConfigurational(String name) {
        return !isConfigurational(name);
    }


    private static Map<String, Constrained> getConstraintMap(GrailsDomainClassProperty[] properties, Map<String, Object> defaultConstraints, Class<?> theClass) {
        ConstraintsEvaluator constraintsEvaluator = GrailsFactoriesLoader.loadFactory(ConstraintsEvaluator.class, defaultConstraints);
        if(constraintsEvaluator != null) {
            return constraintsEvaluator.evaluate(theClass, properties);
        }
        return Collections.emptyMap();
    }

    public static LinkedList<?> getSuperClassChain(Class<?> theClass) {
        LinkedList<Class<?>> classChain = new LinkedList<Class<?>>();
        Class<?> clazz = theClass;
        while (clazz != Object.class && clazz != null) {
            classChain.addFirst(clazz);
            clazz = clazz.getSuperclass();
        }
        return classChain;
    }
}
