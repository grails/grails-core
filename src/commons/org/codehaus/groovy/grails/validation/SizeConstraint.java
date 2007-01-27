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

import java.util.Collection;
import java.lang.reflect.Array;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.springframework.validation.Errors;

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
        return type != null && (Comparable.class.isAssignableFrom(type) ||
        		GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, type) ||
                Collection.class.isAssignableFrom(type) ||
                type.isArray());
        
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

        Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue,  range.getFrom(), range.getTo()  };

        if(propertyValue == null) {
            return; // A null is not a value we should even check
        }

        if(propertyValue.getClass().isArray()) {
            Integer length = new Integer(Array.getLength( propertyValue ));
            if(!range.contains(length)) {

                if(range.getFrom().compareTo( length ) == 1) {
                    super.rejectValue(errors,ConstrainedProperty.LENGTH_CONSTRAINT + ConstrainedProperty.TOOSHORT_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_LENGTH_MESSAGE_CODE, args));
                }
                else if(range.getTo().compareTo(length) == -1) {
                    super.rejectValue(errors,ConstrainedProperty.LENGTH_CONSTRAINT + ConstrainedProperty.TOOLONG_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_LENGTH_MESSAGE_CODE, args));
                }
                return;
            }
        }
        if(propertyValue instanceof Collection) {
            Integer collectionSize = new Integer(((Collection)propertyValue).size());
            if(!range.contains( collectionSize )) {
                if(range.getFrom().compareTo( collectionSize ) == 1) {
                    super.rejectValue(errors,ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOSMALL_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_SIZE_MESSAGE_CODE, args));
                }
                else if(range.getTo().compareTo(collectionSize) == -1) {
                    super.rejectValue(errors,ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOBIG_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_SIZE_MESSAGE_CODE, args));
                }
            }
        }
        else if(propertyValue instanceof Number) {
            if(range.getFrom().compareTo( propertyValue ) == 1) {
                super.rejectValue(errors,ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOSMALL_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_SIZE_MESSAGE_CODE, args));
            }
            else if(range.getTo().compareTo(propertyValue) == -1) {
                super.rejectValue(errors,ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOBIG_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_SIZE_MESSAGE_CODE, args));
            }
        }
        else if(propertyValue instanceof String) {
            Integer stringLength =  new Integer(((String)propertyValue ).length());
            if(!range.contains(stringLength)) {
                if(range.getFrom().compareTo( stringLength ) == 1) {
                    super.rejectValue(errors,ConstrainedProperty.LENGTH_CONSTRAINT + ConstrainedProperty.TOOSHORT_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_LENGTH_MESSAGE_CODE, args));
                }
                else if(range.getTo().compareTo(stringLength) == -1) {
                    super.rejectValue(errors,ConstrainedProperty.LENGTH_CONSTRAINT + ConstrainedProperty.TOOLONG_SUFFIX,args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_LENGTH_MESSAGE_CODE, args));
                }
            }
        }
    }
}
