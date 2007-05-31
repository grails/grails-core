/* Copyright 2004-2005 the original author or authors.
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
 * A Constraint that validates not equal to something
 */
class NotEqualConstraint extends AbstractConstraint {

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    public boolean supports(Class type) {
        return type != null;
    }

    public String getName() {
        return ConstrainedProperty.NOT_EQUAL_CONSTRAINT;
    }

    /**
     * @return Returns the notEqualTo.
     */
    public Object getNotEqualTo() {
        return this.constraintParameter;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(this.constraintParameter.equals( propertyValue )) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, constraintParameter  };
            super.rejectValue( target,errors,ConstrainedProperty.DEFAULT_NOT_EQUAL_MESSAGE_CODE, ConstrainedProperty.NOT_EQUAL_CONSTRAINT,args);
        }
    }

}