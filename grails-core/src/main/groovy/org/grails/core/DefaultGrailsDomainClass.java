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
import grails.core.LazyMappingContext;
import grails.gorm.validation.PersistentEntityValidator;
import grails.util.GrailsClassUtils;
import grails.util.GrailsNameUtils;

import java.beans.PropertyDescriptor;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.core.exceptions.GrailsDomainException;
import org.grails.core.exceptions.InvalidPropertyException;
import grails.validation.ConstraintsEvaluator;
import org.grails.core.support.GrailsDomainConfigurationUtil;
import org.grails.core.util.ClassPropertyFetcher;
import org.grails.datastore.gorm.config.GrailsDomainClassPersistentProperty;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;

/**
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsDomainClass extends AbstractGrailsClass implements GrailsDomainClass, ComponentCapableDomainClass{

    private static final Log log = LogFactory.getLog(DefaultGrailsDomainClass.class);

    private GrailsDomainClassProperty identifier;
    private GrailsDomainClassProperty version;
    private GrailsDomainClassProperty[] properties;
    private GrailsDomainClassProperty[] persistentProperties;
    private Map<String, GrailsDomainClassProperty> propertyMap;
    private Map relationshipMap;
    private Map mappedBy;
    private String mappingStrategy = GrailsDomainClass.GORM;
    private Map<String, Object> defaultConstraints;

    private PersistentEntity persistentEntity;
    private LazyMappingContext mappingContext;

    /**
     * Constructor.
     * @param clazz
     * @param defaultConstraints
     */
    public DefaultGrailsDomainClass(Class<?> clazz, Map<String, Object> defaultConstraints) {
        super(clazz, "");
        this.defaultConstraints = defaultConstraints;
    }

    public DefaultGrailsDomainClass(Class<?> clazz, Map<String, Object> defaultConstraints, LazyMappingContext mappingContext) {
        this(clazz, defaultConstraints);
        this.mappingContext = mappingContext;
    }

    /**
     * Constructor.
     * @param clazz
     */
    public DefaultGrailsDomainClass(Class<?> clazz) {
        this(clazz, null);
    }

    private void initializeProperties() {
        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors();

        // First go through the properties of the class and create domain properties
        // populating into a map
        populateDomainClassProperties(propertyDescriptors);

        // if no identifier property throw exception
        if (identifier == null) {
            throw new GrailsDomainException("Identity property not found, but required in domain class ["+getFullName()+"]");
        }
        // if no version property throw exception
        if (version == null) {
            throw new GrailsDomainException("Version property not found, but required in domain class ["+getFullName()+"]");
        }
        // set properties from map values
        properties = propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);

        establishPersistentProperties();
    }

    private void verifyContextIsInitialized() {
        if (!mappingContext.isInitialized()) {
            throw new RuntimeException("That API cannot be accessed before the spring context is initialized");
        } else {
            log.warn("The GrailsDomainClass API should no longer be used to retrieve data about domain classes. Use the mapping context API instead");
            if (persistentEntity == null) {
                persistentEntity = mappingContext.getPersistentEntity(this.getFullName());
                if (persistentEntity == null) {
                    throw new RuntimeException("Could not retrieve the respective entity for domain " + this.getName() + " in the mapping context API");
                }
            }
        }
    }

    private void throwUnsupported() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support that operation because it is a proxy for the mapping context API");
    }

    public boolean hasSubClasses() {
        verifyContextIsInitialized();
        return !mappingContext.getChildEntities(persistentEntity).isEmpty();
    }

    /**
     * calculates the persistent properties from the evaluated properties
     */
    private void establishPersistentProperties() {
        Collection<GrailsDomainClassProperty> tempList = new ArrayList<GrailsDomainClassProperty>();
        for (Object o : propertyMap.values()) {
            GrailsDomainClassProperty currentProp = (GrailsDomainClassProperty) o;
            if (currentProp.getType() != Object.class && currentProp.isPersistent() &&
                    !currentProp.isIdentity() && !currentProp.getName().equals(GrailsDomainClassProperty.VERSION)) {
                tempList.add(currentProp);
            }
        }
        persistentProperties = tempList.toArray(new GrailsDomainClassProperty[tempList.size()]);
    }

    /**
     * Populates the domain class properties map
     *
     * @param propertyDescriptors The property descriptors
     */
    private void populateDomainClassProperties(PropertyDescriptor[] propertyDescriptors) {
        for (PropertyDescriptor descriptor : propertyDescriptors) {

            if (descriptor.getPropertyType() == null) {
                // indexed property
                continue;
            }

            // ignore certain properties
            if (GrailsDomainConfigurationUtil.isNotConfigurational(descriptor)) {
                GrailsDomainClassProperty property = new DefaultGrailsDomainClassProperty(this, descriptor, defaultConstraints, mappingContext);
                propertyMap.put(property.getName(), property);

                if (property.isIdentity()) {
                    identifier = property;
                }
                else if (property.getName().equals(GrailsDomainClassProperty.VERSION)) {
                    version = property;
                }
            }
        }
    }

    /**
     * Retrieves the association map
     */
    public Map getAssociationMap() {
        if (relationshipMap == null) {
            relationshipMap = getMergedConfigurationMap(GrailsDomainClassProperty.HAS_MANY);
        }
        return relationshipMap;
    }

    @SuppressWarnings("unchecked")
    private Map getMergedConfigurationMap(String propertyName) {
        Map configurationMap = getStaticPropertyValue(propertyName, Map.class);
        if (configurationMap == null) {
            configurationMap = new HashMap();
        }

        Class<?> theClass = getClazz();
        while (theClass != Object.class) {
            theClass = theClass.getSuperclass();
            ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(theClass);
            Map superRelationshipMap = propertyFetcher.getStaticPropertyValue(propertyName, Map.class);
            if (superRelationshipMap != null && !superRelationshipMap.equals(configurationMap)) {
                configurationMap.putAll(superRelationshipMap);
            }
        }
        return configurationMap;
    }

    public boolean isOwningClass(Class domainClass) {
        verifyContextIsInitialized();
        return persistentEntity.isOwningEntity(mappingContext.getPersistentEntity(domainClass.getName()));
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getProperties()
     */
    public GrailsDomainClassProperty[] getProperties() {
        if (properties.length == 0) {
            initializeProperties();
        }
        return properties;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getIdentifier()
     */
    public GrailsDomainClassProperty getIdentifier() {
        return identifier;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getVersion()
     */
    public GrailsDomainClassProperty getVersion() {
        return version;
    }

    public GrailsDomainClassProperty[] getPersistentProperties() {
        return persistentProperties;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getPropertyByName(java.lang.String)
     */
    public GrailsDomainClassProperty getPropertyByName(String name) {
        GrailsDomainClassProperty persistentProperty = getPersistentProperty(name);
        if (persistentProperty == null) {
            throw new InvalidPropertyException("No property found for name ["+name+"] for class ["+getClazz()+"]");
        }

        return persistentProperty;
    }

    public GrailsDomainClassProperty getPersistentProperty(String name) {
        if (propertyMap.containsKey(name)) {
            return propertyMap.get(name);
        }
        int indexOfDot = name.indexOf('.');
        if (indexOfDot > 0) {
            String basePropertyName = name.substring(0, indexOfDot);
            if (propertyMap.containsKey(basePropertyName)) {
                verifyContextIsInitialized();
                PersistentProperty prop = persistentEntity.getPropertyByName(basePropertyName);
                PersistentEntity referencedEntity = prop.getOwner();
                GrailsDomainClass referencedDomainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, referencedEntity.getName());
                if (referencedDomainClass != null) {
                    String restOfPropertyName = name.substring(indexOfDot + 1);
                    return referencedDomainClass.getPropertyByName(restOfPropertyName);
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
    * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getFieldName(java.lang.String)
    */
    public String getFieldName(String propertyName) {
        return getPropertyByName(propertyName).getFieldName();
    }

    /* (non-Javadoc)
     * @see org.grails.core.AbstractGrailsClass#getName()
     */
    @Override
    public String getName() {
        return ClassUtils.getShortName(super.getName());
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#isOneToMany(java.lang.String)
     */
    public boolean isOneToMany(String propertyName) {
        verifyContextIsInitialized();
        return getPropertyByName(propertyName).isOneToMany();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#isManyToOne(java.lang.String)
     */
    public boolean isManyToOne(String propertyName) {
        verifyContextIsInitialized();
        return getPropertyByName(propertyName).isManyToOne();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getRelationshipType(java.lang.String)
     */
    public Class<?> getRelatedClassType(String propertyName) {
        verifyContextIsInitialized();
        return persistentEntity.getPropertyByName(propertyName).getOwner().getJavaClass();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getPropertyName()
     */
    @Override
    public String getPropertyName() {
        return GrailsNameUtils.getPropertyNameRepresentation(getClazz());
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#isBidirectional()
     */
    public boolean isBidirectional(String propertyName) {
        verifyContextIsInitialized();
        return getPropertyByName(propertyName).isBidirectional();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getConstraints()
     */
    @SuppressWarnings("unchecked")
    public Map getConstrainedProperties() {
        Validator validator = getValidator();
        if (validator instanceof PersistentEntityValidator) {
            return ((PersistentEntityValidator) validator).getConstrainedProperties();
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getValidator()
     */
    public Validator getValidator() {
        verifyContextIsInitialized();
        return mappingContext.getValidatorRegistry().getValidator(persistentEntity);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#setValidator(Validator validator)
     */
    public void setValidator(Validator validator) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getMappedBy()
     */
    public String getMappingStrategy() {
        return mappingStrategy;
    }

    public boolean isRoot() {
        verifyContextIsInitialized();
        return persistentEntity.isRoot();
    }

    @SuppressWarnings("unchecked")
    public Set getSubClasses() {
        verifyContextIsInitialized();
        Set<GrailsDomainClass> subClasses = new LinkedHashSet<>();
        for (PersistentEntity e : mappingContext.getChildEntities(persistentEntity)) {
            subClasses.add((GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, e.getName()));
        }
        return subClasses;
    }

    public void refreshConstraints() {
        throwUnsupported();
    }

    ConstraintsEvaluator getConstraintsEvaluator() {
        if (grailsApplication != null && grailsApplication.getMainContext() != null) {
            final ApplicationContext context = grailsApplication.getMainContext();
            if (context.containsBean(ConstraintsEvaluator.BEAN_NAME)) {
                return context.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator.class);
            }
        }
        return GrailsFactoriesLoader.loadFactory(ConstraintsEvaluator.class, defaultConstraints);
    }

    public Map getMappedBy() {
        return mappedBy;
    }

    public boolean hasPersistentProperty(String propertyName) {
        for (GrailsDomainClassProperty persistentProperty : persistentProperties) {
            if (persistentProperty.getName().equals(propertyName)) return true;
        }
        return false;
    }

    public void setMappingStrategy(String strategy) {
        mappingStrategy = strategy;
    }

    public void addComponent(GrailsDomainClass component) {
        throwUnsupported();
    }

    public List<GrailsDomainClass> getComponents() {
        verifyContextIsInitialized();
        List<GrailsDomainClass> components = new ArrayList<>();
        for (Association a : persistentEntity.getAssociations()) {
            if (a.isEmbedded()) {
                components.add((GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, a.getAssociatedEntity().getName()));
            }
        }
        return components;
    }

    public List<GrailsDomainClassProperty> getAssociations() {
        verifyContextIsInitialized();
        List<GrailsDomainClassProperty> associations = new ArrayList<>();
        List<String> associationNames = new ArrayList<>();
        for (Association a : persistentEntity.getAssociations()) {
            associationNames.add(a.getName());
        }
        for (GrailsDomainClassProperty p : persistentProperties) {
            if (associationNames.contains(p.getName())) {
                associations.add(p);
            }
        }
        return associations;

    }
}
