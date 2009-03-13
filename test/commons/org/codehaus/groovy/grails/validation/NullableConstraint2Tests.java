package org.codehaus.groovy.grails.validation;

/**
 * Test cases for 'nullable' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class NullableConstraint2Tests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return NullableConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testString", Boolean.FALSE ),
                null,
                new String[] {"testClass.testString.nullable.error","testClass.testString.nullable"},
                new Object[] {"testString",TestClass.class }
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.FALSE ),
                "test"
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                ""
        );

        testConstraintFailedAndVetoed(
                getConstraint( "testString", Boolean.FALSE ),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", Boolean.FALSE ),
                null,
                "Property [{0}] of class [{1}] cannot be null"
        );
    }

    public void testCreation() {
        NullableConstraint constraint = (NullableConstraint) getConstraint( "testString", Boolean.FALSE );
        assertEquals( ConstrainedProperty.NULLABLE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertTrue( constraint.supports( Object.class ));
        assertFalse(  constraint.supports( int.class ));
        assertFalse(  constraint.supports( float.class ));
        assertFalse(  constraint.supports( null ));
        assertFalse( constraint.isNullable() );

        try {
            getConstraint( "testString", "wrong");
            fail("NullableConstraint must throw an exception for non-boolean parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

    }
}
