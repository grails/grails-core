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

import java.util.List;

/**
 *  A constraint that validates the property is contained within the supplied list
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:21:24 AM
 */
class InListConstraint extends AbstractConstraint {

    List list;

    /**
     * @return Returns the list.
     */
    public List getList() {
        return list;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    public boolean supports(Class type) {
        return type != null;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof List))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.IN_LIST_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must implement the interface [java.util.List]");

        this.list = (List)constraintParameter;
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.IN_LIST_CONSTRAINT;
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        // Check that the list contains the given value. If not, add
        // an error.
        if(!this.list.contains(propertyValue)) {
            Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue, list  };
            super.rejectValue(target,errors,ConstrainedProperty.DEFAULT_NOT_INLIST_MESSAGE_CODE,ConstrainedProperty.NOT_PREFIX + ConstrainedProperty.IN_LIST_CONSTRAINT,args);
        }
    }

}
