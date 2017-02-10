/*
 * Copyright (C) 2011 SpringSource
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
package org.grails.validation;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.io.IOUtils;
import grails.persistence.PersistenceMethod;
import grails.util.GrailsClassUtils;
import grails.validation.Constrained;
import grails.validation.ConstrainedProperty;
import grails.validation.ConstraintsEvaluator;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.grails.core.artefact.AnnotationDomainClassArtefactHandler;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.core.support.GrailsDomainConfigurationUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.CachedIntrospectionResults;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Default implementation of the {@link grails.validation.ConstraintsEvaluator} interface.
 *
 * TODO: Subclass this to add hibernate-specific exceptions!
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultConstraintEvaluator implements ConstraintsEvaluator, org.codehaus.groovy.grails.validation.ConstraintsEvaluator {

    private static final Log LOG = LogFactory.getLog(DefaultConstraintEvaluator.class);
    private final Map<String, Object> defaultConstraints;

    public DefaultConstraintEvaluator(Map<String, Object> defaultConstraints) {
        this.defaultConstraints = (defaultConstraints != null) ? defaultConstraints : Collections.<String, Object>emptyMap();
    }

    public DefaultConstraintEvaluator() {
        this(null);
    }

    @Override
    public Map<String, Object> getDefaultConstraints() {
        return defaultConstraints;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Constrained> evaluate(@SuppressWarnings("rawtypes") Class cls) {
        return evaluateConstraints(cls, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Constrained> evaluate(@SuppressWarnings("rawtypes") Class cls, boolean defaultNullable) {
        return evaluateConstraints(cls, null, defaultNullable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Constrained> evaluate(Class<?> cls, boolean defaultNullable, boolean useOnlyAdHocConstraints, Closure... adHocConstraintsClosures) {
        return evaluateConstraints(cls, null, defaultNullable, useOnlyAdHocConstraints, adHocConstraintsClosures);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Constrained> evaluate(GrailsDomainClass cls) {
        return evaluateConstraints(cls.getClazz(), cls.getPersistentProperties());
    }

    @Override
    public Map<String, Constrained> evaluate(Object object, GrailsDomainClassProperty[] properties) {
        return evaluateConstraints(object.getClass(), properties);
    }

    @Override
    public Map<String, Constrained> evaluate(Class<?> cls, GrailsDomainClassProperty[] properties) {
        return evaluateConstraints(cls, properties);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param theClass   The domain class to evaluate constraints for
     * @param domainClassProperties The properties of the instance
     * @return A Map of constraints
     */
    protected Map<String, Constrained> evaluateConstraints(final Class<?> theClass, GrailsDomainClassProperty[] domainClassProperties) {
        return evaluateConstraints(theClass, domainClassProperties, false);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param theClass        The domain class to evaluate constraints for
     * @param domainClassProperties      The properties of the instance
     * @param defaultNullable Indicates if properties are nullable by default
     * @return A Map of constraints
     */
    protected Map<String, Constrained> evaluateConstraints(final Class<?> theClass, GrailsDomainClassProperty[] domainClassProperties, boolean defaultNullable) {
        List<Closure> constraintsClosures = retrieveConstraintsClosures(theClass);
        Map<String, Constrained> constraintMap = evaluateConstraintsMap(constraintsClosures, theClass);
        return evaluateConstraints(constraintMap, theClass, domainClassProperties, defaultNullable);
    }

    protected Map<String, Constrained> evaluateConstraints(final Class<?> theClass, GrailsDomainClassProperty[] domainClassProperties, boolean defaultNullable, boolean useOnlyAdHocConstraints, Closure[] adHocConstraintsClosures) {
        List<Closure> constraintsClosures = new ArrayList<>();
        if (!useOnlyAdHocConstraints) {
            constraintsClosures.addAll(retrieveConstraintsClosures(theClass));
        }
        if (adHocConstraintsClosures != null) {
            constraintsClosures.addAll(Arrays.asList(adHocConstraintsClosures));
        }
        Map<String, Constrained> constraintMap = evaluateConstraintsMap(constraintsClosures, theClass);
        return evaluateConstraints(constraintMap, theClass, domainClassProperties, defaultNullable);
    }

    protected Map<String, Constrained> evaluateConstraints(Map<String, Constrained> constraintMap, final Class<?> theClass, GrailsDomainClassProperty[] domainClassProperties, boolean defaultNullable) {
        // nothing to do for JPA domain class without Grails's style constraints.
        if (constraintMap.isEmpty() && AnnotationDomainClassArtefactHandler.isJPADomainClass(theClass)) {
            return constraintMap;
        }

        boolean isDomainClass = DomainClassArtefactHandler.isDomainClass(theClass);
        Map<String, GrailsDomainClassProperty> domainClassPropertyMap = indexPropertiesByPropertyName(domainClassProperties);
        Map<String, Method> constrainablePropertyMap = getConstrainablePropertyMap(theClass, isDomainClass);

        for (String propertyName : constrainablePropertyMap.keySet()) {
            GrailsDomainClassProperty domainClassProperty = domainClassPropertyMap.get(propertyName);
            boolean isPersistentProperty = isDomainClass && domainClassProperty != null;

            // ignore the property which should not be validated for domain class.
            if (isDomainClass) {
                if (isAssociationIdProperty(propertyName, domainClassPropertyMap)) continue; // just ignore

                if (isPersistentProperty) {
                    if (!canPropertyBeConstrained(domainClassProperty)) continue; // just ignore

                    if (domainClassProperty.isDerived()) {
                        // remove the constraint given by user and warn
                        if (constraintMap.remove(propertyName) != null) {
                            LOG.warn("Derived domainClassProperties may not be constrained. Property [" + propertyName + "] of domain class " + theClass.getName() + " will not be checked during validation.");
                            continue;
                        }
                    }
                }
                else {
                    // for domain class, a constraint of a property as only getter method is not supported
                    continue;
                }
            }

            // complete constraints not defined by user.
            Constrained constrained = constraintMap.get(propertyName);
            if (constrained == null) {
                // if default is nullable:true and there is no defaultConstraints, it doesn't have to complete a ConstrainedProperty.
                if (defaultConstraints.isEmpty() && defaultNullable) continue;

                ConstrainedProperty constrainedProperty = new ConstrainedProperty(theClass, propertyName, constrainablePropertyMap.get(propertyName).getReturnType());
                constrainedProperty.setOrder(constraintMap.size() + 1);
                constraintMap.put(propertyName, constrainedProperty);
                constrained = constrainedProperty;
            }

            // apply default constraints '*'
            if (isPersistentProperty) {
                // TODO want to unify the two overload methods
                applyDefaultConstraints(domainClassProperty, constrained, defaultConstraints);
            } else {
                applyDefaultConstraints(propertyName, constrained);
            }

            // apply default nullable
            if (isPersistentProperty) {
                // TODO want to unify the two overload methods
                applyDefaultNullableConstraint(domainClassProperty, constrained); // default configuration for domain class is fixed as nullable:false
            } else {
                applyDefaultNullableConstraint(constrained, defaultNullable);
            }
        }
        return constraintMap;
    }

    protected List<Closure> retrieveConstraintsClosures(Class<?> theClass) {
        // Evaluate all the constraints closures in the inheritance chain
        List<Closure> constraintsClosureList = new ArrayList<>();
        for (Object aClassChain : GrailsDomainConfigurationUtil.getSuperClassChain(theClass)) {
            Class<?> clazz = (Class<?>) aClassChain;
            Closure<?> c = (Closure<?>) GrailsClassUtils.getStaticFieldValue(clazz, ConstraintsEvaluator.PROPERTY_NAME);
            if (c == null) {
                c = getConstraintsFromScript(theClass);
            }
            if (c != null) {
                constraintsClosureList.add(c);
            } else {
                LOG.debug("User-defined constraints not found on class [" + clazz + "], applying default constraints");
            }
        }
        return constraintsClosureList;
    }

    protected Map<String, Constrained> evaluateConstraintsMap(List<Closure> constraintsClosureList, Class<?> theClass) {
        // build constraintMap just specified by user.
        ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(theClass);
        for (Closure<?> c : constraintsClosureList) {
            c = (Closure<?>) c.clone();
            c.setResolveStrategy(Closure.DELEGATE_ONLY);
            c.setDelegate(delegate);
            c.call();
        }
        Map<String, Constrained> constraintMap = delegate.getConstrainedProperties();

        // resolve shared constraints
        applySharedConstraints(delegate, constraintMap);
        return constraintMap;
    }


    protected boolean isAssociationIdProperty(String propertyName, Map<String, GrailsDomainClassProperty> domainClassPropertyMap) {
        boolean isPersistentProperty = (domainClassPropertyMap.get(propertyName) != null);
        if (isPersistentProperty) {
            return false;
        }
        if (propertyName.endsWith("Id")) {
            GrailsDomainClassProperty associationProperty = domainClassPropertyMap.get(propertyName.replaceFirst("Id$", ""));
            if (associationProperty != null && associationProperty.isAssociation()) {
                return true;
            }
        }
        return false;
    }

    protected Map<String, GrailsDomainClassProperty> indexPropertiesByPropertyName(GrailsDomainClassProperty[] properties) {
        Map<String, GrailsDomainClassProperty> indexed = new HashMap<>();
        if (properties != null) {
            for (GrailsDomainClassProperty p : properties) {
                indexed.put(p.getName(), p);
            }
        }
        return indexed;
    }

    protected void applySharedConstraints(ConstrainedPropertyBuilder constrainedPropertyBuilder, Map<String, Constrained> constraintMap) {
        for (Map.Entry<String, Constrained> entry : constraintMap.entrySet()) {
            String propertyName = entry.getKey();
            Constrained constrained = entry.getValue();
            String sharedConstraintReference = constrainedPropertyBuilder.getSharedConstraint(propertyName);
            if (sharedConstraintReference != null) {
                Object o = defaultConstraints.get(sharedConstraintReference);
                if (o instanceof Map) {
                    @SuppressWarnings({"unchecked", "rawtypes"}) Map<String, Object> constraintsWithinSharedConstraint = (Map) o;
                    for (Map.Entry<String, Object> e : constraintsWithinSharedConstraint.entrySet()) {
                        constrained.applyConstraint(e.getKey(), e.getValue());
                    }
                } else {
                    throw new GrailsConfigurationException("Property [" + constrained.getOwner().getName() + '.' + propertyName + "] references shared constraint [" + sharedConstraintReference + ":" + o + "], which doesn't exist!");
                }
            }
        }
    }

    protected boolean canPropertyBeConstrained(GrailsDomainClassProperty property) {
        return true;
    }

    protected Closure<?> getConstraintsFromScript(Class<?> theClass) {
        // Fallback to xxxxConstraints.groovy script for Java domain classes
        String className = theClass.getName();
        String constraintsScript = className.replaceAll("\\.", "/") + ConstraintsEvaluator.CONSTRAINTS_GROOVY_SCRIPT;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(constraintsScript);

        if (stream != null) {
            GroovyClassLoader gcl = new GroovyClassLoader();
            try {
                Class<?> scriptClass = gcl.parseClass(IOUtils.toString(stream, "UTF-8"));
                Script script = (Script) scriptClass.newInstance();
                script.run();
                Binding binding = script.getBinding();
                if (binding.getVariables().containsKey(ConstraintsEvaluator.PROPERTY_NAME)) {
                    return (Closure<?>) binding.getVariable(ConstraintsEvaluator.PROPERTY_NAME);
                }
                LOG.warn("Unable to evaluate constraints from [" + constraintsScript + "], constraints closure not found!");
                return null;
            } catch (CompilationFailedException e) {
                LOG.error("Compilation error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            } catch (InstantiationException e) {
                LOG.error("Instantiation error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            } catch (IllegalAccessException e) {
                LOG.error("Illegal access error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void applyDefaultConstraints(GrailsDomainClassProperty domainClassProperty, Constrained constrained, Map<String, Object> defaultConstraints) {
        if (defaultConstraints.containsKey("*")) {
            final Object o = defaultConstraints.get("*");
            if (o instanceof Map) {
                Map<String, Object> globalConstraints = (Map<String, Object>) o;
                applyMapOfConstraints(globalConstraints, domainClassProperty.getName(), domainClassProperty, constrained);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void applyDefaultConstraints(String propertyName, Constrained constrained) {
        if (defaultConstraints.containsKey("*")) {
            final Object o = defaultConstraints.get("*");
            if (o instanceof Map) {
                Map<String, Object> globalConstraints = (Map<String, Object>) o;
                applyMapOfConstraints(globalConstraints, propertyName, null, constrained);
            }
        }
    }

    protected void applyDefaultNullableConstraint(GrailsDomainClassProperty domainClassProperty, Constrained constrained) {
        if (canApplyNullableConstraint(domainClassProperty.getName(), domainClassProperty, constrained)) {
            applyDefaultNullableConstraint(constrained, false);
        }
    }

    protected void applyDefaultNullableConstraint(Constrained constrained, boolean defaultNullable) {
        if (!constrained.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT)) {
            boolean isCollection = Collection.class.isAssignableFrom(constrained.getPropertyType()) || Map.class.isAssignableFrom(constrained.getPropertyType());
            constrained.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, isCollection || defaultNullable);
        }
    }

    protected Map<String, Method> getConstrainablePropertyMap(Class theClass, boolean isDomainClass) {
        Set<String> ignoredProperties = new HashSet<>();
        ignoredProperties.add(GrailsDomainClassProperty.CLASS);
        ignoredProperties.add(GrailsDomainClassProperty.META_CLASS);
        ignoredProperties.add(GrailsDomainClassProperty.ERRORS);

        if (isDomainClass) {
            ignoredProperties.add(GrailsDomainConfigurationUtil.PROPERTIES_PROPERTY);
            ignoredProperties.add(GrailsDomainClassProperty.IDENTITY);
            ignoredProperties.add(GrailsDomainClassProperty.VERSION);
            ignoredProperties.add(GrailsDomainClassProperty.DIRTY_PROPERTY_NAMES);
            ignoredProperties.add(GrailsDomainClassProperty.DIRTY);
            ignoredProperties.add(GrailsDomainClassProperty.ATTACHED);
            final Object transients = GrailsClassUtils.getStaticPropertyValue(theClass,
                    GrailsDomainClassProperty.TRANSIENT);
            if(transients instanceof List) {
                ignoredProperties.addAll((List) transients);
            }
            final PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(theClass);
            for(PropertyDescriptor descriptor : propertyDescriptors) {
                final Method readMethod = descriptor.getReadMethod();

                final Method writeMethod = descriptor.getWriteMethod();
                if( readMethod == null ) {
                    ignoredProperties.add(descriptor.getName());
                }
                else if(writeMethod == null || (Modifier.isTransient(readMethod.getModifiers()))) {
                    PersistenceMethod annotation = readMethod.getAnnotation(PersistenceMethod.class);
                    if(annotation == null) {
                        ignoredProperties.add(descriptor.getName());
                    }
                }
            }
        }


        Field[] declaredFields = theClass.getDeclaredFields();
        for(Field field : declaredFields) {
            if(Modifier.isTransient(field.getModifiers())) {
                ignoredProperties.add(field.getName());
            }
        }

        Map<String, Method> propertyMap = new HashMap<>();
        for (Object aClassChain : GrailsDomainConfigurationUtil.getSuperClassChain(theClass)) {
            Class<?> clazz = (Class<?>) aClassChain;
            for (Method method : clazz.getDeclaredMethods()) {
                if (GrailsClassUtils.isPropertyGetter(method)) {
                    String propertyName = GrailsClassUtils.getPropertyForGetter(method.getName(), method.getReturnType());
                    if (!ignoredProperties.contains(propertyName)) {
                        System.out.println(method.getName());
                        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(theClass, propertyName);
                        if (descriptor != null && descriptor.getWriteMethod() != null) {
                            propertyMap.put(propertyName, method);
                        }
                    }
                }
            }
        }
        return propertyMap;
    }

    protected boolean canApplyNullableConstraint(String propertyName, GrailsDomainClassProperty property, Constrained constrained) {
        if (property == null || property.getType() == null) return false;

        final GrailsDomainClass domainClass = property.getDomainClass();
        // only apply default nullable to Groovy entities not legacy Java ones
        if (!GroovyObject.class.isAssignableFrom(domainClass.getClazz())) return false;

        final GrailsDomainClassProperty versionProperty = domainClass.getVersion();
        final boolean isVersion = versionProperty != null && versionProperty.equals(property);
        return !constrained.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT) &&
            isConstrainableProperty(property, propertyName) && !property.isIdentity() && !isVersion && !property.isDerived();
    }

    protected void applyMapOfConstraints(Map<String, Object> constraints, String propertyName, GrailsDomainClassProperty domainClassProperty, Constrained constrained) {
        for (Map.Entry<String, Object> entry : constraints.entrySet()) {
            String constraintName = entry.getKey();
            Object constrainingValue = entry.getValue();
            if (!constrained.hasAppliedConstraint(constraintName) && constrained.supportsContraint(constraintName)) {
                if (ConstrainedProperty.NULLABLE_CONSTRAINT.equals(constraintName)) {
                    if (isConstrainableProperty(domainClassProperty, propertyName)) {
                        constrained.applyConstraint(constraintName, constrainingValue);
                    }
                } else {
                    constrained.applyConstraint(constraintName, constrainingValue);
                }
            }
        }
    }

    protected boolean isConstrainableProperty(GrailsDomainClassProperty domainClassProperty, String propertyName) {
        if (domainClassProperty == null) return true;

        // there is some properties to have to ignore "nullable" if it's domain class.
        return !propertyName.equals(GrailsDomainClassProperty.DATE_CREATED) &&
            !propertyName.equals(GrailsDomainClassProperty.LAST_UPDATED) &&
            !((domainClassProperty.isOneToOne() || domainClassProperty.isManyToOne()) && domainClassProperty.isCircular());
    }
}
