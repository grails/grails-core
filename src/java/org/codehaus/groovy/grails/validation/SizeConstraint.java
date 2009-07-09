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

import groovy.lang.IntRange;
import org.springframework.validation.Errors;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * A constraint that validates size of the property, for strings and arrays
 * this is the length, collections the size and numbers the value
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:26:10 AM
 */

class SizeConstraint extends AbstractConstraint {

    private IntRange range;

    /**
     * @return Returns the range.
     */
    public IntRange getRange() {
        return range;
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



    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof IntRange))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.SIZE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a of type [groovy.lang.IntRange]");

        this.range = (IntRange)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.SIZE_CONSTRAINT;
    }


    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, range.getFrom(), range.getTo() };

        // size of the property (e.g. String length(), Collection size(), etc.) 
        Integer size = null;

        // determine the value of size based on the property's type
        if(propertyValue.getClass().isArray()) {
            size = new Integer(Array.getLength( propertyValue ));
        }
        else if(propertyValue instanceof Collection) {
            size = new Integer(((Collection)propertyValue).size());
        }
        else if(propertyValue instanceof String) {
            size =  new Integer(((String)propertyValue).length());
        }
        
        if(!range.contains(size)) {
            if(range.getFrom().compareTo(size) == 1) {
                rejectValueTooSmall(args, errors, target);
            }
            else if(range.getTo().compareTo(size) == -1) {
                rejectValueTooBig(args, errors, target);
            }
        }
    }
    
    private void rejectValueTooSmall(Object[] args, Errors errors, Object target){
        rejectValue(args, errors, target, false);
    }
    
    private void rejectValueTooBig(Object[] args, Errors errors, Object target){
        rejectValue(args, errors, target, true);
    }
    
    private void rejectValue(Object[] args, Errors errors, Object target, boolean tooBig) {
        String suffix;
        if (tooBig) {
            suffix = ConstrainedProperty.TOOBIG_SUFFIX;
        } else {
            suffix = ConstrainedProperty.TOOSMALL_SUFFIX;
        }
        super.rejectValue(target,errors, ConstrainedProperty.DEFAULT_INVALID_SIZE_MESSAGE_CODE, ConstrainedProperty.SIZE_CONSTRAINT + suffix , args );
    }
}
