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
 * A constraint that validates the property against a supplied regular expression
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:22:46 AM
 */

class MatchesConstraint extends AbstractConstraint {

    private String regex;

    /**
     * @return Returns the regex.
     */
    public String getRegex() {
        return regex;
    }

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
        if(!(constraintParameter instanceof String))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MATCHES_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be of type [java.lang.String]");

        this.regex = (String)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.MATCHES_CONSTRAINT;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(!propertyValue.toString().matches( regex )) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, regex  };
            super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_DOESNT_MATCH_MESSAGE_CODE,ConstrainedProperty.MATCHES_CONSTRAINT + ConstrainedProperty.INVALID_SUFFIX,args);
        }
    }

}
