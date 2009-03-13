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

import java.math.BigDecimal;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.Errors;

/**
 * A constraint to manage the scale for floating point numbers (i.e., the
 * number of digits to the right of the decimal point).
 *
 * This constraint supports properties of the following types:
 * <ul>
 * <li>java.lang.Float</li>
 * <li>java.lang.Double</li>
 * <li>java.math.BigDecimal (and its subclasses)</li>
 * </ul>
 *
 * When applied, this constraint determines if the number includes more
 * nonzero decimal places than the scale permits. If so, it rounds the number
 * to the maximum number of decimal places allowed by the scale.
 *
 * The rounding behavior described above occurs automatically when the
 * constraint is applied. This constraint does <i>not</i> generate
 * validation errors.

 *
 * @author Jason Rudolph
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 8:23:44 AM
 */
class ScaleConstraint extends AbstractConstraint {

    private int scale;

    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.groovy.grails.validation.Constraint#supports(java.lang.Class)
     */
    public boolean supports(Class type) {
        return type != null && (BigDecimal.class.isAssignableFrom(type) ||
        		GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, type) ||
        		GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, type));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.groovy.grails.validation.Constraint#getName()
     */
    public String getName() {
        return ConstrainedProperty.SCALE_CONSTRAINT;
    }

    /**
     * @return the scale
     */
    public int getScale() {
        return this.scale;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if (!(constraintParameter instanceof Integer)) {
            throw new IllegalArgumentException("Parameter for constraint [" + this.getName() + "] of property ["
                    + this.constraintPropertyName + "] of class [" + this.constraintOwningClass
                    + "] must be a of type [java.lang.Integer]");
        }

        int requestedScale = ((Integer) constraintParameter).intValue();

        if (requestedScale < 0) {
            throw new IllegalArgumentException("Parameter for constraint [" + this.getName() + "] of property ["
                    + this.constraintPropertyName + "] of class [" + this.constraintOwningClass
                    + "] must have a nonnegative value");
        }

        this.scale = requestedScale;
        super.setParameter(constraintParameter);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#processValidate(java.lang.Object,
     *      java.lang.Object, org.springframework.validation.Errors)
     */
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        BigDecimal bigDecimal;

        BeanWrapper bean = new BeanWrapperImpl(target);

        if (propertyValue instanceof Float) {
            bigDecimal = new BigDecimal(propertyValue.toString());
            bigDecimal = getScaledValue(bigDecimal);
            bean.setPropertyValue(this.getPropertyName(), new Float(bigDecimal.floatValue()));
        } else if (propertyValue instanceof Double) {
            bigDecimal = new BigDecimal(propertyValue.toString());
            bigDecimal = getScaledValue(bigDecimal);
            bean.setPropertyValue(this.getPropertyName(), new Double(bigDecimal.doubleValue()));
        } else if (propertyValue instanceof BigDecimal) {
            bigDecimal = (BigDecimal) propertyValue;
            bigDecimal = getScaledValue(bigDecimal);
            bean.setPropertyValue(this.getPropertyName(), bigDecimal);
        } else {
            throw new IllegalArgumentException("Unsupported type detected in constraint [" + this.getName()
                    + "] of property [" + constraintPropertyName + "] of class [" + constraintOwningClass + "]");
        }
    }

    /**
     * @return the <code>BigDecimal</code> object that results from applying the contraint's scale to the underlying number
     * @param originalValue The original value
     */
    private BigDecimal getScaledValue(BigDecimal originalValue) {
        return originalValue.setScale(this.scale, BigDecimal.ROUND_HALF_UP);
    }
}
