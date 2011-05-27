/* Copyright (C) 2011 SpringSource
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
package org.codehaus.groovy.grails.validation;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.Entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Default implementation of the {@link ConstraintsEvaluator} interface.
 *
 * TODO: Subclass this to add hibernate-specific exceptions!
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class DefaultConstraintEvaluator implements ConstraintsEvaluator {

    private static final Log LOG = LogFactory.getLog(DefaultConstraintEvaluator.class);
    private Map<String, Object> defaultConstraints;

    public DefaultConstraintEvaluator(Map<String, Object> defaultConstraints) {
        this.defaultConstraints = defaultConstraints;
    }

    public DefaultConstraintEvaluator() {
        // default
    }

    public Map<String, Object> getDefaultConstraints() {
        return defaultConstraints;
    }

    public Map<String, ConstrainedProperty> evaluate(@SuppressWarnings("rawtypes") Class cls) {
        return evaluateConstraints(cls, null);
    }

    public Map<String, ConstrainedProperty> evaluate(GrailsDomainClass cls) {
        return evaluate(cls.getClazz(), cls.getPersistentProperties());
    }

      /**
     * Evaluates the constraints closure to build the list of constraints
     *
     * @param theClass  The domain class to evaluate constraints for
     * @param properties The properties of the instance
     *
     * @return A Map of constraints
     */
    protected Map<String, ConstrainedProperty> evaluateConstraints(
          final Class<?> theClass,
          GrailsDomainClassProperty[] properties) {

        boolean javaEntity = theClass.isAnnotationPresent(Entity.class);
        LinkedList<?> classChain = getSuperClassChain(theClass);
        Class<?> clazz;

        ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(theClass);

        // Evaluate all the constraints closures in the inheritance chain
        for (Object aClassChain : classChain) {
            clazz = (Class<?>) aClassChain;
            Closure<?> c = (Closure<?>) GrailsClassUtils.getStaticPropertyValue(clazz, PROPERTY_NAME);
            if (c == null) {
                c = getConstraintsFromScript(theClass);
            }

            if (c != null) {
                c.setDelegate(delegate);
                c.call();
            }
            else {
                LOG.debug("User-defined constraints not found on class [" + clazz + "], applying default constraints");
            }
        }

        Map<String, ConstrainedProperty> constrainedProperties = delegate.getConstrainedProperties();
        if (properties != null && !(constrainedProperties.isEmpty() && javaEntity)) {

            for (GrailsDomainClassProperty p : properties) {
                // assume no formula issues if Hibernate isn't available to avoid CNFE
                if (canPropertyBeConstrained(p)) {
                    if (p.isDerived()) {
                        if (constrainedProperties.remove(p.getName()) != null) {
                            LOG.warn("Derived properties may not be constrained. Property [" + p.getName() + "] of domain class " + theClass.getName() + " will not be checked during validation.");
                        }
                    } else {
                        final String propertyName = p.getName();
                        ConstrainedProperty cp = constrainedProperties.get(propertyName);
                        if (cp == null) {
                            cp = new ConstrainedProperty(p.getDomainClass().getClazz(), propertyName, p.getType());
                            cp.setOrder(constrainedProperties.size() + 1);
                            constrainedProperties.put(propertyName, cp);
                        }
                        // Make sure all fields are required by default, unless
                        // specified otherwise by the constraints
                        // If the field is a Java entity annotated with @Entity skip this
                        applyDefaultConstraints(propertyName, p, cp,
                                defaultConstraints);
                        }
                }
            }
        }

        applySharedConstraints(delegate, constrainedProperties);

        return constrainedProperties;
    }

    protected void applySharedConstraints(
            ConstrainedPropertyBuilder constrainedPropertyBuilder,
            Map<String, ConstrainedProperty> constrainedProperties) {
        for (Map.Entry<String, ConstrainedProperty> entry : constrainedProperties.entrySet()) {
            String propertyName = entry.getKey();
            ConstrainedProperty constrainedProperty = entry.getValue();
            String sharedConstraintReference = constrainedPropertyBuilder.getSharedConstraint(propertyName);
            if (sharedConstraintReference != null) {
                Object o = defaultConstraints.get(sharedConstraintReference);
                if (o instanceof Map) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> constraintsWithinSharedConstraint = (Map) o;
                    for (Map.Entry<String, Object> e : constraintsWithinSharedConstraint.entrySet()) {
                        constrainedProperty.applyConstraint(e.getKey(), e.getValue());
                    }
                } else {
                    throw new GrailsConfigurationException("Property [" + constrainedProperty.owningClass.getName()+'.'+propertyName+ "] references shared constraint [" +sharedConstraintReference+ ":" +o+ "], which doesn't exist!");
                }
            }
        }
    }

    protected boolean canPropertyBeConstrained(@SuppressWarnings("unused") GrailsDomainClassProperty property) {
        return true;
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

    protected Closure<?> getConstraintsFromScript(Class<?> theClass) {
        // Fallback to xxxxConstraints.groovy script for Java domain classes
        String className = theClass.getName();
        String constraintsScript = className.replaceAll("\\.","/") + CONSTRAINTS_GROOVY_SCRIPT;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(constraintsScript);

        if (stream!=null) {
            GroovyClassLoader gcl = new GroovyClassLoader();
            try {
                Class<?> scriptClass = gcl.parseClass(DefaultGroovyMethods.getText(stream));
                Script script = (Script)scriptClass.newInstance();
                script.run();
                Binding binding = script.getBinding();
                if (binding.getVariables().containsKey(PROPERTY_NAME)) {
                    return (Closure<?>)binding.getVariable(PROPERTY_NAME);
                }
                LOG.warn("Unable to evaluate constraints from [" + constraintsScript + "], constraints closure not found!");
                return null;
            }
            catch (CompilationFailedException e) {
                LOG.error("Compilation error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            }
            catch (InstantiationException e) {
                LOG.error("Instantiation error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            }
            catch (IllegalAccessException e) {
                LOG.error("Illegal access error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
                return null;
            }
            catch (IOException e) {
                LOG.error("IO error evaluating constraints for class [" + className + "]: " + e.getMessage(), e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void applyDefaultConstraints(String propertyName, GrailsDomainClassProperty p,
            ConstrainedProperty cp, @SuppressWarnings("hiding") Map<String, Object> defaultConstraints) {

        if (defaultConstraints != null && !defaultConstraints.isEmpty()) {
            if (defaultConstraints.containsKey("*")) {
                final Object o = defaultConstraints.get("*");
                if (o instanceof Map) {
                    Map<String, Object> globalConstraints = (Map<String, Object>)o;
                    applyMapOfConstraints(globalConstraints, propertyName, p, cp);
                }
            }
        }

        if (canApplyNullableConstraint(propertyName, p, cp)) {
            applyDefaultNullableConstraint(p, cp);
        }
    }

    protected void applyDefaultNullableConstraint(GrailsDomainClassProperty p, ConstrainedProperty cp) {
        cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT,
                Collection.class.isAssignableFrom(p.getType()) ||
                Map.class.isAssignableFrom(p.getType()));
    }

    protected boolean canApplyNullableConstraint(String propertyName, GrailsDomainClassProperty property, ConstrainedProperty constrainedProperty) {
        if (property == null || property.getType() == null) return false;

        final GrailsDomainClass domainClass = property.getDomainClass();
        // only apply default nullable to Groovy entities not legacy Java ones
        if (!GroovyObject.class.isAssignableFrom(domainClass.getClazz())) return false;

        final GrailsDomainClassProperty versionProperty = domainClass.getVersion();
        final boolean isVersion = versionProperty != null && versionProperty.equals(property);
        return !constrainedProperty.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT) &&
            isConstrainableProperty(property, propertyName) && !property.isIdentity() && !isVersion && !property.isDerived();
    }

    protected void applyMapOfConstraints(Map<String, Object> constraints, String propertyName, GrailsDomainClassProperty p, ConstrainedProperty cp) {
        for (Map.Entry<String, Object> entry : constraints.entrySet()) {
            String constraintName = entry.getKey();
            Object constrainingValue = entry.getValue();
            if (!cp.hasAppliedConstraint(constraintName) && cp.supportsContraint(constraintName)) {
                if (ConstrainedProperty.NULLABLE_CONSTRAINT.equals(constraintName)) {
                    if (isConstrainableProperty(p,propertyName)) {
                        cp.applyConstraint(constraintName, constrainingValue);
                    }
                }
                else {
                    cp.applyConstraint(constraintName,constrainingValue);
                }
            }
        }
    }

    protected boolean isConstrainableProperty(GrailsDomainClassProperty p, String propertyName) {
        return !propertyName.equals(GrailsDomainClassProperty.DATE_CREATED) &&
               !propertyName.equals(GrailsDomainClassProperty.LAST_UPDATED) &&
               !((p.isOneToOne() || p.isManyToOne()) && p.isCircular());
    }

    public Map<String, ConstrainedProperty> evaluate(Object object, GrailsDomainClassProperty[] properties) {
        return evaluateConstraints(object.getClass(), properties);
    }

    public Map<String, ConstrainedProperty> evaluate(Class<?> cls, GrailsDomainClassProperty[] properties) {
        return evaluateConstraints(cls, properties);
    }
}
