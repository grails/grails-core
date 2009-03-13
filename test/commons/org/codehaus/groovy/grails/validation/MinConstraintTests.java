package org.codehaus.groovy.grails.validation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test cases for 'min' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MinConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return MinConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testFloat", new Float(1.5d)),
                new Float(1.4d),
                new String[] {"testClass.testFloat.min.error","testClass.testFloat.min.notmet"},
                new Object[] {"testFloat",TestClass.class,new Float(1.4d),new Float(1.5d)}
        );

        testConstraintPassed(
                getConstraint( "testFloat", new Float( 1.5d )),
                new Float( 1.7d )
        );

        testConstraintFailed(
                getConstraint( "testLong", new Long( 15000 )),
                new Long( 10000 )
        );

        // bound is included in valid interval
        testConstraintPassed(
                getConstraint( "testInteger", new Integer( 100 )),
                new Integer( 100 )
        );

        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testFloat", new Float( 1.5d )),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testFloat", new Float( 1.7 ) ),
                new Float( 1.5 ),
                "Property [{0}] of class [{1}] with value [{2}] is less than minimum value [{3}]"
        );

    }

    public void testCreation() {
        MinConstraint constraint = new MinConstraint();
        assertEquals( ConstrainedProperty.MIN_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( int.class ));
        assertTrue( constraint.supports( float.class ));
        assertTrue( constraint.supports( Integer.class ));
        assertTrue( constraint.supports( Float.class ));
        assertTrue( constraint.supports( BigDecimal.class ));
        assertFalse( constraint.supports( List.class ));
        assertFalse( constraint.supports( null ));

        constraint = (MinConstraint) getConstraint( "testLong", new Long(100) );
        assertEquals( new Long( 100 ), constraint.getMinValue() );

        try {
            getConstraint( "testFloat", new Object() );
            fail("MinConstraint must throw an exception for non-comparable parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint( "testFloat", new Double(4d) );
            fail("MinConstraint must throw an exception for parameter with wrong type .");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint( "testBigDecimal", new Integer(5) );
            fail("MinConstraint must throw an exception for parameter with wrong type .");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }

}
