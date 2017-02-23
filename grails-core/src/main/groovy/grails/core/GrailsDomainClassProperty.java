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

import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.datastore.mapping.model.config.GormProperties;

/**
 * A property of a GrailsDomainClass instance.
 *
 * @author Graeme Rocher
 * @deprecated Use the {@link org.grails.datastore.mapping.model.MappingContext} API instead
 */
@Deprecated
public interface GrailsDomainClassProperty {

    String IDENTITY = GormProperties.IDENTITY;
    String VERSION = GormProperties.VERSION;
    String ATTACHED = GormProperties.ATTACHED;
    String TRANSIENT = GormProperties.TRANSIENT;
    String CONSTRAINTS = GormProperties.CONSTRAINTS;
    String META_CLASS = GormProperties.META_CLASS;
    String CLASS = GormProperties.CLASS;
    String MAPPING_STRATEGY = GormProperties.MAPPING_STRATEGY;
    String MAPPED_BY = GormProperties.MAPPED_BY;
    String BELONGS_TO = GormProperties.BELONGS_TO;
    String HAS_MANY = GormProperties.HAS_MANY;
    String HAS_ONE = GormProperties.HAS_ONE;
    String FETCH_MODE = "fetchMode";
    String DATE_CREATED = GormProperties.DATE_CREATED;
    String MAPPING = GormProperties.MAPPING;
    String NAMED_QUERIES = GormProperties.NAMED_QUERIES;
    String LAST_UPDATED = GormProperties.LAST_UPDATED;
    String DOMAIN_CLASS = "domainClass";
    String SORT = "sort";
    String EMBEDDED = GormProperties.EMBEDDED;
    String ERRORS = GormProperties.ERRORS;
    String DIRTY = GormProperties.DIRTY;
    String DIRTY_PROPERTY_NAMES = GormProperties.DIRTY_PROPERTY_NAMES;
    String DEFAULT_DATA_SOURCE = ConnectionSource.DEFAULT;
    String ALL_DATA_SOURCES = ConnectionSource.ALL;
    int FETCH_EAGER = 1;
    int FETCH_LAZY = 0;

    /**
     * Returns the configured fetch mode for the property
     */
    int getFetchMode();

    /**
     * Returns the name of the property.
     * @return The property name
     */
    String getName();

    /**
     * Returns the type for the domain class
     * @return  The property type
     */
    @SuppressWarnings("rawtypes")
    Class getType();

    /**
     * Returns the referenced property type. This differs from getType() in that in
     * the case of an Association it will return the type of the elements contained within the Collection,
     * otherwise it will delegate to getType();
     *
     * @return The referenced type
     */
    @SuppressWarnings("rawtypes")
    Class getReferencedPropertyType();

    /**
     * Returns the other side of a bidirectional association
     *
     * @return The other side of the relationship or null if not known
     */
    GrailsDomainClassProperty getOtherSide();

    /**
     * Returns the class type as a property name representation.
     *
     * @return The property name representation
     */
    String getTypePropertyName();

    /**
     * Returns the parent domain class of the property instance.
     * @return The parent domain class
     */
    GrailsDomainClass getDomainClass();

    /**
     * Returns true if the domain class property is a persistent property.
     * @return Whether the property is persistent
     */
    boolean isPersistent();

    /**
     * Returns false if the property is required.
     * @return Whether the property is optional
     */
    boolean isOptional();

    /**
     * Returns true of the property is an identifier.
     * @return Whether the property is the identifier
     */
    boolean isIdentity();

    /**
     * Returns true if the property is a one-to-many relationship.
     * @return Whether it is a oneToMany
     */
    boolean isOneToMany();

    /**
     * Returns true if the property is a many-to-one relationship.
     * @return Whether it is a manyToOne
     */
    boolean isManyToOne();

    /**
     * Returns true if the property is a many-to-many relationship.
     * @return true if it is a manyToMany
     */
    boolean isManyToMany();

    /**
     * Returns true if the property is a bi-directional relationship.
     * @return A boolean value
     */
    boolean isBidirectional();

    /**
     * Returns the domain field name for this property.
     */
    String getFieldName();

    /**
     * Returns true if the property is a one-to-one relationship.
     * @return true if it is a one-to-one relationship
     */
    boolean isOneToOne();

    /**
     * Returns the GrailsDomainClass of a relationship property or null,
     * if the property is not a relationship property.
     *
     * @return The GrailsDomainClass
     */
    GrailsDomainClass getReferencedDomainClass();

    /**
     * Returns true if this property is a relationship property.
     * @return true if it is an associative property
     */
    boolean isAssociation();

    /**
     * Returns true if this properties type is an enum.
     * @return true if it is
     */
    boolean isEnum();

    /**
     * @return The natural name representation of the property (eg. 'lastName' becomes 'Last Name'
     */
    String getNaturalName();

    /**
     * Sets the references domain class on the property.
     * @param referencedGrailsDomainClass
     */
    void setReferencedDomainClass(GrailsDomainClass referencedGrailsDomainClass);

    /**
     * Sets the other side of an associative property.
     * @param referencedProperty
     */
    void setOtherSide(GrailsDomainClassProperty referencedProperty);

    /**
     * Check whether this property is set up to receive save-update cascading via the Mapping DSL rather than
     * using 'belongsTo'.
     * @return True if this property is explicitly defined to receive save and update cascades, false otherwise.
     */
    boolean isExplicitSaveUpdateCascade();

    /**
     * Sets whether the domain class property is explicitly set up to receive
     * save and update cascades via a means other than 'belongsTo'.  Specifically,
     * the cascades will have been set up via the Mapping DSL.
     *
     * @param explicitSaveUpdateCascade Whether this property is explicity defined, via
     * the mapping DSL, to receive save and update cascades.
     */
    void setExplicitSaveUpdateCascade(boolean explicitSaveUpdateCascade);

    /**
     * Whether the property is inherited from a super class.
     * @return true if its inherited
     */
    boolean isInherited();

    /**
     * Whether this side of the association is the "owning" side.
     *
     * @return true if it is the owning side
     */
    boolean isOwningSide();

    /**
     * Whether the relationship is cirucular.
     *
     * @return true if it is
     */
    boolean isCircular();

    /**
     * Retrieves the name of property referenced by this property if it is
     * an association and is known, otherwise null.
     *
     * @return The name of the prop
     */
    String getReferencedPropertyName();

    /**
     * Returns true if this propert is an embedded component.
     *
     * @return true if it is, false otherwise
     */
    boolean isEmbedded();

    /**
     * If #isEmbedded returns true then this method can be called to obtain a reference to the
     * embedded component, which implements the GrailsDomainClass interface.
     *
     * @see GrailsDomainClass
     * @see GrailsDomainClassProperty#isEmbedded()
     *
     * @return The component or null if it is not an embedded component
     */
    GrailsDomainClass getComponent();

    void setOwningSide(boolean b);

    /**
     * Return whether this is a collection of basic types like String, Integer etc.
     * @return true if it is a collection of basic types
     */
    boolean isBasicCollectionType();

    boolean isHasOne();

    void setDerived(boolean derived);

    boolean isDerived();
}
