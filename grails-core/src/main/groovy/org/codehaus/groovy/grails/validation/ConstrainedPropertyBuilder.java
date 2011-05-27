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
package org.codehaus.groovy.grails.validation;

import grails.util.GrailsUtil;
import groovy.lang.MissingMethodException;
import groovy.util.BuilderSupport;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.springframework.beans.InvalidPropertyException;

/**
 * Builder used as a delegate within the "constraints" closure of GrailsDomainClass instances .
 *
 * @author Graeme Rocher
 */
public class ConstrainedPropertyBuilder extends BuilderSupport {

    private Map<String, ConstrainedProperty> constrainedProperties = new HashMap<String, ConstrainedProperty>();
    private Map<String, String> sharedConstraints = new HashMap<String, String>();
    private int order = 1;
    private Class<?> targetClass;
    private ClassPropertyFetcher classPropertyFetcher;
    private static final String SHARED_CONSTRAINT = "shared";
    private static final String IMPORT_FROM_CONSTRAINT = "importFrom";

    public ConstrainedPropertyBuilder(Object target) {
        this(target.getClass());
    }

    public ConstrainedPropertyBuilder(Class<?> targetClass) {
        this.targetClass = targetClass;
        classPropertyFetcher = ClassPropertyFetcher.forClass(targetClass);
    }

    public String getSharedConstraint(String propertyName) {
        return sharedConstraints.get(propertyName);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object createNode(Object name, Map attributes) {
        // we do this so that missing property exception is throw if it doesn't exist

        try {
            String property = (String)name;
            ConstrainedProperty cp;
            if (constrainedProperties.containsKey(property)) {
                cp = constrainedProperties.get(property);
            }
            else {
                cp = new ConstrainedProperty(targetClass, property, classPropertyFetcher.getPropertyType(property));
                cp.setOrder(order++);
                constrainedProperties.put(property, cp);
            }

            if (cp.getPropertyType() == null) {
                if (!IMPORT_FROM_CONSTRAINT.equals(name)) {
                    GrailsUtil.warn("Property [" + cp.getPropertyName() + "] not found in domain class " +
                        targetClass.getName() + "; cannot apply constraints: " + attributes);
                }
                return cp;
            }

            for (Object o : attributes.keySet()) {
                String constraintName = (String) o;
                final Object value = attributes.get(constraintName);
                if (SHARED_CONSTRAINT.equals(constraintName)) {
                    if (value != null) {
                        sharedConstraints.put(property, value.toString());
                    }
                    continue;
                }
                if (cp.supportsContraint(constraintName)) {
                    cp.applyConstraint(constraintName, value);
                }
                else {
                    if (ConstrainedProperty.hasRegisteredConstraint(constraintName)) {
                        // constraint is registered but doesn't support this property's type
                        GrailsUtil.warn("Property [" + cp.getPropertyName() + "] of domain class " +
                                targetClass.getName() + " has type [" + cp.getPropertyType().getName() +
                                "] and doesn't support constraint [" + constraintName +
                                "]. This constraint will not be checked during validation.");
                    }
                    else {
                        // in the case where the constraint is not supported we still retain meta data
                        // about the constraint in case its needed for other things
                        cp.addMetaConstraint(constraintName, value);
                    }
                }
            }

            return cp;
        }
        catch(InvalidPropertyException ipe) {
            throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes});
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        if (IMPORT_FROM_CONSTRAINT.equals(name) && (value instanceof Class)) {
            return handleImportFrom(attributes, (Class) value);
        }
        throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes,value});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object handleImportFrom(Map attributes, Class importFromClazz) {

        Map importFromConstrainedProperties = new DefaultConstraintEvaluator().evaluate(importFromClazz);

        PropertyDescriptor[] targetPropertyDescriptorArray = classPropertyFetcher.getPropertyDescriptors();

        List toBeIncludedPropertyNamesParam = (List) attributes.get("include");
        List toBeExcludedPropertyNamesParam = (List) attributes.get("exclude");

        List<String> resultingPropertyNames = new ArrayList<String>();
        for (PropertyDescriptor targetPropertyDescriptor : targetPropertyDescriptorArray) {
            String targetPropertyName = targetPropertyDescriptor.getName();

            if (toBeIncludedPropertyNamesParam == null) {
                resultingPropertyNames.add(targetPropertyName);
            }
            else if (isListOfRegexpsContainsString(toBeIncludedPropertyNamesParam, targetPropertyName)) {
                resultingPropertyNames.add(targetPropertyName);
            }

            if (toBeExcludedPropertyNamesParam != null
                    && isListOfRegexpsContainsString(toBeExcludedPropertyNamesParam, targetPropertyName)) {
                resultingPropertyNames.remove(targetPropertyName);
            }
        }

        resultingPropertyNames.remove("class");
        resultingPropertyNames.remove("metaClass");

        for (String targetPropertyName : resultingPropertyNames) {
            ConstrainedProperty importFromConstrainedProperty =
                    (ConstrainedProperty) importFromConstrainedProperties.get(targetPropertyName);

            if (importFromConstrainedProperty != null) {
                // Map importFromConstrainedPropertyAttributes = importFromConstrainedProperty.getAttributes();
                // createNode(targetPropertyName, importFromConstrainedPropertyAttributes);
                Map importFromConstrainedPropertyAttributes = new HashMap();
                for (Constraint importFromAppliedConstraint : importFromConstrainedProperty.getAppliedConstraints()) {
                    String importFromAppliedConstraintName = importFromAppliedConstraint.getName();
                    Object importFromAppliedConstraintParameter = importFromAppliedConstraint.getParameter();
                    importFromConstrainedPropertyAttributes.put(
                            importFromAppliedConstraintName, importFromAppliedConstraintParameter);
                }

                createNode(targetPropertyName, importFromConstrainedPropertyAttributes);
            }
        }

        return null;
    }

    private boolean isListOfRegexpsContainsString(List<String> listOfStrings, String stringToMatch) {
        boolean result = false;

        for (String listElement:listOfStrings) {
            if (stringToMatch.matches(listElement)) {
                result = true;
                break;
            }
        }

        return result;
    }

    @Override
    protected void setParent(Object parent, Object child) {
        // do nothing
    }

    @Override
    protected Object createNode(Object name) {
        return createNode(name, Collections.EMPTY_MAP);
    }

    @Override
    protected Object createNode(Object name, Object value) {
        return createNode(name,Collections.EMPTY_MAP,value);
    }

    public Map<String, ConstrainedProperty> getConstrainedProperties() {
        return constrainedProperties;
    }
}
