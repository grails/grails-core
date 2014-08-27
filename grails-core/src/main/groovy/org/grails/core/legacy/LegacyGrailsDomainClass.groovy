package org.grails.core.legacy

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.beans.BeanWrapper
import org.springframework.validation.Validator

/**
 * Legacy bridge to older Grails 2.3.x API for backwards compatibility
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class LegacyGrailsDomainClass  implements GrailsDomainClass  {

    grails.core.GrailsDomainClass newDomainClass

    LegacyGrailsDomainClass(grails.core.GrailsDomainClass domainClass) {
        this.newDomainClass = domainClass
    }

    @Override
    boolean isOwningClass(Class domainClass) {
        return newDomainClass.isOwningClass(domainClass)
    }

    @Override
    GrailsDomainClassProperty[] getProperties() {
        return newDomainClass.getProperties() as GrailsDomainClassProperty[]
    }

    @Override
    GrailsDomainClassProperty[] getPersistantProperties() {
        return newDomainClass.persistentProperties as GrailsDomainClassProperty[]
    }

    @Override
    GrailsDomainClassProperty[] getPersistentProperties() {
        return newDomainClass.persistentProperties as GrailsDomainClassProperty[]
    }

    @Override
    GrailsDomainClassProperty getIdentifier() {
        return (GrailsDomainClassProperty)newDomainClass.identifier
    }

    @Override
    GrailsDomainClassProperty getVersion() {
        return (GrailsDomainClassProperty)newDomainClass.version
    }

    @Override
    Map getAssociationMap() {
        return newDomainClass.associationMap
    }

    @Override
    GrailsDomainClassProperty getPropertyByName(String name) {
        return null
    }

    @Override
    GrailsDomainClassProperty getPersistentProperty(String name) {
        return null
    }

    @Override
    String getFieldName(String propertyName) {
        return newDomainClass.getFieldName(propertyName)
    }

    @Override
    boolean isAbstract() {
        return newDomainClass.isAbstract()
    }

    @Override
    org.codehaus.groovy.grails.commons.GrailsApplication getGrailsApplication() {
        return newDomainClass.getGrailsApplication()
    }

    @Override
    GrailsApplication getApplication() {
        return newDomainClass.application
    }

    @Override
    Object getPropertyValue(String name) {
        return newDomainClass.getPropertyValue(name)
    }

    @Override
    boolean hasProperty(String name) {
        return newDomainClass.hasProperty(name)
    }

    @Override
    Object newInstance() {
        return newDomainClass.newInstance()
    }

    @Override
    String getName() {
        return newDomainClass.name
    }

    @Override
    String getShortName() {
        return newDomainClass.shortName
    }

    @Override
    String getFullName() {
        return newDomainClass.fullName
    }

    @Override
    String getPropertyName() {
        return newDomainClass.propertyName
    }

    @Override
    String getLogicalPropertyName() {
        return newDomainClass.logicalPropertyName
    }

    @Override
    String getNaturalName() {
        return newDomainClass.naturalName
    }

    @Override
    String getPackageName() {
        return newDomainClass.packageName
    }

    @Override
    Class getClazz() {
        return newDomainClass.clazz
    }

    @Override
    BeanWrapper getReference() {
        return newDomainClass.getReference()
    }

    @Override
    Object getReferenceInstance() {
        return newDomainClass.getReferenceInstance()
    }

    @Override
    def <T> T getPropertyValue(String name, Class<T> type) {
        return newDomainClass.getPropertyValue(name, type)
    }

    @Override
    String getPluginName() {
        return newDomainClass.pluginName
    }

    @Override
    boolean isOneToMany(String propertyName) {
        return newDomainClass.isOneToMany(propertyName)
    }

    @Override
    boolean isManyToOne(String propertyName) {
        return newDomainClass.isManyToOne(propertyName)
    }

    @Override
    boolean isBidirectional(String propertyName) {
        return newDomainClass.isBidirectional(propertyName)
    }

    @Override
    Class getRelatedClassType(String propertyName) {
        return newDomainClass.getRelatedClassType(propertyName)
    }

    @Override
    Map getConstrainedProperties() {
        return newDomainClass.constrainedProperties
    }

    @Override
    Validator getValidator() {
        return newDomainClass.validator
    }

    @Override
    void setValidator(Validator validator) {
        newDomainClass.validator = validator
    }

    @Override
    String getMappingStrategy() {
        return newDomainClass.mappingStrategy
    }

    @Override
    boolean isRoot() {
        return newDomainClass.isRoot()
    }

    @Override
    Set<GrailsDomainClass> getSubClasses() {
        return null
    }

    @Override
    void refreshConstraints() {
        newDomainClass.refreshConstraints()
    }

    @Override
    boolean hasSubClasses() {
        return newDomainClass.hasSubClasses()
    }

    @Override
    Map getMappedBy() {
        return newDomainClass.mappedBy
    }

    @Override
    boolean hasPersistentProperty(String propertyName) {
        return newDomainClass.hasPersistentProperty(propertyName)
    }

    @Override
    void setMappingStrategy(String strategy) {
        newDomainClass.mappingStrategy = strategy
    }

    @Override
    void setGrailsApplication(GrailsApplication grailsApplication) {
        newDomainClass.grailsApplication = grailsApplication
    }
}
