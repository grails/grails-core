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

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.core.LazyMappingContext;
import grails.util.GrailsClassUtils;
import grails.util.GrailsNameUtils;

import java.beans.PropertyDescriptor;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.*;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;

/**
 * Represents a property of a domain class and contains meta information about the
 * properties relationships, naming conventions and type.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class DefaultGrailsDomainClassProperty implements GrailsDomainClassProperty {

    private static final Log log = LogFactory.getLog(DefaultGrailsDomainClassProperty.class);

    private GrailsDomainClass domainClass;
    private boolean persistent = true; // persistant by default
    private boolean identity;
    private String name;
    private Class<?> type;

    private String naturalName;
    private boolean inherited;
    private boolean basicCollectionType;
    private Map<String, Object> defaultConstraints;
    private boolean explicitSaveUpdateCascade = false;

    private LazyMappingContext mappingContext;
    private PersistentEntity persistentEntity;
    private PersistentProperty persistentProperty;
    private Association association;
    private GrailsDomainClass referencedDomainClass;

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
    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PropertyDescriptor descriptor, LazyMappingContext mappingContext) {
        this(domainClass, descriptor, null, mappingContext);
    }

    /**
     * Constructor.
     * @param domainClass
     * @param descriptor
     */
    @SuppressWarnings("rawtypes")
    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PropertyDescriptor descriptor, Map<String, Object> defaultConstraints, LazyMappingContext mappingContext) {
        this.domainClass = domainClass;
        name = descriptor.getName();
        naturalName = GrailsNameUtils.getNaturalName(descriptor.getName());
        type = descriptor.getPropertyType();
        identity = descriptor.getName().equals(IDENTITY);

        // establish if property is persistant
        if (domainClass != null) {
            // figure out if this property is inherited
            inherited = GrailsClassUtils.isPropertyInherited(domainClass.getClazz(), name);
        }
        this.defaultConstraints = defaultConstraints;
        this.mappingContext = mappingContext;
    }

    private void verifyContextIsInitialized() {
        if (mappingContext == null || !mappingContext.isInitialized()) {
            throw new RuntimeException("That API cannot be accessed before the spring context is initialized");
        } else {
            log.warn("The GrailsDomainClassProperty API should no longer be used to retrieve data about domain class properties. Use the mapping context API instead");
            if (persistentEntity == null) {
                persistentEntity = mappingContext.getPersistentEntity(this.domainClass.getFullName());
                if (persistentEntity != null) {
                    persistentProperty = persistentEntity.getPropertyByName(this.getName());
                    if (persistentProperty instanceof Association) {
                        this.association = (Association)persistentProperty;
                        if (!association.isBasic()) {
                            this.referencedDomainClass = mappingContext.getDomainClass(association.getAssociatedEntity().getName());
                        }
                    }
                }
            }
            if (persistentEntity == null) {
                StringBuilder sb = new StringBuilder("Could not retrieve the respective entity for ");
                sb.append(this.domainClass.getName());
                sb.append(".");
                sb.append(this.getName());
                sb.append(" in the mapping context API");
                throw new RuntimeException(sb.toString());
            }
        }
    }

    private void throwUnsupported() {
        throw new UnsupportedOperationException("The DomainClassProperty API does not support that operation. Use the mapping context API instead.");
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
        verifyContextIsInitialized();
        return persistentProperty != null;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isRequired()
     */
    public boolean isOptional() {
        verifyContextIsInitialized();
        return persistentProperty.isNullable();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isIdentity()
     */
    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean b) {
        throwUnsupported();
    }

    @Override
    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToMany()
     */
    public boolean isOneToMany() {
        verifyContextIsInitialized();
        return persistentProperty instanceof OneToMany;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isManyToOne()
     */
    @Override
    public boolean isManyToOne() {
        verifyContextIsInitialized();
        return persistentProperty instanceof ManyToOne;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getFieldName()
     */
    @Override
    public String getFieldName() {
        return getName().toUpperCase();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToOne()
     */
    @Override
    public boolean isOneToOne() {
        verifyContextIsInitialized();
        return persistentProperty instanceof OneToOne;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getDomainClass()
     */
    @Override
    public GrailsDomainClass getDomainClass() {
        return domainClass;
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isManyToMany()
     */
    @Override
    public boolean isManyToMany() {
        verifyContextIsInitialized();
        return persistentProperty instanceof ManyToMany;
    }

    /**
     * @param manyToMany The manyToMany to set.
     */
    protected void setManyToMany(boolean manyToMany) {
        throwUnsupported();
    }

    /**
     * @param oneToMany The oneToMany to set.
     */
    protected void setOneToMany(boolean oneToMany) {
        throwUnsupported();
    }

    /**
     * @param manyToOne The manyToOne to set.
     */
    protected void setManyToOne(boolean manyToOne) {
        throwUnsupported();
    }

    /**
     * @param oneToOne The oneToOne to set.
     */
    protected void setOneToOne(boolean oneToOne) {
        throwUnsupported();
    }

    /**
     * Set whether the foreign key is stored in the parent or child in a one-to-one
     * @param isHasOne True if its stored in the parent
     */
    protected void setHasOne(boolean isHasOne) {
        throwUnsupported();
    }

    /**
     * @return true if the foreign key in a one-to-one is stored in the parent
     */
    @Override
    public boolean isHasOne() {
        return association instanceof ToOne && ((ToOne)association).isForeignKeyInChild();
    }

    /**
     * @param persistent The persistant to set.
     */
    protected void setPersistent(boolean persistent) {
        throwUnsupported();
    }

    /**
     * Sets whether the relationship is bidirectional or not
     */
    protected void setBidirectional(boolean bidirectional) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getTypePropertyName()
     */
    @Override
    public String getTypePropertyName() {
        String shortTypeName = ClassUtils.getShortName(type);
        return shortTypeName.substring(0,1).toLowerCase(Locale.ENGLISH) + shortTypeName.substring(1);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getReferencedPropertyType()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class getReferencedPropertyType() {
        verifyContextIsInitialized();
        if (association != null) {
            PersistentEntity entity = association.getAssociatedEntity();
            if (entity != null) {
                return entity.getJavaClass();
            } else if (association.isBasic()) {
                return ((Basic)association).getComponentType();
            }
        } else if (persistentProperty instanceof Simple) {
            return persistentProperty.getType();
        }
        return null;
    }


    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isBidirectional()
     */
    @Override
    public boolean isBidirectional() {
        verifyContextIsInitialized();
        return association != null && association.isBidirectional();
    }

    /**
     * Sets the referenced property type of this property
     */
    protected void setReferencedPropertyType(Class<?> referencedPropertyType) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public GrailsDomainClass getReferencedDomainClass() {
        verifyContextIsInitialized();
        return referencedDomainClass;
    }

    public void setReferencedDomainClass(GrailsDomainClass referencedDomainClass) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public boolean isAssociation() {
        verifyContextIsInitialized();
        return association != null;
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
        verifyContextIsInitialized();
        return persistentProperty.toString();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getOtherSide()
     */
    public GrailsDomainClassProperty getOtherSide() {
        verifyContextIsInitialized();
        if (referencedDomainClass != null && association != null) {
            Association otherSide = association.getInverseSide();
            if (otherSide != null) {
                return referencedDomainClass.getPropertyByName(otherSide.getName());
            }
        }
        return null;
    }

    public void setOtherSide(GrailsDomainClassProperty property) {
        throwUnsupported();
    }

    @Override
    public boolean isExplicitSaveUpdateCascade() {
        throwUnsupported();
        return false;
    }

    @Override
    public void setExplicitSaveUpdateCascade(boolean explicitSaveUpdateCascade) {
        throwUnsupported();
    }

    public boolean isInherited() {
        return inherited;
    }

    public int getFetchMode() {
        verifyContextIsInitialized();
        return persistentProperty.getMapping().getMappedForm().getFetchStrategy().ordinal();
    }

    public boolean isOwningSide() {
        verifyContextIsInitialized();
        return association != null && association.isOwningSide();
    }

    public void setOwningSide(boolean b) {
        throwUnsupported();
    }

    @SuppressWarnings("unchecked")
    public boolean isCircular() {
        verifyContextIsInitialized();
        return association != null && association.isCircular();
    }

    public void setReferencePropertyName(String name) {
        throwUnsupported();
    }

    public String getReferencedPropertyName() {
        verifyContextIsInitialized();
        if (association != null) {
            return association.getReferencedPropertyName();
        } else {
            return null;
        }
    }

    public boolean isEmbedded() {
        verifyContextIsInitialized();
        return association != null && association.isEmbedded();
    }

    public GrailsDomainClass getComponent() {
        verifyContextIsInitialized();
        if (association instanceof Embedded) {
            return getReferencedDomainClass();
        } else {
            return null;
        }
    }

    public void setEmbedded(boolean isEmbedded) {
        throwUnsupported();
    }

    public boolean isDerived() {
        verifyContextIsInitialized();
        return persistentProperty.getMapping().getMappedForm().isDerived();
    }

    public void setDerived(boolean derived) {
        throwUnsupported();
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
        throwUnsupported();
    }

    public boolean isBasicCollectionType() {
        verifyContextIsInitialized();
        return persistentProperty != null && persistentProperty instanceof Basic;
    }

}
