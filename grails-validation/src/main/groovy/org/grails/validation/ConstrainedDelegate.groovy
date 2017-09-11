package org.grails.validation

import grails.gorm.validation.ConstrainedProperty
import grails.gorm.validation.Constraint
import grails.util.GrailsUtil
import grails.validation.Constrained
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.springframework.validation.Errors

/**
 * Bridge from the old API to the new
 *
 * @author Graeme Rocher
 * @since 6.1
 *
 */
@CompileStatic
@Slf4j
class ConstrainedDelegate implements Constrained, ConstrainedProperty {
    final ConstrainedProperty property

    private grails.validation.ConstrainedProperty copy

    ConstrainedDelegate(ConstrainedProperty property) {
        this.property = property
    }

    /**
     * @return Returns the appliedConstraints.
     */
    Collection<Constraint> getAppliedConstraints() {
        return (Collection<Constraint>)property.appliedConstraints.collect() { new ConstraintDelegate(it) }
    }

    @Override
    String getPropertyName() {
        return property.getPropertyName()
    }

    @Override
    grails.gorm.validation.Constraint getAppliedConstraint(String name) {
        return property.getAppliedConstraint(name)
    }

    @Override
    void validate(Object target, Object propertyValue, Errors errors) {
        property.validate(target, propertyValue, errors)
    }

    @Override
    String getWidget() {
        return property.getWidget()
    }

    @Override
    boolean hasAppliedConstraint(String constraintName) {
        return property.hasAppliedConstraint(constraintName)
    }

    @Override
    Class<?> getPropertyType() {
        return property.getPropertyType()
    }

    @Override
    Comparable getMax() {
        return property.getMax()
    }

    @Override
    Comparable getMin() {
        return property.getMin()
    }

    @Override
    List getInList() {
        return property.getInList()
    }

    @Override
    Range getRange() {
        return property.getRange()
    }

    @Override
    Integer getScale() {
        return property.getScale()
    }

    @Override
    Range getSize() {
        return property.getSize()
    }

    @Override
    boolean isBlank() {
        return property.isBlank()
    }

    @Override
    boolean isEmail() {
        return property.isEmail()
    }

    @Override
    boolean isCreditCard() {
        return property.isCreditCard()
    }

    @Override
    String getMatches() {
        return property.getMatches()
    }

    @Override
    Object getNotEqual() {
        return property.getNotEqual()
    }

    @Override
    Integer getMaxSize() {
        return property.getMaxSize()
    }

    @Override
    Integer getMinSize() {
        return property.getMinSize()
    }

    @Override
    boolean isNullable() {
        return property.isNullable()
    }

    @Override
    boolean isUrl() {
        return property.isUrl()
    }

    @Override
    boolean isDisplay() {
        return property.isDisplay()
    }

    @Override
    boolean isEditable() {
        return property.isEditable()
    }

    @Override
    int getOrder() {
        return property.getOrder()
    }

    @Override
    String getFormat() {
        return property.getFormat()
    }

    @Override
    boolean isPassword() {
        return property.isPassword()
    }

    @Override
    boolean supportsContraint(String constraintName) {
        return property.supportsContraint(constraintName)
    }

    @Override
    void applyConstraint(String constraintName, Object constrainingValue) {
        property.applyConstraint(constraintName, constrainingValue)
    }

    @Override
    Class getOwner() {
        return property.getOwner()
    }

    Object asType(Class type) {
        if(type == grails.validation.ConstrainedProperty) {
            GrailsUtil.deprecated("A class used the deprecated [grails.validation.ConstrainedProperty] type. Please update to use [$ConstrainedProperty.name] instead")
            if(copy == null) {
                copy = new grails.validation.ConstrainedProperty(owner, propertyName, propertyType)
                for(constraint in appliedConstraints) {
                    copy.applyConstraint(constraint.name, constraint.parameter)
                }
                copy.widget = widget
                copy.display = display
                copy.editable = editable
                copy.order = order
                copy.format = format
                copy.password = password
            }

            return copy
        }
        throw new GroovyCastException(this, type)
    }
}
