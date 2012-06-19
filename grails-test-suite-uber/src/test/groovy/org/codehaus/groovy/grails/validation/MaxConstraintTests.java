package org.codehaus.groovy.grails.validation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test cases for 'max' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MaxConstraintTests extends AbstractConstraintTests{
    @Override
    protected Class<?> getConstraintClass() {
        return MaxConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint("testFloat", 1.5f), 1.7f,
                new String[] {"testClass.testFloat.max.error","testClass.testFloat.max.exceeded"},
                new Object[] {"testFloat",TestClass.class, 1.7f, 1.5f});

        testConstraintPassed(getConstraint("testFloat", 1.5f), 1.4f);

        testConstraintFailed(getConstraint("testLong", 150L), 10000L);

        testConstraintFailed(
                getConstraint("testBigDecimal", new BigDecimal("123.45")),
                new BigDecimal("123.46"));

        // bound is included in valid interval
        testConstraintPassed(getConstraint("testInteger", 100), 100);

        // must always pass for null value
        testConstraintPassed(getConstraint("testFloat", 1.5f), null);

        testConstraintDefaultMessage(
                getConstraint("testFloat", 1.5f), 1.7f,
                "Property [{0}] of class [{1}] with value [{2}] exceeds maximum value [{3}]");
    }

    public void testCreation() {
        MaxConstraint constraint = new MaxConstraint();
        assertEquals(ConstrainedProperty.MAX_CONSTRAINT, constraint.getName());
        assertTrue(constraint.supports(int.class));
        assertTrue(constraint.supports(float.class));
        assertTrue(constraint.supports(Integer.class));
        assertTrue(constraint.supports(Float.class));
        assertTrue(constraint.supports(BigDecimal.class));
        assertFalse(constraint.supports(List.class));
        assertFalse(constraint.supports(null));

        constraint = (MaxConstraint) getConstraint("testLong", 100L);
        assertEquals(100L, constraint.getMaxValue());

        try {
            getConstraint("testFloat", new Object());
            fail("MaxConstraint must throw an exception for non-comparable parameters.");
        } catch (IllegalArgumentException iae) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint("testFloat", 4d);
            fail("MaxConstraint must throw an exception for parameter with wrong type .");
        } catch (IllegalArgumentException iae) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint("testBigDecimal", 5);
            fail("MaxConstraint must throw an exception for parameter with wrong type .");
        } catch (IllegalArgumentException iae) {
            // Great
        }
    }
}
