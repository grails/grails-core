package org.codehaus.groovy.grails.validation;

import org.springframework.validation.Errors;

/**
 * Marker interface for vetoing constraint.
 * <p/>
 * Vetoing constraints are those which might return 'true' from validateWithVetoing method to prevent any additional
 * validation of the property. These constraints are proceeded before any other constraints, and validation continues
 * only if no one of vetoing constraint hadn't vetoed.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public interface VetoingConstraint extends Constraint {
    boolean validateWithVetoing(Object target, Object propertyValue, Errors errors);
}
