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
import org.apache.commons.validator.EmailValidator;

/**
 * A Constraint that validates an email address
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:13:29 AM
 */
class EmailConstraint extends AbstractConstraint {

    private boolean email;



    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    public boolean supports(Class type) {
        return type != null && String.class.isAssignableFrom(type);

    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Boolean))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.EMAIL_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean value");

        this.email = ((Boolean)constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.EMAIL_CONSTRAINT;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(propertyValue == null) {
            return; // A null is not a value we should even check
        }

        if(email) {
            EmailValidator emailValidator = EmailValidator.getInstance();
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue };
            if(!emailValidator.isValid(propertyValue.toString())  ) {
                super.rejectValue(target,errors,ConstrainedProperty.EMAIL_CONSTRAINT + ConstrainedProperty.INVALID_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_EMAIL_MESSAGE_CODE, args));
            }
        }
    }
}
