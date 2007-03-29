/* Copyright 2004-2005 Marc Palmer
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

import groovy.lang.Closure;
import org.springframework.validation.Errors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

import java.util.Collection;

/**
 * <p>A constraint class that validates using a user-supplied closure.</p>
 * <p>The Closure will receive one or two parameters containing the new value of the property and the object
 * on which the validation is being performed. The value is always the first parameterm and the object is the second.
 * These parameters must be type compatible with the value of the property and constrained class.</p>
 *
 * <p>
 * The Closure can return any of:
 * </p>
 * <ul>
 * <li>NULL to indicate success
 * <li>true to indicate success
 * <li>false to indicate a failure, with the default failure message
 * <li>a string to indicate a failure with the specific error code which will be appended to the
 * prefix for the constrained class and property i.e. classname.propertyname.stringfromclosurehere
 * <li>a list containing an error code and any other arguments for the error message. The error code will
 * be appended to the standard classname.propertyname prefix and the arguments made available to the
 * error message as parameters numbered 3 onwards.
 * </ul>
 *
 * @author Marc Palmer
 * @since 0.4
 *
 *        Created: Jan 19, 2007
 *        Time: 8:44:39 AM
 */
class ValidatorConstraint extends AbstractConstraint {
    private Closure validator;
    private int numValidatorParams;

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if(validator != null) {

            Object[] params = numValidatorParams == 2
                    ? new Object[] { propertyValue, target }
                    : new Object[] { propertyValue };

            final Object result = validator.call(params);

            boolean bad = false;
            String errmsg = null;
            Object[] args = null;

            if (result != null)
            {
                if (result instanceof Boolean)
                {
                    bad = !((Boolean)result).booleanValue();
                }
                else if (result instanceof String)
                {
                    bad = true;
                    errmsg = (String)result;
                }
                else if ((result instanceof Collection) || result.getClass().isArray())
                {
                    bad = true;
                    Object[] values = (result instanceof Collection) ? ((Collection)result).toArray() : (Object[])result;
                    if(!(values[0] instanceof String))
                    {
                        throw new IllegalArgumentException("Return value from validation closure ["
                            +ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["
                            +constraintOwningClass+"] is returning a list but the first element must be a string " +
                            "containing the error message code");
                    }
                    errmsg = (String)values[0];
                    args = new Object[values.length - 1 + 3];
                    int i = 0;
                    args[i++] = constraintPropertyName;
                    args[i++] = constraintOwningClass;
                    args[i++] = propertyValue;
                    System.arraycopy( values, 1, args, i, values.length-1 );
                } else {
                    throw new IllegalArgumentException("Return value from validation closure ["
                        +ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["
                        +constraintOwningClass+"] must be a boolean, a string, an array or a collection");
                }
            }
            if( bad ) {
                if (args == null)
                {
                    args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue };
                }
                super.rejectValue(errors, errmsg == null ? ConstrainedProperty.VALIDATOR_CONSTRAINT + ConstrainedProperty.INVALID_SUFFIX : errmsg,
                    args,getDefaultMessage(ConstrainedProperty.DEFAULT_INVALID_VALIDATOR_MESSAGE_CODE, args));
            }
        }
    }

    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Closure))
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a Closure");

        this.validator = (Closure)constraintParameter;

        Class[] params = this.validator.getParameterTypes();
        // Groovy should always force one parameter, but let's check anyway...
        if (params.length == 0)
        {
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a Closure taking at least 1 parameter (value, [object])");
        } else if (params.length > 2)
        {
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a Closure taking no more than 2 parameters (value, [object])");
        }

        numValidatorParams = params.length;

        BeanWrapper wr = new BeanWrapperImpl(constraintOwningClass);
        Class propType = wr.getPropertyType(constraintPropertyName);

        if (!GrailsClassUtils.isGroovyAssignableFrom(params[0], propType))
        {
            throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of " +
                "property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a Closure " +
                "taking with the first parameter (value) compatible with the type of the " +
                "property ["+propType+"], but the parameter is of type ["+params[0]+"]");
        }
        if (params.length > 1)
        {
            if (!GrailsClassUtils.isGroovyAssignableFrom(params[1], constraintOwningClass))
            {
                throw new IllegalArgumentException("Parameter for constraint ["+ConstrainedProperty.VALIDATOR_CONSTRAINT+"] of " +
                    "property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a Closure " +
                    "taking with the second parameter (object) compatible with the type of the object being " +
                    "constrained ["+constraintOwningClass+"], but the parameter is of type ["+params[1]+"]");
            }
        }

        super.setParameter(constraintParameter);
    }

    public String getName() {
        return ConstrainedProperty.VALIDATOR_CONSTRAINT;
    }

    public boolean supports(Class type) {
        if(type == null)
            return false;

        return true;
    }
}
