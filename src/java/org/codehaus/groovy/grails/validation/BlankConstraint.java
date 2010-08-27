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

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;

/**
 * A Constraint that validates a string is not blank.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class BlankConstraint extends AbstractVetoingConstraint {

    private boolean blank;

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    public boolean supports(Class type) {
        return type != null && String.class.isAssignableFrom(type);
    }

    public Object getParameter() {
        return Boolean.valueOf(blank);
    }

    public boolean isBlank() {
        return blank;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    @Override
    public void setParameter(Object constraintParameter) {
        if (!(constraintParameter instanceof Boolean)) {
            throw new IllegalArgumentException("Parameter for constraint [" +
                    ConstrainedProperty.BLANK_CONSTRAINT + "] of property [" +
                    constraintPropertyName + "] of class [" + constraintOwningClass +
                    "] must be a boolean value");
        }

        blank = ((Boolean)constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.BLANK_CONSTRAINT;
    }

    @Override
    protected boolean skipBlankValues() {
        return false;
    }

    @Override
    protected boolean processValidateWithVetoing(Object target, Object propertyValue, Errors errors) {
        if (propertyValue instanceof String && StringUtils.isBlank((String)propertyValue)) {
            if (!blank) {
                Object[] args = new Object[] { constraintPropertyName, constraintOwningClass };
                rejectValue(target, errors, ConstrainedProperty.DEFAULT_BLANK_MESSAGE_CODE,
                        ConstrainedProperty.BLANK_CONSTRAINT, args);
                // empty string is caught by 'blank' constraint, no addition validation needed
                return true;
            }
        }
        return false;
    }
}
