package org.grails.validation

import grails.validation.Constraint
import groovy.transform.CompileStatic
import org.springframework.context.MessageSource

/**
 * Bridges old validation API and new API
 *
 * @author Graeme Rocher
 * @since 3.3
 */
@CompileStatic
class ConstraintDelegate implements Constraint {
    @Delegate final grails.gorm.validation.Constraint constraint

    ConstraintDelegate(grails.gorm.validation.Constraint constraint) {
        this.constraint = constraint
    }

    @Override
    void setParameter(Object parameter) {
        throw new UnsupportedOperationException("Constraints are no longer mutable")
    }

    @Override
    void setOwningClass(Class owningClass) {
        throw new UnsupportedOperationException("Constraints are no longer mutable")
    }

    @Override
    void setPropertyName(String propertyName) {
        throw new UnsupportedOperationException("Constraints are no longer mutable")
    }

    @Override
    void setMessageSource(MessageSource source) {
        throw new UnsupportedOperationException("Constraints are no longer mutable")
    }
}
