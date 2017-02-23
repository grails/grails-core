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
import grails.util.GrailsClassUtils;
import grails.util.GrailsNameUtils;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.*;
import org.springframework.util.ClassUtils;

import javax.persistence.FetchType;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a property of a domain class and contains meta information about the
 * properties relationships, naming conventions and type.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class DefaultGrailsDomainClassProperty implements GrailsDomainClassProperty {

    private GrailsDomainClass domainClass;

    private String naturalName;
    private boolean explicitSaveUpdateCascade = false;

    private final PersistentEntity persistentEntity;
    private final PersistentProperty persistentProperty;
    private final Association association;
    private final GrailsDomainClass referencedDomainClass;
    /**
     * Constructor.
     * @param domainClass The domain class
     * @param property The persistent property instance
     */
    @SuppressWarnings("rawtypes")
    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PersistentEntity entity, PersistentProperty property) {
        this.domainClass = domainClass;
        this.persistentEntity = entity;
        this.persistentProperty = property;
        this.association = property instanceof Association ? (Association) property : null;
        if(association != null) {
            PersistentEntity associatedEntity = association.getAssociatedEntity();
            if(associatedEntity != null) {
                this.referencedDomainClass = (GrailsDomainClass) domainClass
                        .getApplication()
                        .getArtefact(
                        DomainClassArtefactHandler.TYPE, associatedEntity.getName()
                );
            }
            else {
                this.referencedDomainClass = null;
            }
        }
        else {
            this.referencedDomainClass = null;
        }
        naturalName = GrailsNameUtils.getNaturalName(property.getName());
    }

    private void throwUnsupported() {
        throw new UnsupportedOperationException("The DomainClass property API does not support that operation. Use the mapping context API instead.");
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getName()
     */
    public String getName() {
        return persistentProperty.getName();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getType()
     */
    @SuppressWarnings("rawtypes")
    public Class getType() {
        return persistentProperty.getType();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isPersistant()
     */
    public boolean isPersistent() {
        return persistentProperty != null;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isRequired()
     */
    public boolean isOptional() {
        return persistentProperty.isNullable();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isIdentity()
     */
    public boolean isIdentity() {
        return persistentProperty instanceof Identity;
    }

    @Deprecated
    public void setIdentity(boolean b) {
        throwUnsupported();
    }

    @Override
    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToMany()
     */
    public boolean isOneToMany() {
        return persistentProperty instanceof OneToMany;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isManyToOne()
     */
    @Override
    public boolean isManyToOne() {
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
        return persistentProperty instanceof ManyToMany;
    }

    /**
     * @param manyToMany The manyToMany to set.
     */
    @Deprecated
    protected void setManyToMany(boolean manyToMany) {
        throwUnsupported();
    }

    /**
     * @param oneToMany The oneToMany to set.
     */
    @Deprecated
    protected void setOneToMany(boolean oneToMany) {
        throwUnsupported();
    }

    /**
     * @param manyToOne The manyToOne to set.
     */
    @Deprecated
    protected void setManyToOne(boolean manyToOne) {
        throwUnsupported();
    }

    /**
     * @param oneToOne The oneToOne to set.
     */
    @Deprecated
    protected void setOneToOne(boolean oneToOne) {
        throwUnsupported();
    }

    /**
     * Set whether the foreign key is stored in the parent or child in a one-to-one
     * @param isHasOne True if its stored in the parent
     */
    @Deprecated
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
    @Deprecated
    protected void setPersistent(boolean persistent) {
        throwUnsupported();
    }

    /**
     * Sets whether the relationship is bidirectional or not
     */
    @Deprecated
    protected void setBidirectional(boolean bidirectional) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getTypePropertyName()
     */
    @Override
    public String getTypePropertyName() {
        String shortTypeName = ClassUtils.getShortName(persistentProperty.getType());
        return shortTypeName.substring(0,1).toLowerCase(Locale.ENGLISH) + shortTypeName.substring(1);
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getReferencedPropertyType()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class getReferencedPropertyType() {
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
        return association != null && association.isBidirectional();
    }

    /**
     * Sets the referenced property type of this property
     */
    @Deprecated
    protected void setReferencedPropertyType(Class<?> referencedPropertyType) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public GrailsDomainClass getReferencedDomainClass() {
        return referencedDomainClass;
    }

    public void setReferencedDomainClass(GrailsDomainClass referencedDomainClass) {
        throwUnsupported();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#isAssociation()
     */
    public boolean isAssociation() {
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
        return persistentProperty.toString();
    }

    /* (non-Javadoc)
     * @see grails.core.GrailsDomainClassProperty#getOtherSide()
     */
    public GrailsDomainClassProperty getOtherSide() {
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
        return persistentProperty.isInherited();
    }

    public int getFetchMode() {
        FetchType fetchStrategy = persistentProperty.getMapping().getMappedForm().getFetchStrategy();
        switch (fetchStrategy) {
            case EAGER:
                return FETCH_EAGER;
            case LAZY:
                return FETCH_LAZY;
        }
        return FETCH_LAZY;
    }

    public boolean isOwningSide() {
        return association != null && association.isOwningSide();
    }

    public void setOwningSide(boolean b) {
        throwUnsupported();
    }

    @SuppressWarnings("unchecked")
    public boolean isCircular() {
        return association != null && association.isCircular();
    }

    public void setReferencePropertyName(String name) {
        throwUnsupported();
    }

    public String getReferencedPropertyName() {
        if (association != null) {
            return association.getReferencedPropertyName();
        } else {
            return null;
        }
    }

    public boolean isEmbedded() {
        return association != null && association.isEmbedded();
    }

    public GrailsDomainClass getComponent() {
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
        return persistentProperty != null && persistentProperty instanceof Basic;
    }

}
