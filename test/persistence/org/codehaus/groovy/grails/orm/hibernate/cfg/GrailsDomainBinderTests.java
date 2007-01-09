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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import groovy.lang.IntRange;
import groovy.lang.ObjectRange;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.mapping.Column;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @author Jason Rudolph
 * @since 0.4
 * 
 * Created: 06-Jan-2007
 */
public class GrailsDomainBinderTests extends TestCase {

    /**
     * @see GrailsDomainBinder#bindStringColumnConstraints(Column, ConstrainedProperty)
     */
    public void testBindStringColumnConstraints() {
        // Verify that the correct length is set when a maxSize constraint is applied
        ConstrainedProperty constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(30));
        assertColumnLength(constrainedProperty, 30);

        // Verify that the correct length is set when a size constraint is applied
        constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(6, 32768));
        assertColumnLength(constrainedProperty, 32768);

        // Verify that the default length remains intact when no size-related constraints are applied
        constrainedProperty = getConstrainedStringProperty();
        assertColumnLength(constrainedProperty, Column.DEFAULT_LENGTH);
    }

    /**
     * @see GrailsDomainBinder#bindNumericColumnConstraints(Column, ConstrainedProperty)
     */
    public void testBindNumericColumnConstraints() {
        // Verify that the correct precision is set when the maxSize constraint has the number with the most digits
        ConstrainedProperty constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(123));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(0));
        assertColumnPrecisionAndScale(constrainedProperty, 3, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the minSize constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(123));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(-1234));
        assertColumnPrecisionAndScale(constrainedProperty, 4, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the high value of a size constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(0, 123));
        assertColumnPrecisionAndScale(constrainedProperty, 3, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the low value of a size constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(-1234, 123));
        assertColumnPrecisionAndScale(constrainedProperty, 4, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the max constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the minSize constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-123.45"));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the high value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("0"), new BigDecimal("123.45")));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the low value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("-123.45"), new BigDecimal("123")));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct scale is set when the scale constraint is specified in isolation
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(4));
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, 4);

        // Verify that the precision is set correctly for a floating point number with a min/max constraint and a scale...
        //  1) where the min/max constraint includes fewer decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(3));
        assertColumnPrecisionAndScale(constrainedProperty, 6, 3); // precision (6) = number of integer digits in max constraint ("123.45") + scale (3)

        //  2) where the min/max constraint includes more decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(3));
        assertColumnPrecisionAndScale(constrainedProperty, 7, 3); // precision (7) = number of digits in max constraint ("123.4567") 
    }

    private void assertColumnLength(ConstrainedProperty constrainedProperty, int expectedLength) {
        Column column = new Column();
        GrailsDomainBinder.bindStringColumnConstraints(column, constrainedProperty);
        assertEquals(expectedLength, column.getLength());
    }
    
    private void assertColumnPrecisionAndScale(ConstrainedProperty constrainedProperty, int expectedPrecision, int expectedScale) {
        Column column = new Column();
        GrailsDomainBinder.bindNumericColumnConstraints(column, constrainedProperty);
        assertEquals(expectedPrecision, column.getPrecision());
        assertEquals(expectedScale, column.getScale());
    }

    private ConstrainedProperty getConstrainedBigDecimalProperty() {
        return getConstrainedProperty("bigDecimalProperty");
    }

    private ConstrainedProperty getConstrainedStringProperty() {
        return getConstrainedProperty("stringProperty");
    }

    private ConstrainedProperty getConstrainedProperty(String propertyName) {
        BeanWrapper constrainedBean = new BeanWrapperImpl(new TestClass());
        return new ConstrainedProperty(constrainedBean.getClass(), propertyName, constrainedBean.getPropertyType(propertyName));
    }

    /**
     * Simple bean whose instances serve as test objects for the various binding tests.
     */
    private class TestClass {
        private BigDecimal bigDecimalProperty;
        private String stringProperty;
    }
}
