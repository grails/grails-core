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

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * A constraint that validates minimum size or length of the property, for strings and arrays this is the length, collections
 * the size and numbers the value
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:30:39 AM
 */
class MinSizeConstraint extends AbstractConstraint {

    private int minSize;

    /**
     * @return Returns the minSize.
     */
    public int getMinSize() {
        return minSize;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Integer))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.MIN_SIZE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a of type [java.lang.Integer]");

        this.minSize = ((Integer)constraintParameter).intValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.MIN_SIZE_CONSTRAINT;
    }


    /* (non-Javadoc)
       * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
       */
    public boolean supports(Class type) {
        return type != null && (
                String.class.isAssignableFrom(type) ||
                Collection.class.isAssignableFrom(type) ||
                type.isArray()
        );
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, new Integer(minSize) };

        if(propertyValue.getClass().isArray()) {
            int length = Array.getLength( propertyValue );
            if(length < minSize) {
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE, ConstrainedProperty.MIN_SIZE_CONSTRAINT + ConstrainedProperty.NOTMET_SUFFIX,args );
            }
        }
        else if(propertyValue instanceof Collection) {
            if( ((Collection)propertyValue).size() < minSize ) {
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE, ConstrainedProperty.MIN_SIZE_CONSTRAINT + ConstrainedProperty.NOTMET_SUFFIX,args );
            }
        }
        else if(propertyValue instanceof String) {
            if(((String)propertyValue ).length() < minSize) {
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE,ConstrainedProperty.MIN_SIZE_CONSTRAINT + ConstrainedProperty.NOTMET_SUFFIX,args);
            }
        }
    }
}
