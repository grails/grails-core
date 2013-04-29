/*
 * Copyright 2004-2005 the original author or authors.
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

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.springframework.validation.Errors;

/**
 * Validates not equal to something.
 */
public class NotEqualConstraint extends AbstractConstraint {

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    public boolean supports(Class type) {
        return type != null;
    }

    public String getName() {
        return ConstrainedProperty.NOT_EQUAL_CONSTRAINT;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    @Override
    public void setParameter(Object constraintParameter) {
        if (constraintParameter == null) {
            throw new IllegalArgumentException("Parameter for constraint [" + ConstrainedProperty.NOT_EQUAL_CONSTRAINT +
                    "] of property [" + constraintPropertyName + "] of class [" +
                    constraintOwningClass + "] cannot be null");
        }

        Class<?> propertyClass = GrailsClassUtils.getPropertyType(constraintOwningClass, constraintPropertyName);
        // TODO: Find an alternative way to do the UrlMapping check!
        if (!GrailsClassUtils.isAssignableOrConvertibleFrom(constraintParameter.getClass(),propertyClass)  && propertyClass != null) {
            throw new IllegalArgumentException("Parameter for constraint [" +
                    ConstrainedProperty.NOT_EQUAL_CONSTRAINT + "] of property [" +
                    constraintPropertyName + "] of class [" + constraintOwningClass +
                    "] must be the same type as property: [" + propertyClass.getName() + "]");
        }
        super.setParameter(constraintParameter);
    }

    /**
     * @return Returns the notEqualTo.
     */
    public Object getNotEqualTo() {
        return constraintParameter;
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (constraintParameter.equals(propertyValue)) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, constraintParameter };
            rejectValue(target, errors, ConstrainedProperty.DEFAULT_NOT_EQUAL_MESSAGE_CODE,
                    ConstrainedProperty.NOT_EQUAL_CONSTRAINT, args);
        }
    }
}
