/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.validation;

import org.springframework.validation.Errors;

/**
 * Validates not null.
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @since 0.4
 */
public class NullableConstraint extends AbstractVetoingConstraint {

    private boolean nullable;

    public boolean isNullable() {
        return nullable;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    public boolean supports(Class type) {
        return type != null && !type.isPrimitive();
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    @Override
    public void setParameter(Object constraintParameter) {
        if (!(constraintParameter instanceof Boolean)) {
            throw new IllegalArgumentException("Parameter for constraint [" + ConstrainedProperty.NULLABLE_CONSTRAINT +
                    "] of property [" + constraintPropertyName + "] of class [" +
                    constraintOwningClass + "] must be a boolean value");
        }

        nullable = ((Boolean)constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.NULLABLE_CONSTRAINT;
    }

    @Override
    protected boolean skipNullValues() {
        return false;
    }

    @Override
    protected boolean processValidateWithVetoing(Object target, Object propertyValue, Errors errors) {
        if (propertyValue == null) {
            if (!nullable) {
                Object[] args = new Object[] { constraintPropertyName, constraintOwningClass };
                rejectValue(target, errors, ConstrainedProperty.DEFAULT_NULL_MESSAGE_CODE,
                        ConstrainedProperty.NULLABLE_CONSTRAINT, args);
                // null value is caught by 'blank' constraint, no addition validation needed
                return true;
            }
        }
        return false;
    }
}
