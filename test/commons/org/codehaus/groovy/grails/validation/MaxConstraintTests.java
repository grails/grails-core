package org.codehaus.groovy.grails.validation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test cases for 'max' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MaxConstraintTests extends AbstractConstraintTests{
    protected Class getConstraintClass() {
        return MaxConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testFloat", new Float(1.5d)),
                new Float(1.7d),
                new String[] {"testClass.testFloat.max.error","testClass.testFloat.max.exceeded"},
                new Object[] {"testFloat",TestClass.class,new Float(1.7d),new Float(1.5d)}
        );

        testConstraintPassed(
                getConstraint( "testFloat", new Float( 1.5d )),
                new Float( 1.4d )
        );

        testConstraintFailed(
                getConstraint( "testLong", new Long( 150 )),
                new Long( 10000 )
        );

        testConstraintFailed(
                getConstraint( "testBigDecimal", new BigDecimal( "123.45" )),
                new BigDecimal( "123.46" )
        );

        // bound is included in valid interval
        testConstraintPassed(
                getConstraint( "testInteger", new Integer( 100 )),
                new Integer( 100 )
        );

        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testFloat", new Float( 1.5 )),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testFloat", new Float( 1.5 ) ),
                new Float( 1.7 ),
                "Property [{0}] of class [{1}] with value [{2}] exceeds maximum value [{3}]"
        );

    }

    public void testCreation() {
        MaxConstraint constraint = new MaxConstraint();
        assertEquals( ConstrainedProperty.MAX_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( int.class ));
        assertTrue( constraint.supports( float.class ));
        assertTrue( constraint.supports( Integer.class ));
        assertTrue( constraint.supports( Float.class ));
        assertTrue( constraint.supports( BigDecimal.class ));
        assertFalse( constraint.supports( List.class ));
        assertFalse( constraint.supports( null ));

        constraint = (MaxConstraint) getConstraint( "testLong", new Long(100) );
        assertEquals( new Long( 100 ), constraint.getMaxValue() );
        
        try {
            getConstraint( "testFloat", new Object() );
            fail("MaxConstraint must throw an exception for non-comparable parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint( "testFloat", new Double(4d) );
            fail("MaxConstraint must throw an exception for parameter with wrong type .");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

        // property is Float but parameter is Double
        try {
            getConstraint( "testBigDecimal", new Integer(5) );
            fail("MaxConstraint must throw an exception for parameter with wrong type .");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
