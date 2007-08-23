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
import org.apache.commons.validator.CreditCardValidator;

/**
 * A constraint class that validates a credit card number
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:12:04 AM
 */
class CreditCardConstraint extends AbstractConstraint {
    private boolean creditCard;


    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(creditCard) {
            CreditCardValidator validator = new CreditCardValidator();

            if(!validator.isValid(propertyValue.toString())  ) {
                Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue };
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_CREDIT_CARD_MESSAGE_CODE,ConstrainedProperty.CREDIT_CARD_CONSTRAINT + ConstrainedProperty.INVALID_SUFFIX,args);
            }
        }
    }

    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Boolean))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.CREDIT_CARD_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean value");

        this.creditCard = ((Boolean)constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.CREDIT_CARD_CONSTRAINT;
    }

    public boolean supports(Class type) {
        return type != null && String.class.isAssignableFrom(type);

    }
}
