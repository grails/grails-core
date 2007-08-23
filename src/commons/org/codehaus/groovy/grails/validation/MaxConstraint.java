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

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
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
        		GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, type));
    }


    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(constraintParameter == null) {
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MAX_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] cannot be null");
        }
        if(!(constraintParameter instanceof Comparable) && (!constraintParameter.getClass().isPrimitive()))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MAX_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must implement the interface [java.lang.Comparable]");

        Class propertyClass = GrailsClassUtils.getPropertyType( constraintOwningClass, constraintPropertyName );
        if(!GrailsClassUtils.isAssignableOrConvertibleFrom( constraintParameter.getClass(),propertyClass ))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MAX_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be the same type as property: [" + propertyClass.getName() + "]");

        this.maxValue = (Comparable)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.MAX_CONSTRAINT;
    }


    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(maxValue.compareTo(propertyValue) < 0) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, maxValue  };
            super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_MAX_MESSAGE_CODE,ConstrainedProperty.MAX_CONSTRAINT + ConstrainedProperty.EXCEEDED_SUFFIX,args);
        }
    }
}
