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
 * A Constraint that implements a maximum value constraint
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:19:14 AM
 */
class MaxConstraint extends AbstractConstraint {


    private Comparable maxValue;

    /**
     * @return Returns the maxValue.
     */
    public Comparable getMaxValue() {
        return maxValue;
    }


    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    public boolean supports(Class type) {
        return type != null && (Comparable.class.isAssignableFrom(type) ||
                Number.class.isAssignableFrom(type));

    }


    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Comparable))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MAX_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must implement the interface [java.lang.Comparable]");

        this.maxValue = (Comparable)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.MAX_CONSTRAINT;
    }


    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(propertyValue == null) {
            return; // A null is not a value we should even check
        }

        if(maxValue.compareTo(propertyValue) < 0) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, maxValue  };
            super.rejectValue(errors,ConstrainedProperty.MAX_CONSTRAINT + ConstrainedProperty.EXCEEDED_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_MAX_MESSAGE_CODE, args));
        }
    }
}
