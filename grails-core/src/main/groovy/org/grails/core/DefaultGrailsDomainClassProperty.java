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
package org.grails.core;

import grails.core.ComponentCapableDomainClass;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.util.GrailsClassUtils;
import grails.util.GrailsNameUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import grails.validation.Constrained;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.io.support.GrailsFactoriesLoader;
import grails.validation.ConstraintsEvaluator;
import org.grails.core.support.GrailsDomainConfigurationUtil;
import org.grails.core.util.ClassPropertyFetcher;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Represents a property of a domain class and contains meta information about the
 * properties relationships, naming conventions and type.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class DefaultGrailsDomainClassProperty implements GrailsDomainClassProperty, org.codehaus.groovy.grails.commons.GrailsDomainClassProperty {

    private GrailsDomainClass domainClass;
    private boolean persistent = true; // persistant by default
    private boolean identity;
    private boolean oneToMany;
    private String name;
    private Class<?> type;
    private boolean manyToMany;
    private boolean manyToOne;
    private boolean oneToOne;
    private boolean hasOne = false;
    private boolean bidirectional;
    private boolean derived = false;

    private Class<?> referencedPropertyType;
    private GrailsDomainClass referencedDomainClass;
    private GrailsDomainClassProperty otherSide;
    private String naturalName;
    private boolean inherited;
    private int fetchMode = FETCH_LAZY;
    private boolean owningSide;
    private String referencePropertyName;
    private boolean embedded;
    private GrailsDomainClass component;
    private boolean basicCollectionType;
    private Map<String, Object> defaultConstraints;
    private boolean explicitSaveUpdateCascade = false;

    /**
     * Constructor.
     * @param domainClass
     * @param descriptor
     */
    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PropertyDescriptor descriptor) {
        this(domainClass, descriptor, null);
    }

    /**
     * Constructor.
     * @param domainClass
     * @param descriptor
     */
    @SuppressWarnings("rawtypes")
    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PropertyDescriptor descriptor, Map<String, Object> defaultConstraints) {
        this.domainClass = domainClass;
        name = descriptor.getName();
        naturalName = GrailsNameUtils.getNaturalName(descriptor.getName());
        type = descriptor.getPropertyType();
        identity = descriptor.getName().equals(IDENTITY);

        // establish if property is persistant
        if (domainClass != null) {
            // figure out if this property is inherited
            if (!domainClass.isRoot()) {
                inherited = GrailsClassUtils.isPropertyInherited(domainClass.getClazz(), name);
            }
            List transientProps = getTransients();
            checkIfTransient(transientProps);

            establishFetchMode();
        }

        if (descriptor.getReadMethod() == null || descriptor.getWriteMethod() == null) {
            persistent = false;
        }

        if (Errors.class.isAssignableFrom(type)) {
            persistent = false;
        }
        this.defaultConstraints = defaultConstraints;
    }

    /**
     * Evaluates the fetchmode.
     */
    @SuppressWarnings("rawtypes")
    private void establishFetchMode() {
        Map fetchMap = domainClass.getPropertyValue(GrailsDomainClassProperty.FETCH_MODE, Map.class);
        if (fetchMap != null && fetchMap.containsKey(name)) {
            if ("eager".equals(fetchMap.get(name))) {
                fetchMode = FETCH_EAGER;
            }
        }
    }

    /**
     * Checks whether this property is transient
     *
     * @param transientProps The transient properties
     */
    @SuppressWarnings("rawtypes")
    private void checkIfTransient(List transientProps) {
        if (transientProps == null) {
            return;
        }

        for (Object currentObj : transientProps) {
            // make sure its a string otherwise ignore. Note: Again maybe a warning?
            if (currentObj instanceof String) {
                String propertyName = (String)currentObj;
                // if the property name is on the not persistant list
                // then set persistant to false
                if (propertyName.equals(name)) {
                    persistent = false;
                    break;
                }
            }
        }
    }

    /**
     * Retrieves the transient properties
     *
     * @return A list of transient properties
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List getTransients() {
        List allTransientProps = new ArrayList();

        List<Class<?>> allClasses = getAllDomainClassesInHierarchy();
        for (Class currentClass : allClasses) {
            ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(currentClass);
            Object transientProperty = propertyFetcher.getPropertyValue(TRANSIENT, false);
            if (transientProperty instanceof List) {
                List transientList = (List) transientProperty;
                allTransientProps.addAll(transientList);
            }
        }

        return allTransientProps;
    }

    private List<Class<?>> getAllDomainClassesInHierarchy() {
        List<Class<?>> classesInHierarchy = new ArrayList<Class<?>>();

        Class<?> currentClass = domainClass.getClazz();
        while(currentClass != null) {
            classesInHierarchy.add(currentClass);
            Class<?> superClass = currentClass.getSuperclass();
            currentClass = DomainClassArtefactHandler.isDomainClass(superClass) ? superClass : null;
        }
        return classesInHierarchy;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getType()
     */
    @SuppressWarnings("rawtypes")
    public Class getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isPersistant()
     */
    public boolean isPersistent() {
        return persistent;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isRequired()
     */
    public boolean isOptional() {
        Constrained constrained = (Constrained) domainClass.getConstrainedProperties().get(name);
        return (constrained != null) && constrained.isNullable();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isIdentity()
     */
    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean b) {
        identity = b;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToMany()
     */
    public boolean isOneToMany() {
        return oneToMany;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isManyToOne()
     */
    public boolean isManyToOne() {
        return manyToOne;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getFieldName()
     */
    public String getFieldName() {
        return getName().toUpperCase();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToOne()
     */
    public boolean isOneToOne() {
        return oneToOne;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getDomainClass()
     */
    public GrailsDomainClass getDomainClass() {
        return domainClass;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isManyToMany()
     */
    public boolean isManyToMany() {
        return manyToMany;
    }

    /**
     * @param manyToMany The manyToMany to set.
     */
    protected void setManyToMany(boolean manyToMany) {
        this.manyToMany = manyToMany;
    }

    /**
     * @param oneToMany The oneToMany to set.
     */
    protected void setOneToMany(boolean oneToMany) {
        this.oneToMany = oneToMany;
    }

    /**
     * @param manyToOne The manyToOne to set.
     */
    protected void setManyToOne(boolean manyToOne) {
        this.manyToOne = manyToOne;
    }

    /**
     * @param oneToOne The oneToOne to set.
     */
    protected void setOneToOne(boolean oneToOne) {
        this.oneToOne = oneToOne;
    }

    /**
     * Set whether the foreign key is stored in the parent or child in a one-to-one
     * @param isHasOne True if its stored in the parent
     */
    protected void setHasOne(boolean isHasOne) {
        this.hasOne = isHasOne;
    }

    /**
     * @return true if the foreign key in a one-to-one is stored in the parent
     */
    public boolean isHasOne() {
        return hasOne;
    }

    /**
     * @param persistent The persistant to set.
     */
    protected void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Sets whether the relationship is bidirectional or not
     */
    protected void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getTypePropertyName()
     */
    public String getTypePropertyName() {
        String shortTypeName = ClassUtils.getShortName(type);
        return shortTypeName.substring(0,1).toLowerCase(Locale.ENGLISH) + shortTypeName.substring(1);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getReferencedPropertyType()
     */
    @SuppressWarnings("rawtypes")
    public Class getReferencedPropertyType() {
        if (isDomainAssociation()) {
            return referencedPropertyType;
        }

        return getType();
    }

    private boolean isDomainAssociation() {
        return (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) &&
                referencedPropertyType != null;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isBidirectional()
     */
    public boolean isBidirectional() {
        return bidirectional;
    }

    /**
     * Sets the referenced property type of this property
     */
    protected void setReferencedPropertyType(Class<?> referencedPropertyType) {
        this.referencedPropertyType = referencedPropertyType;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public GrailsDomainClass getReferencedDomainClass() {
        return referencedDomainClass;
    }

    public void setReferencedDomainClass(GrailsDomainClass referencedDomainClass) {
        if (referencedDomainClass != null) {
            this.referencedDomainClass = referencedDomainClass;
            this.referencedPropertyType = referencedDomainClass.getClazz();
        }
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public boolean isAssociation() {
        return isOneToMany() ||
                isOneToOne() ||
                isManyToOne() ||
                isManyToMany() ||
                isEmbedded();
    }

    public boolean isEnum() {
        return getType().isEnum();
    }

    public String getNaturalName() {
        return naturalName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String assType = null;
        if (isManyToMany()) {
            assType = "many-to-many";
        }
        else if (isOneToMany()) {
            assType = "one-to-many";
        }
        else if (isOneToOne()) {
            assType = "one-to-one";
        }
        else if (isManyToOne()) {
            assType = "many-to-one";
        }
        else if (isEmbedded()) {
            assType = "embedded";
        }
        return new ToStringCreator(this)
                .append("name", name)
                .append("type", type)
                .append("persistent", isPersistent())
                .append("optional", isOptional())
                .append("association", isAssociation())
                .append("bidirectional", isBidirectional())
                .append("association-type", assType)
                .toString();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getOtherSide()
     */
    public GrailsDomainClassProperty getOtherSide() {
        return otherSide;
    }

    public void setOtherSide(GrailsDomainClassProperty property) {
        if (!equals(property)) {
            setBidirectional(true);
            if (isOneToOne() && property.isOneToMany()) {
                setOneToOne(false);
                setManyToOne(true);
            }
        }
        otherSide = property;
    }

    public boolean isExplicitSaveUpdateCascade() {
        return explicitSaveUpdateCascade;
    }

    public void setExplicitSaveUpdateCascade(boolean explicitSaveUpdateCascade) {
        this.explicitSaveUpdateCascade = explicitSaveUpdateCascade;
    }

    public boolean isInherited() {
        return inherited;
    }

    public int getFetchMode() {
        return fetchMode;
    }

    public boolean isOwningSide() {
        return isHasOne() || owningSide;
    }

    public void setOwningSide(boolean b) {
        owningSide = b;
    }

    @SuppressWarnings("unchecked")
    public boolean isCircular() {
        if (otherSide != null) {
            if (otherSide.getDomainClass().getClazz().isAssignableFrom(domainClass.getClazz())) {
                return true;
            }
        }
        else if (getReferencedPropertyType().isAssignableFrom(domainClass.getClazz())) {
            return true;
        }
        return false;
    }

    public void setReferencePropertyName(String name) {
        referencePropertyName = name;
    }

    public String getReferencedPropertyName() {
        return referencePropertyName;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public GrailsDomainClass getComponent() {
        return component;
    }

    public void setEmbedded(boolean isEmbedded) {
        embedded = isEmbedded;
        if (isEmbedded) {
            component = new ComponentDomainClass(getType());

            final GrailsDomainClass dc = getDomainClass();
            if (dc instanceof ComponentCapableDomainClass) {
                ((ComponentCapableDomainClass) dc).addComponent(component);
            }
        }
    }

    public boolean isDerived() {
        return derived;
    }

    public void setDerived(boolean derived) {
        this.derived = derived;
    }

    /**
     * Overridden equals to take into account inherited properties
     * e.g. childClass.propertyName is equal to parentClass.propertyName if the types match and
     * childClass.property.isInherited
     *
     * @param o the Object to compare this property to
     * @return boolean indicating equality of the two objects
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof GrailsDomainClassProperty) {
            if (!super.equals(o)) {
                GrailsDomainClassProperty otherProp = (GrailsDomainClassProperty) o;
                Class<?> myActualClass = getDomainClass().getClazz();
                Class<?> otherActualClass = otherProp.getDomainClass().getClazz();
                return otherProp.getName().equals(getName()) &&
                        otherProp.getReferencedPropertyType().equals(getReferencedPropertyType()) &&
                        (otherActualClass.isAssignableFrom(myActualClass) ||
                        myActualClass.isAssignableFrom(otherActualClass));
            }

            return true;
        }

        return false;
    }

    public void setBasicCollectionType(boolean b) {
        basicCollectionType = b;
    }

    public boolean isBasicCollectionType() {
        return basicCollectionType;
    }

    @SuppressWarnings("rawtypes")
    private class ComponentDomainClass extends AbstractGrailsClass implements GrailsDomainClass {
        private GrailsDomainClassProperty[] properties;
        private Map constraints = Collections.emptyMap();
        private List transients = Collections.emptyList();

        public ComponentDomainClass(Class<?> type) {
            super(type, "");

            PropertyDescriptor[] descriptors = getPropertyDescriptors();

            List tmp = getPropertyValue(GrailsDomainClassProperty.TRANSIENT, List.class);
            if (tmp != null) transients = tmp;
            properties = createDomainClassProperties(descriptors);

            ConstraintsEvaluator constraintsEvaluator = getConstraintsEvaluator();
            if(constraintsEvaluator != null) {
                constraints = constraintsEvaluator.evaluate(type, properties);
            }
            else {
                constraints = Collections.emptyMap();
            }
        }

        private ConstraintsEvaluator getConstraintsEvaluator() {
            if (domainClass instanceof DefaultGrailsDomainClass) {
                return ((DefaultGrailsDomainClass) domainClass).getConstraintsEvaluator();
            }
            return GrailsFactoriesLoader.loadFactory(ConstraintsEvaluator.class);
        }

        private GrailsDomainClassProperty[] createDomainClassProperties(PropertyDescriptor[] descriptors) {

            List<DefaultGrailsDomainClassProperty> props = new ArrayList<DefaultGrailsDomainClassProperty>();
            Collection<String> embeddedNames = getEmbeddedList();
            for (PropertyDescriptor descriptor : descriptors) {
                if (isPersistentProperty(descriptor)) {
                    DefaultGrailsDomainClassProperty property = new DefaultGrailsDomainClassProperty(
                            this, descriptor, defaultConstraints);
                    props.add(property);
                    if (embeddedNames.contains(property.getName())) {
                        property.setEmbedded(true);
                    }
                }
            }
            return props.toArray(new GrailsDomainClassProperty[props.size()]);
        }

        @SuppressWarnings("unchecked")
        private Collection<String> getEmbeddedList() {
            Object potentialList = GrailsClassUtils.getStaticPropertyValue(getClazz(), "embedded");
            if (potentialList instanceof Collection) {
                return (Collection<String>)potentialList;
            }
            return Collections.emptyList();
        }

        private boolean isPersistentProperty(PropertyDescriptor descriptor) {
            String propertyName = descriptor.getName();

            return GrailsDomainConfigurationUtil.isNotConfigurational(descriptor) && !transients.contains(propertyName);
        }

        public boolean isOwningClass(Class dc) {
            return dc != null && dc.equals(getDomainClass().getClazz());
        }

        public GrailsDomainClassProperty[] getProperties() {
            return properties;
        }

        /**
         * @deprecated Use #getPersistentProperties instead
         */
        @SuppressWarnings("dep-ann")
        public GrailsDomainClassProperty[] getPersistantProperties() {
            return properties;
        }

        public GrailsDomainClassProperty[] getPersistentProperties() {
            return properties;
        }

        public GrailsDomainClassProperty getIdentifier() {
            return null;  // no identifier for embedded component
        }

        public GrailsDomainClassProperty getVersion() {
            return null;  // no version for embedded component
        }

        public Map getAssociationMap() {
            return Collections.emptyMap();
        }

        public GrailsDomainClassProperty getPropertyByName(String name) {
            for (GrailsDomainClassProperty property : properties) {
                if (property.getName().equals(name)) return property;
            }
            return null;
        }

        public GrailsDomainClassProperty getPersistentProperty(String name) {
            return getPropertyByName(name);
        }

        public String getFieldName(String propertyName) {
            return null;
        }

        public boolean isOneToMany(String propertyName) {
            return false;
        }

        public boolean isManyToOne(String propertyName) {
            return false;
        }

        public boolean isBidirectional(String propertyName) {
            return false;
        }

        public Class<?> getRelatedClassType(String propertyName) {
            return getPropertyByName(propertyName).getReferencedPropertyType();
        }

        public Map getConstrainedProperties() {
            return constraints;
        }

        public Validator getValidator() {
            return null;
        }

        public void setValidator(Validator validator) {
            // ignored
        }

        public String getMappingStrategy() {
            return GrailsDomainClass.GORM;
        }

        public boolean isRoot() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public Set getSubClasses() {
            return Collections.emptySet();
        }

        public void refreshConstraints() {
            constraints =getConstraintsEvaluator().evaluate(getClazz(), getPersistentProperties());
        }

        public boolean hasSubClasses() {
            return false;
        }

        public Map getMappedBy() {
            return Collections.emptyMap();
        }

        public boolean hasPersistentProperty(String propertyName) {
            for (int i = 0; i < properties.length; i++) {
                GrailsDomainClassProperty persistantProperty = properties[i];
                if (persistantProperty.getName().equals(propertyName)) return true;
            }
            return false;
        }

        public void setMappingStrategy(String strategy) {
            // do nothing
        }
    }
}
