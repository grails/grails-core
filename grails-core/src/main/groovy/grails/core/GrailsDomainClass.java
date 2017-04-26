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
package grails.core;

import java.util.Map;
import java.util.Set;

import org.springframework.validation.Validator;

/**
 * Represents a persistable Grails domain class.
 *
 * @author Graeme Rocher
 * @since Jul 5, 2005
 *
 * @deprecated Use {@link org.grails.datastore.mapping.model.PersistentEntity} instead
 */
@Deprecated
public interface GrailsDomainClass extends GrailsClass {

    /**
     * The name of the default ORM implementation used to map the class
     */
    String GORM = "GORM";

    String ORM_MAPPING = "mapping";

    /**
     * @return Whether to autowire
     */
    boolean isAutowire();
    /**
     * @param domainClass
     * @return true if the specifying domain class is on the owning side of a relationship
     */
    @SuppressWarnings("rawtypes")
    boolean isOwningClass(Class domainClass);

    /**
     * Returns all of the properties of the domain class
     * @return The domain class properties
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty[] getProperties();

    /**
     * Returns all of the persistant properties of the domain class
     * @return The domain class' persistant properties
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty[] getPersistentProperties();

    /**
     * Returns the identifier property
     * @return The identifier property
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty getIdentifier();

    /**
     * Returns the version property
     * @return The version property
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty getVersion();

    /**
     * Returns this classes association map
     * @return The association map
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    Map getAssociationMap();

    /**
     * Returns the property for the given name
     *
     * @param name The property for the name
     * @throws org.grails.core.exceptions.InvalidPropertyException
     * @return The domain class property for the given name
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty getPropertyByName(String name);

    /**
     * Returns the property for the given name or null if non exists
     *
     * @param name The property name
     * @return The property or null
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    GrailsDomainClassProperty getPersistentProperty(String name);

    /**
     * Returns the field name for the given property name
     * @param propertyName
     * @return The field representation of the property name
     */
    String getFieldName(String propertyName);

    /**
     * <p>Returns the default property name of the GrailsClass. For example the property name for
     * a class called "User" would be "user"</p>
     *
     * @return The property name representation of the class name
     */
    String getPropertyName();

    /**
     * Returns true if the given property is a one to many relationship
     * @param propertyName The name of the property
     * @return A boolean value
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean isOneToMany(String propertyName);

    /**
     * Returns true if the given property is a many to one relationship
     * @param propertyName The name of the property
     * @return A boolean value
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean isManyToOne(String propertyName);

    /**
     * Returns true if the given property is a bi-directional relationship
     *
     * @param propertyName The name of the property
     * @return A boolean value
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean isBidirectional(String propertyName);

    /**
     * Returns the type of the related class of the given property
     *
     * @param propertyName The name of the property
     * @return The type of the class or null if no relationship exists for the specified property
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    Class getRelatedClassType(String propertyName);

    /**
     * Returns a map of constraints applied to this domain class with the keys being the property name
     * and the values being ConstrainedProperty instances
     *
     * @return A map of constraints
     */
    @SuppressWarnings("rawtypes")
    Map getConstrainedProperties();

    /**
     * Retreives the validator for this domain class
     *
     * @return A validator instance or null if none exists
     */
    Validator getValidator();

    /**
     * Sets the validator for this domain class
     *
     * @param validator The domain class validator to set
     */
    void setValidator(Validator validator);

    /**
     * @return The name of the ORM implementation used to map the domain class (default is "GORM")
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    String getMappingStrategy();

    /**
     * Whether the class is the root of a hierarchy
     *
     * @return true if it is the root of the hierarchy
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean isRoot();

    /**
     * Returns the sub-classes for this class if any
     *
     * @return A set of sub classes or an empty set if none exist
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    Set<GrailsDomainClass> getSubClasses();

    /**
     * Refreshes the constraint defined on a domain class
     */
    @Deprecated
    void refreshConstraints();

    /**
     * Returns true if the domain classes has sub classes
     * @return true if it does
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean hasSubClasses();

    /**
     * @return The map that defines association mappings
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    Map getMappedBy();

    /**
     * Returns true if this domain class has a persistent property for the given name
     * @param propertyName The property name
     * @return true if it does
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    boolean hasPersistentProperty(String propertyName);

    /**
     * Sets the strategy to use for ORM mapping. Default is GORM
     * @param strategy The mapping strategy
     * @deprecated Use {@link org.grails.datastore.mapping.model.MappingContext} API instead
     */
    @Deprecated
    void setMappingStrategy(String strategy);
}
