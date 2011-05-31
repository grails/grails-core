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
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator;
import org.springframework.validation.Errors;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * Utility methods used in configuring the Grails Hibernate integration.
 *
 * @author Graeme Rocher
 */
public class GrailsDomainConfigurationUtil {

    public static final String PROPERTY_NAME = "constraints";

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public static Serializable getAssociationIdentifier(Object target, String propertyName,
            GrailsDomainClass referencedDomainClass) {

        String getterName = GrailsClassUtils.getGetterName(propertyName);

        try {
            Method m = target.getClass().getDeclaredMethod(getterName, EMPTY_CLASS_ARRAY);
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
                if (prop != null && prop.isAssociation()) {

                    GrailsDomainClass referenced = prop.getReferencedDomainClass();
                    if (referenced != null) {
                        boolean isOwnedBy = referenced.isOwningClass(domainClass.getClazz());
                        prop.setOwningSide(isOwnedBy);
                        String refPropertyName = null;
                        try {
                            refPropertyName = prop.getReferencedPropertyName();
                        }
                        catch (UnsupportedOperationException e) {
                            // ignore (to support Hibernate entities)
                        }
                        if (!StringUtils.isBlank(refPropertyName)) {
                            GrailsDomainClassProperty otherSide = referenced.getPropertyByName(refPropertyName);
                            prop.setOtherSide(otherSide);
                            otherSide.setOtherSide(prop);
                        }
                        else {
                            GrailsDomainClassProperty[] referencedProperties = referenced.getPersistentProperties();
                            for (GrailsDomainClassProperty referencedProp : referencedProperties) {
                                // for bi-directional circular dependencies we don't want the other side
                                // to be equal to self
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
            associationMap = Collections.EMPTY_MAP;
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
            return Collections.EMPTY_MAP;
        }
        return mappedByMap;
    }

    /**
     * Establish whether it's a basic type.
     *
     * @param prop The domain class property
     * @return True if it is basic
     */
    public static boolean isBasicType(GrailsDomainClassProperty prop) {
        if (prop == null) return false;
        return isBasicType(prop.getType());
    }

    private static final Set<String> BASIC_TYPES;
    static {
        Set<String> basics = new HashSet<String>(Arrays.asList(
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
                URL.class.getName()));
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
     * @return True if it is configurational
     */
    public static boolean isNotConfigurational(PropertyDescriptor descriptor) {
        final String name = descriptor.getName();
        return !Errors.class.isAssignableFrom(descriptor.getPropertyType()) &&
               !name.equals(GrailsDomainClassProperty.META_CLASS) &&
               !name.equals(GrailsDomainClassProperty.CLASS) &&
               !name.equals(GrailsDomainClassProperty.TRANSIENT) &&
               !name.equals(GrailsDomainClassProperty.RELATES_TO_MANY) &&
               !name.equals(GrailsDomainClassProperty.HAS_MANY) &&
               !name.equals(GrailsDomainClassProperty.EVANESCENT) &&
               !name.equals(GrailsDomainClassProperty.CONSTRAINTS) &&
               !name.equals(GrailsDomainClassProperty.MAPPING_STRATEGY) &&
               !name.equals(GrailsDomainClassProperty.MAPPED_BY) &&
               !name.equals(GrailsDomainClassProperty.BELONGS_TO);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param instance   The instance to evaluate constraints for
     * @param properties The properties of the instance
     * @param defaultConstraints A map that defines the default constraints
     *
     * @return A Map of constraints
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(Object instance,
            GrailsDomainClassProperty[] properties, Map<String, Object> defaultConstraints) {
        final Class<?> theClass = instance.getClass();
        return new DefaultConstraintEvaluator(defaultConstraints).evaluate(theClass, properties);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param theClass  The domain class to evaluate constraints for
     * @param properties The properties of the instance
     * @param defaultConstraints A map that defines the default constraints
     *
     * @return A Map of constraints
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(final Class<?> theClass,
            GrailsDomainClassProperty[] properties, Map<String, Object> defaultConstraints) {
        return new DefaultConstraintEvaluator(defaultConstraints).evaluate(theClass, properties);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints.
     *
     * @param instance   The instance to evaluate constraints for
     * @param properties The properties of the instance
     * @return A Map of constraints
     *          When the bean cannot be introspected
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(Object instance, GrailsDomainClassProperty[] properties)  {
        return evaluateConstraints(instance, properties,null);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints.
     *
     * @param instance   The instance to evaluate constraints for
     * @return A Map of constraints
     *          When the bean cannot be introspected
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(Object instance)  {
        return evaluateConstraints(instance, null, null);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param theClass  The class to evaluate constraints for
     * @return A Map of constraints
     *          When the bean cannot be introspected
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(Class<?> theClass)  {
        return evaluateConstraints(theClass, null, null);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints.
     *
     * @param theClass  The class to evaluate constraints for
     * @return A Map of constraints
     *          When the bean cannot be introspected
     *
     * @deprecated Use {@link DefaultConstraintEvaluator} instead
     */
    @Deprecated
    public static Map<String, ConstrainedProperty> evaluateConstraints(Class<?> theClass, GrailsDomainClassProperty[] properties)  {
        return evaluateConstraints(theClass, properties, null);
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
