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
import grails.core.DefaultGrailsApplication;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.util.GrailsNameUtils;
import grails.validation.Constrained;
import groovy.lang.MetaProperty;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.core.exceptions.GrailsDomainException;
import org.grails.core.exceptions.InvalidPropertyException;
import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.Simple;
import org.grails.datastore.mapping.reflect.NameUtils;
import org.grails.validation.discovery.ConstrainedDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Default implementation of the {@link GrailsDomainClass} interface
 *
 * @author Graeme Rocher
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
@Deprecated
public class DefaultGrailsDomainClass extends AbstractGrailsClass implements GrailsDomainClass, ComponentCapableDomainClass {

    private static final Logger log = LoggerFactory.getLogger(DefaultGrailsDomainClass.class);

    private GrailsDomainClassProperty identifier;
    private GrailsDomainClassProperty version;
    private GrailsDomainClassProperty[] properties;
    private GrailsDomainClassProperty[] persistentProperties;
    private Map<String, GrailsDomainClassProperty> propertyMap;
    private Map relationshipMap;
    private String mappingStrategy = GrailsDomainClass.GORM;

    private PersistentEntity persistentEntity;
    private MappingContext mappingContext;
    private Map<String, Constrained> constrainedProperties;
    private boolean propertiesInitialized;
    private Boolean autowire = null;
    /**
     * @param clazz
     * @param defaultConstraints
     */
    public DefaultGrailsDomainClass(Class<?> clazz, Map<String, Object> defaultConstraints) {
        this(clazz, defaultConstraints, null);
    }

    public DefaultGrailsDomainClass(Class<?> clazz, Map<String, Object> defaultConstraints, MappingContext mappingContext) {
        this(clazz, mappingContext);
        this.mappingContext = mappingContext;
    }

    public DefaultGrailsDomainClass(Class<?> clazz, MappingContext mappingContext) {
        this(clazz);
        this.mappingContext = mappingContext;
    }

    /**
     * Constructor.
     *
     * @param clazz
     */
    public DefaultGrailsDomainClass(Class<?> clazz) {
        super(clazz, "");
    }

    private void initializePersistentProperties() {
        if (!propertiesInitialized) {
            verifyContextIsInitialized();
            propertyMap = new LinkedHashMap<>();
            PersistentProperty identity = persistentEntity.getIdentity();
            if (identity != null) {
                identifier = new DefaultGrailsDomainClassProperty(this, persistentEntity, identity);
            }
            else {
                PersistentProperty[] compositeIdentity = persistentEntity.getCompositeIdentity();
                if(compositeIdentity != null) {
                    // use dummy. This is a horrible hack, but no current composite id support in this API
                    identity = new Simple(persistentEntity, persistentEntity.getMappingContext(), "id", Long.class) {
                        @Override
                        public PropertyMapping getMapping() {
                            return null;
                        }
                    };
                    identifier = new DefaultGrailsDomainClassProperty(this, persistentEntity, identity);
                    for (PersistentProperty property : compositeIdentity) {
                        propertyMap.put(property.getName(), new DefaultGrailsDomainClassProperty(this, persistentEntity, property));
                    }
                }
            }

            // First go through the properties of the class and create domain properties
            // populating into a map
            populateDomainClassProperties();
            // set properties from map values
            persistentProperties = propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);

            // if no identifier property throw exception
            if (identifier == null ) {
                throw new GrailsDomainException("Identity property not found, but required in domain class [" + getFullName() + "]");
            } else if(identifier != null){
                propertyMap.put(identifier.getName(), identifier);
            }
            // if no version property throw exception
            if (version != null) {
                propertyMap.put(version.getName(), version);
            }


            List<MetaProperty> properties = getMetaClass().getProperties();
            for (MetaProperty property : properties) {
                String name = property.getName();
                if(!propertyMap.containsKey(name) && !NameUtils.isConfigurational(name) && !Modifier.isStatic(property.getModifiers())) {
                    propertyMap.put(name, new MetaGrailsDomainClassProperty(this, property));
                }
            }
            this.properties = propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);
        }
        propertiesInitialized = true;
    }

    private void verifyContextIsInitialized() {
        if (mappingContext == null) {
            throw new GrailsConfigurationException("That API cannot be accessed before the spring context is initialized");
        } else {
            if (log.isWarnEnabled()) {
                log.warn("The GrailsDomainClass API should no longer be used to retrieve data about domain classes. Use the mapping context API instead");
            }
            if (persistentEntity == null) {
                persistentEntity = mappingContext.getPersistentEntity(this.getFullName());
                if (persistentEntity == null) {
                    MappingContext concreteMappingContext = getApplication().getMappingContext();
                    if(concreteMappingContext.getClass() == KeyValueMappingContext.class) {
                        // In a unit testing context, allow
                        persistentEntity = concreteMappingContext.addPersistentEntity(getClazz());
                    }
                    else {
                        throw new GrailsConfigurationException("Could not retrieve the respective entity for domain " + this.getName() + " in the mapping context API");
                    }
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
     * Populates the domain class properties map
     */
    private void populateDomainClassProperties() {
        for (PersistentProperty property : persistentEntity.getPersistentProperties()) {
            GrailsDomainClassProperty grailsProperty = new DefaultGrailsDomainClassProperty(this, persistentEntity, property);
            if (grailsProperty.getName().equals(GrailsDomainClassProperty.VERSION)) {
                version = grailsProperty;
            } else {
                propertyMap.put(grailsProperty.getName(), grailsProperty);
            }
        }
    }

    /**
     * Retrieves the association map
     *
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    public Map getAssociationMap() {
        if (relationshipMap == null) {
            relationshipMap = getMergedConfigurationMap();
        }
        return relationshipMap;
    }

    @SuppressWarnings("unchecked")
    private Map getMergedConfigurationMap() {
        verifyContextIsInitialized();
        Map configurationMap = new HashMap();
        PersistentEntity currentEntity = this.persistentEntity;
        while (currentEntity != null) {

            List<Association> associations = currentEntity.getAssociations();

            for (Association association : associations) {
                PersistentEntity associatedEntity = association.getAssociatedEntity();
                if (associatedEntity != null) {
                    configurationMap.put(association.getName(), associatedEntity.getJavaClass());
                }
            }
            currentEntity = currentEntity.getParentEntity();
        }

        return configurationMap;
    }

    @Override
    public boolean isAutowire() {
        if(autowire == null) {
            verifyContextIsInitialized();
            autowire = persistentEntity.getMapping().getMappedForm().isAutowire();
        }
        return autowire;
    }

    public boolean isOwningClass(Class domainClass) {
        verifyContextIsInitialized();
        return persistentEntity.isOwningEntity(mappingContext.getPersistentEntity(domainClass.getName()));
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getProperties()
     */
    public GrailsDomainClassProperty[] getProperties() {
        initializePersistentProperties();
        return properties;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getIdentifier()
     */
    public GrailsDomainClassProperty getIdentifier() {
        initializePersistentProperties();
        return identifier;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getVersion()
     */
    public GrailsDomainClassProperty getVersion() {
        initializePersistentProperties();
        return version;
    }

    public GrailsDomainClassProperty[] getPersistentProperties() {
        initializePersistentProperties();
        return persistentProperties;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getPropertyByName(java.lang.String)
     */
    public GrailsDomainClassProperty getPropertyByName(String name) {
        GrailsDomainClassProperty persistentProperty = getPersistentProperty(name);
        if (persistentProperty == null) {
            MetaProperty metaProperty = getMetaClass().getMetaProperty(name);
            if (metaProperty != null) {
                return new MetaGrailsDomainClassProperty(this, metaProperty);
            } else {
                throw new InvalidPropertyException("No property found for name [" + name + "] for class [" + getClazz() + "]");
            }
        }

        return persistentProperty;
    }

    public GrailsDomainClassProperty getPersistentProperty(String name) {
        initializePersistentProperties();
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
                GrailsDomainClass referencedDomainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, referencedEntity.getName());
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
        verifyContextIsInitialized();
        if(constrainedProperties == null) {
            ConstrainedDiscovery constrainedDiscovery = GrailsFactoriesLoader.loadFactory(ConstrainedDiscovery.class);
            if(constrainedDiscovery == null) {
                constrainedProperties = Collections.emptyMap();
            }
            else {
                constrainedProperties = constrainedDiscovery.findConstrainedProperties(persistentEntity);
            }
        }
        return constrainedProperties;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getValidator()
     */
    public Validator getValidator() {
        verifyContextIsInitialized();
        return mappingContext.getEntityValidator(persistentEntity);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#setValidator(Validator validator)
     */
    public void setValidator(Validator validator) {
        verifyContextIsInitialized();
        mappingContext.addEntityValidator(persistentEntity, validator);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClass#getMappedBy()
     */
    public String getMappingStrategy() {
        return mappingStrategy;
    }

    /**
     * Check whether the class is a root entity
     *
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    @Override
    public boolean isRoot() {
        verifyContextIsInitialized();
        return persistentEntity.isRoot();
    }

    /**
     * Obtains a Set of subclasses
     *
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public Set getSubClasses() {
        verifyContextIsInitialized();
        Set<GrailsDomainClass> subClasses = new LinkedHashSet<>();
        for (PersistentEntity e : mappingContext.getChildEntities(persistentEntity)) {
            subClasses.add((GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, e.getName()));
        }
        return subClasses;
    }

    @Deprecated
    public void refreshConstraints() {
        throwUnsupported();
    }

    @Override
    public Map getMappedBy() {
        throwUnsupported();
        return null;
    }

    public boolean hasPersistentProperty(String propertyName) {
        verifyContextIsInitialized();
        return persistentEntity.getPropertyByName(propertyName) != null;
    }

    public void setMappingStrategy(String strategy) {
        mappingStrategy = strategy;
    }

    @Deprecated
    public void addComponent(GrailsDomainClass component) {
        throwUnsupported();
    }

    /**
     * Retrieves a list of embedded components
     *
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    public List<GrailsDomainClass> getComponents() {
        verifyContextIsInitialized();
        List<GrailsDomainClass> components = new ArrayList<>();
        for (Association a : persistentEntity.getAssociations()) {
            if (a.isEmbedded()) {
                components.add((GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, a.getAssociatedEntity().getName()));
            }
        }
        return components;
    }

    /**
     * Retrieves a list of associations
     *
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
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
