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

import grails.util.GrailsNameUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.exceptions.GrailsDomainException;
import org.codehaus.groovy.grails.exceptions.InvalidPropertyException;
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator;
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Validator;

/**
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsDomainClass extends AbstractGrailsClass implements GrailsDomainClass, ComponentCapableDomainClass {

    private GrailsDomainClassProperty identifier;
    private GrailsDomainClassProperty version;
    private GrailsDomainClassProperty[] properties;
    private GrailsDomainClassProperty[] persistentProperties;
    private Map<String, GrailsDomainClassProperty> propertyMap;
    private Map relationshipMap;
    private Map hasOneMap;

    private Map constraints;
    private Map mappedBy;
    private Validator validator;
    private String mappingStrategy = GrailsDomainClass.GORM;
    private List<Class<?>> owners = new ArrayList<Class<?>>();
    private boolean root = true;
    private Set subClasses = new HashSet();
    private Collection<String> embedded;
    private Map<String, Object> defaultConstraints;
    private List<GrailsDomainClass> components = new ArrayList<GrailsDomainClass>();
    private List<String> dataSources;

    /**
     * Constructor.
     * @param clazz
     * @param defaultConstraints
     */
    public DefaultGrailsDomainClass(Class<?> clazz, Map<String, Object> defaultConstraints) {
        super(clazz, "");
        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors();

        final Class<?> superClass = clazz.getSuperclass();
        if (DomainClassArtefactHandler.isDomainClass(superClass)) {
            root = false;
        }
        propertyMap = new LinkedHashMap<String, GrailsDomainClassProperty>();
        relationshipMap = getAssociationMap();
        embedded = getEmbeddedList();
        this.defaultConstraints = defaultConstraints;

        // get mapping strategy by setting
        mappingStrategy = getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String.class);
        if (mappingStrategy == null) {
            mappingStrategy = GORM;
        }

        // get any mappedBy settings
        mappedBy = getStaticPropertyValue(GrailsDomainClassProperty.MAPPED_BY, Map.class);
        hasOneMap = getStaticPropertyValue(GrailsDomainClassProperty.HAS_ONE, Map.class);
        if (hasOneMap == null) {
            hasOneMap = Collections.emptyMap();
        }

        if (mappedBy == null) {
            mappedBy = Collections.emptyMap();
        }

        // establish the owners of relationships
        establishRelationshipOwners();

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

        // establish relationships
        establishRelationships();

        // set persistent properties
        establishPersistentProperties();
    }

    /**
     * Constructor.
     * @param clazz
     */
    public DefaultGrailsDomainClass(Class<?> clazz) {
        this(clazz, null);
    }

    public boolean hasSubClasses() {
        return !getSubClasses().isEmpty();
    }

    /**
     * calculates the persistent properties from the evaluated properties
     */
    private void establishPersistentProperties() {
        Collection<GrailsDomainClassProperty> tempList = new ArrayList<GrailsDomainClassProperty>();
        for (Object o : propertyMap.values()) {
            GrailsDomainClassProperty currentProp = (GrailsDomainClassProperty) o;
            if (currentProp.getType() != Object.class && currentProp.isPersistent() && !currentProp.isIdentity() && !currentProp.getName().equals(GrailsDomainClassProperty.VERSION)) {
                tempList.add(currentProp);
            }
        }
        persistentProperties = tempList.toArray(new GrailsDomainClassProperty[tempList.size()]);
    }

    /**
     * Evaluates the belongsTo property to find out who owns who
     */
    @SuppressWarnings("unchecked")
    private void establishRelationshipOwners() {
        Class<?> belongsTo = getStaticPropertyValue(GrailsDomainClassProperty.BELONGS_TO, Class.class);
        if (belongsTo == null) {
            List ownersProp = getStaticPropertyValue(GrailsDomainClassProperty.BELONGS_TO, List.class);
            if (ownersProp != null) {
                owners = ownersProp;
            }
            else {
                Map ownersMap = getStaticPropertyValue(GrailsDomainClassProperty.BELONGS_TO, Map.class);
                if (ownersMap!=null) {
                    owners = new ArrayList(ownersMap.values());
                }
            }
        }
        else {
            owners = new ArrayList();
            owners.add(belongsTo);
        }
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
                GrailsDomainClassProperty property = new DefaultGrailsDomainClassProperty(this, descriptor);
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
    @SuppressWarnings("unchecked")
    public Map getAssociationMap() {
        if (relationshipMap == null) {
            relationshipMap = getStaticPropertyValue(GrailsDomainClassProperty.HAS_MANY, Map.class);
            if (relationshipMap == null) {
                relationshipMap = new HashMap();
            }

            Class<?> theClass = getClazz();
            while (theClass != Object.class) {
                theClass = theClass.getSuperclass();
                ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(theClass);
                Map superRelationshipMap = propertyFetcher.getStaticPropertyValue(GrailsDomainClassProperty.HAS_MANY, Map.class);
                if (superRelationshipMap != null && !superRelationshipMap.equals(relationshipMap)) {
                    relationshipMap.putAll(superRelationshipMap);
                }
            }
        }
        return relationshipMap;
    }

    /**
     * Retrieves the list of known embedded component types
     *
     * @return A list of embedded components
     */
    @SuppressWarnings("unchecked")
    private Collection<String> getEmbeddedList() {
        Collection potentialList = getStaticPropertyValue(GrailsDomainClassProperty.EMBEDDED, Collection.class);
        if (potentialList != null) {
            return potentialList;
        }
        return Collections.emptyList();
    }

    /**
     * Calculates the relationship type based other types referenced
     */
    private void establishRelationships() {
        for (Object o : propertyMap.values()) {
            DefaultGrailsDomainClassProperty currentProp = (DefaultGrailsDomainClassProperty) o;
            if (!currentProp.isPersistent()) continue;

            Class currentPropType = currentProp.getType();
            // establish if the property is a one-to-many
            // if it is a Set and there are relationships defined
            // and it is defined as persistent
            if (currentPropType != null) {
                if (Collection.class.isAssignableFrom(currentPropType) || Map.class.isAssignableFrom(currentPropType)) {
                    establishRelationshipForCollection(currentProp);
                }
                // otherwise if the type is a domain class establish relationship
                else if (DomainClassArtefactHandler.isDomainClass(currentPropType) &&
                        currentProp.isPersistent()) {
                    establishDomainClassRelationship(currentProp);
                }
                else if (embedded.contains(currentProp.getName())) {
                    establishDomainClassRelationship(currentProp);
                }
            }
        }
    }

    /**
     * Establishes a relationship for a java.util.Set
     *
     * @param property The collection property
     */
    private void establishRelationshipForCollection(DefaultGrailsDomainClassProperty property) {
        // is it a relationship
        Class<?> relatedClassType = getRelatedClassType(property.getName());


        if (relatedClassType != null) {
            // set the referenced type in the property
            property.setReferencedPropertyType(relatedClassType);

            // if the related type is a domain class
            // then figure out what kind of relationship it is
            if (DomainClassArtefactHandler.isDomainClass(relatedClassType)) {

                // check the relationship defined in the referenced type
                // if it is also a Set/domain class etc.
                Map relatedClassRelationships = GrailsDomainConfigurationUtil.getAssociationMap(relatedClassType);
                Class<?> relatedClassPropertyType = null;

                // First check whether there is an explicit relationship
                // mapping for this property (as provided by "mappedBy").
                String mappingProperty = (String)mappedBy.get(property.getName());
                if (!StringUtils.isBlank(mappingProperty)) {
                    // First find the specified property on the related class, if it exists.
                    PropertyDescriptor pd = findProperty(GrailsClassUtils.getPropertiesOfType(relatedClassType, getClazz()), mappingProperty);

                    // If a property of the required type does not exist, search
                    // for any collection properties on the related class.
                    if (pd == null) pd = findProperty(GrailsClassUtils.getPropertiesAssignableToType(relatedClassType, Collection.class), mappingProperty);

                    // We've run out of options. The given "mappedBy"
                    // setting is invalid.
                    if (pd == null) {
                        throw new GrailsDomainException("Non-existent mapping property ["+mappingProperty+"] specified for property ["+property.getName()+"] in class ["+getClazz()+"]");
                    }

                    // Tie the properties together.
                    relatedClassPropertyType = pd.getPropertyType();
                    property.setReferencePropertyName(pd.getName());
                }
                else {
                    // if the related type has a relationships map it may be a many-to-many
                    // figure out if there is a many-to-many relationship defined
                    if (isRelationshipManyToMany(property, relatedClassType, relatedClassRelationships)) {
                        String relatedClassPropertyName = null;
                        Map relatedClassMappedBy = GrailsDomainConfigurationUtil.getMappedByMap(relatedClassType);
                        // retrieve the relationship property
                        for (Object o : relatedClassRelationships.keySet()) {
                            String currentKey = (String) o;
                            String mappedByProperty = (String) relatedClassMappedBy.get(currentKey);
                            if (mappedByProperty != null && !mappedByProperty.equals(property.getName())) continue;
                            Class<?> currentClass = (Class<?>)relatedClassRelationships.get(currentKey);
                            if (currentClass.isAssignableFrom(getClazz())) {
                                relatedClassPropertyName = currentKey;
                                break;
                            }
                        }

                        // if there is one defined get the type
                        if (relatedClassPropertyName != null) {
                            relatedClassPropertyType = GrailsClassUtils.getPropertyType(relatedClassType, relatedClassPropertyName);
                        }
                    }
                    // otherwise figure out if there is a one-to-many relationship by retrieving any properties that are of the related type
                    // if there is more than one property then (for the moment) ignore the relationship
                    if (relatedClassPropertyType == null) {
                        PropertyDescriptor[] descriptors = GrailsClassUtils.getPropertiesOfType(relatedClassType, getClazz());

                        if (descriptors.length == 1) {
                            relatedClassPropertyType = descriptors[0].getPropertyType();
                            property.setReferencePropertyName(descriptors[0].getName());
                        }
                        else if (descriptors.length > 1) {
                            // try now to use the class name by convention
                            String classPropertyName = getPropertyName();
                            PropertyDescriptor pd = findProperty(descriptors, classPropertyName);
                            if (pd == null) {
                                throw new GrailsDomainException("Property ["+property.getName()+"] in class ["+getClazz()+"] is a bidirectional one-to-many with two possible properties on the inverse side. "+
                                        "Either name one of the properties on other side of the relationship ["+classPropertyName+"] or use the 'mappedBy' static to define the property " +
                                        "that the relationship is mapped with. Example: static mappedBy = ["+property.getName()+":'myprop']");
                            }
                            relatedClassPropertyType = pd.getPropertyType();
                            property.setReferencePropertyName(pd.getName());
                        }
                    }
                }

                establishRelationshipForSetToType(property,relatedClassPropertyType);
                // if its a many-to-many figure out the owning side of the relationship
                if (property.isManyToMany()) {
                    establishOwnerOfManyToMany(property, relatedClassType);
                }
            }
            // otherwise set it to not persistent as you can't persist
            // relationships to non-domain classes
            else {
                property.setBasicCollectionType(true);
            }
        }
        else if (!Map.class.isAssignableFrom(property.getType())) {
            // no relationship defined for set.
            // set not persistent
            property.setPersistent(false);
        }
    }

    /**
     * Finds a property type is an array of descriptors for the given property name
     *
     * @param descriptors The descriptors
     * @param propertyName The property name
     * @return The Class or null
     */
    private PropertyDescriptor findProperty(PropertyDescriptor[] descriptors, String propertyName) {
        PropertyDescriptor d = null;
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getName().equals(propertyName)) {
                d = descriptor;
                break;
            }
        }
        return d;
    }

    /**
     * Find out if the relationship is a many-to-many
     *
     * @param property The property
     * @param relatedClassType The related type
     * @param relatedClassRelationships The related types relationships
     * @return <code>true</code> if the relationship is a many-to-many
     */
    private boolean isRelationshipManyToMany(DefaultGrailsDomainClassProperty property,
            Class<?> relatedClassType, Map relatedClassRelationships) {
        return relatedClassRelationships != null &&
            !relatedClassRelationships.isEmpty() &&
            !relatedClassType.equals(property.getDomainClass().getClazz());
    }

    /**
     * Inspects a related classes' ownership settings against this properties class' ownership
     * settings to find out who owns a many-to-many relationship
     *
     * @param property The property
     * @param relatedClassType The related type
     */
    @SuppressWarnings("unchecked")
    private void establishOwnerOfManyToMany(DefaultGrailsDomainClassProperty property, Class<?> relatedClassType) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(relatedClassType);
        Object relatedBelongsTo = cpf.getPropertyValue(GrailsDomainClassProperty.BELONGS_TO);
        boolean owningSide = false;
        boolean relatedOwner = isOwningSide(relatedClassType, owners);
        final Class<?> propertyClass = property.getDomainClass().getClazz();
        if (relatedBelongsTo instanceof Collection) {
            final Collection associatedOwners = (Collection)relatedBelongsTo;
            owningSide = isOwningSide(propertyClass, associatedOwners);
        }
        else if (relatedBelongsTo instanceof Class) {
            final Collection associatedOwners = new ArrayList();
            associatedOwners.add(relatedBelongsTo);
            owningSide = isOwningSide(propertyClass, associatedOwners);
        }
        property.setOwningSide(owningSide);
        if (relatedOwner && property.isOwningSide()) {
            throw new GrailsDomainException("Domain classes [" + propertyClass +
                    "] and [" + relatedClassType +
                    "] cannot own each other in a many-to-many relationship. Both contain belongsTo definitions that reference each other.");
        }
        if (!relatedOwner && !property.isOwningSide() && !(property.isCircular() && property.isManyToMany())) {
            throw new GrailsDomainException("No owner defined between domain classes ["+propertyClass+"] and ["+relatedClassType+"] in a many-to-many relationship. Example: static belongsTo = "+relatedClassType.getName());
        }
    }

    private boolean isOwningSide(Class<?> relatedClassType, Collection<Class<?>> potentialOwners) {
        boolean relatedOwner = false;
        for (Class<?> relatedClass : potentialOwners) {
            if (relatedClass.isAssignableFrom(relatedClassType)) {
                relatedOwner = true;
                break;
            }
        }
        return relatedOwner;
    }

    /**
     * Establishes whether the relationship is a bi-directional or uni-directional one-to-many
     * and applies the appropriate settings to the specified property
     *
     * @param property The property to apply settings to
     * @param relatedClassPropertyType The related type
     */
    private void establishRelationshipForSetToType(DefaultGrailsDomainClassProperty property,
            Class<?> relatedClassPropertyType) {

        if (relatedClassPropertyType == null) {
            // uni-directional one-to-many
            property.setOneToMany(true);
            property.setBidirectional(false);
        }
        else if (Collection.class.isAssignableFrom(relatedClassPropertyType) ||
                 Map.class.isAssignableFrom(relatedClassPropertyType)) {
            // many-to-many
            property.setManyToMany(true);
            property.setBidirectional(true);
        }
        else if (DomainClassArtefactHandler.isDomainClass(relatedClassPropertyType)) {
            // bi-directional one-to-many
            property.setOneToMany(true);
            property.setBidirectional(true);
        }
    }

    /**
     * Establish relationship with related domain class
     *
     * @param property Establishes a relationship between this class and the domain class property
     */
    private void establishDomainClassRelationship(DefaultGrailsDomainClassProperty property) {
        Class<?> propType = property.getType();
        if (embedded.contains(property.getName())) {
            property.setEmbedded(true);
            return;
        }

        // establish relationship to type
        Map relatedClassRelationships = GrailsDomainConfigurationUtil.getAssociationMap(propType);
        @SuppressWarnings("hiding")
        Map mappedBy = GrailsDomainConfigurationUtil.getMappedByMap(propType);

        Class<?> relatedClassPropertyType = null;

        // if there is a relationships map use that to find out
        // whether it is mapped to a Set
        if (relatedClassRelationships != null && !relatedClassRelationships.isEmpty()) {

            String relatedClassPropertyName = findOneToManyThatMatchesType(property, relatedClassRelationships);
            PropertyDescriptor[] descriptors = GrailsClassUtils.getPropertiesOfType(getClazz(), property.getType());

            // if there is only one property on many-to-one side of the relationship then
            // try to establish if it is bidirectional
            if (descriptors.length == 1 && isNotMappedToDifferentProperty(property,relatedClassPropertyName, mappedBy)) {
                if (!StringUtils.isBlank(relatedClassPropertyName)) {
                    property.setReferencePropertyName(relatedClassPropertyName);
                    // get the type of the property
                    relatedClassPropertyType = GrailsClassUtils.getPropertyType(propType, relatedClassPropertyName);
                }
            }
            // if there is more than one property on the many-to-one side then we need to either
            // find out if there is a mappedBy property or whether a convention is used to decide
            // on the mapping property
            else if (descriptors.length > 1) {
                if (mappedBy.containsValue(property.getName())) {
                    for (Object o : mappedBy.keySet()) {
                        String mappedByPropertyName = (String) o;
                        if (property.getName().equals(mappedBy.get(mappedByPropertyName))) {
                            Class<?> mappedByRelatedType = (Class<?>) relatedClassRelationships.get(mappedByPropertyName);
                            if (mappedByRelatedType != null && propType.isAssignableFrom(mappedByRelatedType))
                                relatedClassPropertyType = GrailsClassUtils.getPropertyType(propType, mappedByPropertyName);
                        }
                    }
                }
                else {
                    String classNameAsProperty = GrailsNameUtils.getPropertyName(propType);
                    if (property.getName().equals(classNameAsProperty) && !mappedBy.containsKey(relatedClassPropertyName)) {
                        relatedClassPropertyType = GrailsClassUtils.getPropertyType(propType, relatedClassPropertyName);
                    }
                }
            }
        }
        // otherwise retrieve all the properties of the type from the associated class
        if (relatedClassPropertyType == null) {
            PropertyDescriptor[] descriptors = GrailsClassUtils.getPropertiesOfType(propType, getClazz());

            // if there is only one then the association is established
            if (descriptors.length == 1) {
                relatedClassPropertyType = descriptors[0].getPropertyType();
            }
        }

        //    establish relationship based on this type
        establishDomainClassRelationshipToType(property, relatedClassPropertyType);
    }

    private boolean isNotMappedToDifferentProperty(GrailsDomainClassProperty property,
            String relatedClassPropertyName, @SuppressWarnings("hiding") Map mappedBy) {

        String mappedByForRelation = (String)mappedBy.get(relatedClassPropertyName);
        if (mappedByForRelation == null) return true;
        if (!property.getName().equals(mappedByForRelation)) return false;
        return true;
    }

    private String findOneToManyThatMatchesType(DefaultGrailsDomainClassProperty property, Map relatedClassRelationships) {
        String relatedClassPropertyName = null;

        for (Object o : relatedClassRelationships.keySet()) {
            String currentKey = (String) o;
            Class<?> currentClass = (Class<?>)relatedClassRelationships.get(currentKey);

            if (property.getDomainClass().getClazz().getName().equals(currentClass.getName())) {
                relatedClassPropertyName = currentKey;
                break;
            }
        }
        return relatedClassPropertyName;
    }

    private void establishDomainClassRelationshipToType(DefaultGrailsDomainClassProperty property, Class<?> relatedClassPropertyType) {
        // uni-directional one-to-one

        if (relatedClassPropertyType == null) {
            if (hasOneMap.containsKey(property.getName())) {
                property.setHasOne(true);
            }
            property.setOneToOne(true);
            property.setBidirectional(false);
        }
        // bi-directional many-to-one
        else if (Collection.class.isAssignableFrom(relatedClassPropertyType)||Map.class.isAssignableFrom(relatedClassPropertyType)) {
            property.setManyToOne(true);
            property.setBidirectional(true);
        }
        // bi-directional one-to-one
        else if (DomainClassArtefactHandler.isDomainClass(relatedClassPropertyType)) {
            if (hasOneMap.containsKey(property.getName())) {
                property.setHasOne(true);
            }

            property.setOneToOne(true);
            if (!getClazz().equals(relatedClassPropertyType)) {
                property.setBidirectional(true);
            }
        }
    }

    public boolean isOwningClass(Class domainClass) {
        return owners.contains(domainClass);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getProperties()
     */
    public GrailsDomainClassProperty[] getProperties() {
        return properties;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getIdentifier()
     */
    public GrailsDomainClassProperty getIdentifier() {
        return identifier;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getVersion()
     */
    public GrailsDomainClassProperty getVersion() {
        return version;
    }

    /**
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getPersistantProperties()
     */
    @Deprecated
    public GrailsDomainClassProperty[] getPersistantProperties() {
        return persistentProperties;
    }

    public GrailsDomainClassProperty[] getPersistentProperties() {
        return persistentProperties;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#getPropertyByName(java.lang.String)
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
                GrailsDomainClassProperty prop = propertyMap.get(basePropertyName);
                GrailsDomainClass referencedDomainClass = prop.getReferencedDomainClass();
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
     * @see org.codehaus.groovy.grails.commons.AbstractGrailsClass#getName()
     */
    @Override
    public String getName() {
        return ClassUtils.getShortClassName(super.getName());
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#isOneToMany(java.lang.String)
     */
    public boolean isOneToMany(String propertyName) {
        return getPropertyByName(propertyName).isOneToMany();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClass#isManyToOne(java.lang.String)
     */
    public boolean isManyToOne(String propertyName) {
        return getPropertyByName(propertyName).isManyToOne();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getRelationshipType(java.lang.String)
     */
    public Class<?> getRelatedClassType(String propertyName) {
        return (Class<?>)relationshipMap.get(propertyName);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getPropertyName()
     */
    @Override
    public String getPropertyName() {
        return GrailsNameUtils.getPropertyNameRepresentation(getClazz());
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#isBidirectional()
     */
    public boolean isBidirectional(String propertyName) {
        return getPropertyByName(propertyName).isBidirectional();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getConstraints()
     */
    @SuppressWarnings("unchecked")
    public Map getConstrainedProperties() {
        if (constraints == null) {
            initializeConstraints();
        }
        return Collections.unmodifiableMap(constraints);
    }

    private void initializeConstraints() {
        // process the constraints
        final ConstraintsEvaluator constraintsEvaluator = getConstraintsEvaluator();
        constraints = constraintsEvaluator.evaluate(getClazz(), persistentProperties);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getValidator()
     */
    public Validator getValidator() {
        return validator;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#setValidator(Validator validator)
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass#getMappedBy()
     */
    public String getMappingStrategy() {
        return mappingStrategy;
    }

    public boolean isRoot() {
        return root;
    }

    @SuppressWarnings("unchecked")
    public Set getSubClasses() {
        return subClasses;
    }

    public void refreshConstraints() {
        final ConstraintsEvaluator constraintEvaluator = getConstraintsEvaluator();
        if (defaultConstraints!=null) {

            constraints = constraintEvaluator.evaluate(getClazz(), persistentProperties);
        }
        else {
            constraints = constraintEvaluator.evaluate(getClazz(), persistentProperties);
        }

        // Embedded components have their own ComponentDomainClass instance which
        // won't be refreshed by the application. So, we have to do it here.
        for (GrailsDomainClassProperty property : persistentProperties) {
            if (property.isEmbedded()) {
                property.getComponent().refreshConstraints();
            }
        }
    }

    private ConstraintsEvaluator getConstraintsEvaluator() {
        if (grailsApplication != null && grailsApplication.getMainContext() != null) {
            final ApplicationContext context = grailsApplication.getMainContext();
            if (context.containsBean(ConstraintsEvaluator.BEAN_NAME)) {
                return context.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator.class);
            }
        }
        return new DefaultConstraintEvaluator(defaultConstraints);
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
        this.components.add(component);
    }

    public List<GrailsDomainClass> getComponents() {
        return Collections.unmodifiableList(components);
    }

    @SuppressWarnings("unchecked")
    public List<String> getDataSources() {
        if (dataSources == null) {
            dataSources = getStaticPropertyValue(GrailsDomainClassProperty.DATA_SOURCES, List.class);
            if (dataSources == null) {
                dataSources = Collections.singletonList(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE);
            }
        }

        return dataSources;
    }

    public boolean usesDataSource(final String name) {

        // TODO handle subclassing

        if (getDataSources().contains(name)) {
            return true;
        }

        if (getDataSources().contains(GrailsDomainClassProperty.ALL_DATA_SOURCES)) {
            return true;
        }

        return false;
    }
}
