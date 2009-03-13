package org.codehaus.groovy.grails.validation;

/**
 * Tests for 'blank' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class BlankConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return BlankConstraint.class;
    }

    public void testValidate() {
        testConstraintMessageCodes(
                getConstraint( "testString", Boolean.FALSE ),
                "",
                new String[] {"testClass.testString.blank.error","testClass.testString.blank"},
                new Object[] {"testString", TestClass.class }
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.FALSE ),
                "someData"
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                "someData"
        );

        testConstraintFailedAndVetoed(
                getConstraint( "testString", Boolean.FALSE ),
                ""
        );

        testConstraintFailed(
                getConstraint( "testString", Boolean.FALSE ),
                "    "
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", Boolean.FALSE ),
                "",
                "Property [{0}] of class [{1}] cannot be blank"
        );
    }

    public void testConstraintCreation() {
        BlankConstraint constraint = new BlankConstraint();
        assertEquals( ConstrainedProperty.BLANK_CONSTRAINT, constraint.getName());
        assertTrue( constraint.supports( String.class ));
        assertFalse( constraint.supports( null ));
        assertFalse( constraint.supports( Long.class ));
        constraint = (BlankConstraint) getConstraint( "testString", Boolean.TRUE );
        assertEquals( Boolean.TRUE, constraint.getParameter() );

        try {
            getConstraint( "testString", "wrong");
            fail("BlankConstraint must throw an exception for non-boolean parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
