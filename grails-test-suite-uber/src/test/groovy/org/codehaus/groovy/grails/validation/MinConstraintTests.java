package org.codehaus.groovy.grails.validation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test cases for 'min' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MinConstraintTests extends AbstractConstraintTests {
    @Override
    protected Class<?> getConstraintClass() {
        return MinConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint("testFloat", 1.5f), 1.4f,
                new String[] {"testClass.testFloat.min.error","testClass.testFloat.min.notmet"},
                new Object[] {"testFloat", TestClass.class, 1.4f, 1.5f });

        testConstraintPassed(getConstraint("testFloat", 1.5f), 1.7f);

        testConstraintFailed(getConstraint("testLong", 15000L), 10000L);

        // bound is included in valid interval
        testConstraintPassed(getConstraint("testInteger", 100), 100);

        // must always pass for null value
        testConstraintPassed(getConstraint("testFloat", 1.5f), null);

        testConstraintDefaultMessage(
                getConstraint("testFloat", 1.7f), 1.5f,
                "Property [{0}] of class [{1}] with value [{2}] is less than minimum value [{3}]");
    }

    public void testCreation() {
        MinConstraint constraint = new MinConstraint();
        assertEquals(ConstrainedProperty.MIN_CONSTRAINT, constraint.getName());
        assertTrue(constraint.supports(int.class));
        assertTrue(constraint.supports(float.class));
        assertTrue(constraint.supports(Integer.class));
        assertTrue(constraint.supports(Float.class));
        assertTrue(constraint.supports(BigDecimal.class));
        assertFalse(constraint.supports(List.class));
        assertFalse(constraint.supports(null));

        constraint = (MinConstraint) getConstraint("testLong", 100L);
        assertEquals(100L, constraint.getMinValue());

        try {
            getConstraint("testFloat", new Object());
            fail("MinConstraint must throw an exception for non-comparable parameters.");
        } catch (IllegalArgumentException iae) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint("testFloat", 4d);
            fail("MinConstraint must throw an exception for parameter with wrong type .");
        } catch (IllegalArgumentException iae) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint("testBigDecimal", 5);
            fail("MinConstraint must throw an exception for parameter with wrong type .");
        } catch (IllegalArgumentException iae) {
            // Great
        }
    }
}
