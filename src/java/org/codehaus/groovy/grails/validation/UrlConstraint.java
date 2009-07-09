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

import org.codehaus.groovy.grails.validation.routines.RegexValidator;
import org.codehaus.groovy.grails.validation.routines.UrlValidator;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * A Constraint that validates a url
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:16:04 AM
 */
class UrlConstraint extends AbstractConstraint {

    private boolean url;
    private UrlValidator validator;


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
        RegexValidator domainValidator = null;

        if (constraintParameter instanceof Boolean) {
            this.url = ((Boolean) constraintParameter).booleanValue();
        } else if (constraintParameter instanceof String) {
            this.url = true;
            domainValidator = new RegexValidator((String) constraintParameter);
        } else if (constraintParameter instanceof List) {
            this.url = true;
            List regexpList = (List) constraintParameter;
            domainValidator = new RegexValidator((String[]) regexpList.toArray(new String[regexpList.size()]));
        } else {
            throw new IllegalArgumentException("Parameter for constraint [" + ConstrainedProperty.URL_CONSTRAINT + "] of property [" + constraintPropertyName + "] of class [" + constraintOwningClass + "] must be a boolean, string, or list value");
        }

        validator = new UrlValidator(
                domainValidator,
                UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.ALLOW_2_SLASHES
        );

        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.URL_CONSTRAINT;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (url) {
            if (!validator.isValid(propertyValue.toString())) {
                Object[] args = new Object[]{constraintPropertyName, constraintOwningClass, propertyValue};
                super.rejectValue(target, errors, ConstrainedProperty.DEFAULT_INVALID_URL_MESSAGE_CODE, ConstrainedProperty.URL_CONSTRAINT + ConstrainedProperty.INVALID_SUFFIX, args);
            }
        }
    }

}

