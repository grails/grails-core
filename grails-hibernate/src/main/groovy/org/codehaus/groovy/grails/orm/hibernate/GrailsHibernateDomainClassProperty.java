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
package org.codehaus.groovy.grails.orm.hibernate;

import grails.util.GrailsNameUtils;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * An implementation of the GrailsDomainClassProperty interface that allows Classes mapped in
 * Hibernate to integrate with Grails' validation, dynamic methods etc. seamlessly.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsHibernateDomainClassProperty implements GrailsDomainClassProperty {
    private GrailsHibernateDomainClass domainClass;

    private String name;
    private String naturalName;
    private Class<?> type;
    private boolean identity;
    private boolean oneToOne;
    private boolean manyToOne;
    private boolean association;
    private boolean oneToMany;
    private boolean manyToMany;
    private boolean bidirectional;
    private boolean optional;
    private Class<?> relatedClassType;
    private GrailsDomainClass referencedDomainClass;
    private GrailsDomainClassProperty otherSide;
    private boolean owingSide;
    private String columnName;
    private boolean explicitSaveUpdateCascade;

    public GrailsHibernateDomainClassProperty(GrailsHibernateDomainClass domainClass, String propertyName) {
        this.domainClass = domainClass;
        name = propertyName;
        naturalName = GrailsNameUtils.getNaturalName(propertyName);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        if (type == null) {
            attemptResolveType();
        }
        return type;
    }

    private void attemptResolveType() {
        PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(domainClass.getClazz(), name);
        type = propertyDescriptor == null ? null : propertyDescriptor.getPropertyType();
        if (type == null) {
            Field field = ReflectionUtils.findField(domainClass.getClazz(), name);
            if (field  != null) {
                type = field.getType();
            }
        }
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getTypePropertyName() {
        return GrailsNameUtils.getPropertyNameRepresentation(type);
    }

    public GrailsDomainClass getDomainClass() {
        return domainClass;
    }

    public boolean isPersistent() {
        return true;
    }

    public String getNaturalName() {
        return naturalName;
    }

    public void setReferencedDomainClass(GrailsDomainClass referencedGrailsDomainClass) {
        this.referencedDomainClass = referencedGrailsDomainClass;
    }

    public void setOtherSide(GrailsDomainClassProperty referencedProperty) {
        otherSide = referencedProperty;
    }

    public boolean isExplicitSaveUpdateCascade() {
        return explicitSaveUpdateCascade;
    }

    public void setExplicitSaveUpdateCascade(boolean explicitSaveUpdateCascade) {
        this.explicitSaveUpdateCascade = explicitSaveUpdateCascade;
    }

    public GrailsDomainClassProperty getOtherSide() {
        return otherSide;
    }

    public Class<?> getReferencedPropertyType() {
        return relatedClassType;
    }

    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean identity) {
        this.identity = identity;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOneToOne() {
        return oneToOne;
    }

    public void setOneToOne(boolean oneToOne) {
        this.oneToOne = oneToOne;
    }

    public boolean isManyToOne() {
        return manyToOne;
    }

    public void setManyToOne(boolean manyToOne) {
        this.manyToOne = manyToOne;
    }

    public boolean isAssociation() {
        return association;
    }

    public boolean isEnum() {
        return GrailsClassUtils.isJdk5Enum(getType());
    }

    public void setAssociation(boolean association) {
        this.association = association;
    }

    public boolean isOneToMany() {
        return oneToMany;
    }

    public void setOneToMany(boolean oneToMany) {
        this.oneToMany = oneToMany;
    }

    public boolean isManyToMany() {
        return manyToMany;
    }

    public void setManyToMany(boolean manyToMany) {
        this.manyToMany = manyToMany;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public String getFieldName() {
        return getName().toUpperCase();
    }

    public void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    public GrailsDomainClass getReferencedDomainClass() {
        return referencedDomainClass;
    }

    public void setRelatedClassType(Class<?> relatedType) {
        relatedClassType = relatedType;
    }

    public boolean isInherited() {
        return false;
    }

    public int getFetchMode() {
        return FETCH_LAZY;
    }

    public boolean isOwningSide() {
        return owingSide;
    }

    public boolean isCircular() {
        return false;
    }

    public String getReferencedPropertyName() {
        return null;
    }

    public boolean isEmbedded() {
        return false;
    }

    public GrailsDomainClass getComponent() {
        return null;
    }

    public void setOwningSide(boolean b) {
        owingSide = b;
    }

    public boolean isBasicCollectionType() {
        return false;
    }

    public boolean isHasOne() {
        return false;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setDerived(boolean derived) {
        // ignored
    }

    public boolean isDerived() {
        return false;
    }
}
