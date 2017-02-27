package org.grails.core

import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic

/**
 * Simplified version that encapsulates a meta property
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class MetaGrailsDomainClassProperty implements GrailsDomainClassProperty  {

    final GrailsDomainClass domainClass
    final MetaProperty property

    MetaGrailsDomainClassProperty(GrailsDomainClass domainClass, MetaProperty property) {
        this.domainClass = domainClass
        this.property = property
    }

    @Override
    int getFetchMode() {
        return 0
    }

    @Override
    String getName() {
        return property.name
    }

    @Override
    Class getType() {
        return property.type
    }

    @Override
    Class getReferencedPropertyType() {
        return null
    }

    @Override
    GrailsDomainClassProperty getOtherSide() {
        return null
    }

    @Override
    String getTypePropertyName() {
        return GrailsNameUtils.getPropertyName(property.type)
    }

    @Override
    GrailsDomainClass getDomainClass() {
        return domainClass
    }

    @Override
    boolean isPersistent() {
        return false
    }

    @Override
    boolean isOptional() {
        return true
    }

    @Override
    boolean isIdentity() {
        return false
    }

    @Override
    boolean isOneToMany() {
        return false
    }

    @Override
    boolean isManyToOne() {
        return false
    }

    @Override
    boolean isManyToMany() {
        return false
    }

    @Override
    boolean isBidirectional() {
        return false
    }

    @Override
    String getFieldName() {
        return property.name
    }

    @Override
    boolean isOneToOne() {
        return false
    }

    @Override
    GrailsDomainClass getReferencedDomainClass() {
        return null
    }

    @Override
    boolean isAssociation() {
        return false
    }

    @Override
    boolean isEnum() {
        return property.type.isEnum()
    }

    @Override
    String getNaturalName() {
        return GrailsNameUtils.getNaturalName(property.name)
    }

    @Override
    void setReferencedDomainClass(GrailsDomainClass referencedGrailsDomainClass) {

    }

    @Override
    void setOtherSide(GrailsDomainClassProperty referencedProperty) {

    }

    @Override
    boolean isExplicitSaveUpdateCascade() {
        return false
    }

    @Override
    void setExplicitSaveUpdateCascade(boolean explicitSaveUpdateCascade) {

    }

    @Override
    boolean isInherited() {
        return false
    }

    @Override
    boolean isOwningSide() {
        return false
    }

    @Override
    boolean isCircular() {
        return false
    }

    @Override
    String getReferencedPropertyName() {
        return null
    }

    @Override
    boolean isEmbedded() {
        return false
    }

    @Override
    GrailsDomainClass getComponent() {
        return null
    }

    @Override
    void setOwningSide(boolean b) {

    }

    @Override
    boolean isBasicCollectionType() {
        return false
    }

    @Override
    boolean isHasOne() {
        return false
    }

    @Override
    void setDerived(boolean derived) {

    }

    @Override
    boolean isDerived() {
        return false
    }
}
