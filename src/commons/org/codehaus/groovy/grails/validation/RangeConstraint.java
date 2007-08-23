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

import groovy.lang.Range;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.springframework.validation.Errors;

/**
 * A Constraint that validates a range
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:17:32 AM
 */
class RangeConstraint extends AbstractConstraint {
    Range range;
    /**
     * @return Returns the range.
     */
    public Range getRange() {
        return range;
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
        if(!(constraintParameter instanceof Range))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.RANGE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a of type [groovy.lang.Range]");

        this.range = (Range)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.RANGE_CONSTRAINT;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(!this.range.contains(propertyValue)) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, range.getFrom(), range.getTo()  };

            if(range.getFrom().compareTo( propertyValue ) == 1) {
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_RANGE_MESSAGE_CODE, ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOSMALL_SUFFIX,args );
            }
            else if(range.getTo().compareTo(propertyValue) == -1) {
                super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_INVALID_RANGE_MESSAGE_CODE, ConstrainedProperty.SIZE_CONSTRAINT + ConstrainedProperty.TOOBIG_SUFFIX,args );
            }
        }
    }
}
