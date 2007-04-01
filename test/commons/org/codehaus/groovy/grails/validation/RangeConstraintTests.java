package org.codehaus.groovy.grails.validation;

import groovy.lang.IntRange;
import groovy.lang.ObjectRange;

/**
 * Test cases for 'range' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class RangeConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return RangeConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testInteger", new IntRange( 1, 5 )),
                new Integer(7),
                new String[] {"testClass.testInteger.range.error","testClass.testInteger.size.toobig"},
                new Object[] {"testInteger",TestClass.class,new Integer(7),new Integer(1),new Integer(5)}
        );

        testConstraintMessageCodes(
                getConstraint( "testInteger", new IntRange( 1, 5 )),
                new Integer(0),
                new String[] {"testClass.testInteger.range.error","testClass.testInteger.size.toosmall"},
                new Object[] {"testInteger",TestClass.class,new Integer(0),new Integer(1),new Integer(5)}
        );

        testConstraintPassed(
                getConstraint( "testString", new ObjectRange("abca","abcf")),
                "abcd"
        );

        testConstraintPassed(
                getConstraint( "testInteger", new IntRange( 1, 7 )),
                new Integer( 5 )
        );

        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testInteger", new IntRange( 1, 7 )),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testInteger", new IntRange( 1, 5 ) ),
                new Integer( 7 ),
                "Property [{0}] of class [{1}] with value [{2}] does not fall within the valid range from [{3}] to [{4}]"
        );

    }

    public void testCreation() {
        RangeConstraint constraint = (RangeConstraint) getConstraint( "testInteger", new IntRange(1,5) );
        assertEquals( ConstrainedProperty.RANGE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( Integer.class ));
        assertTrue(  constraint.supports( Long.class ));
        assertTrue( constraint.supports( Double.class ));
        assertFalse( constraint.supports( Object.class ));
        assertFalse( constraint.supports( null ));
        assertEquals( new IntRange(1,5), constraint.getRange() );

        try {
            getConstraint( "testInteger", "wrong");
            fail("RangeConstraint must throw an exception for non-range parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

    }
}
