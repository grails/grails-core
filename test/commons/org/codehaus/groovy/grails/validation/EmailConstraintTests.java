package org.codehaus.groovy.grails.validation;

/**
 * Test cases for 'email' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class EmailConstraintTests extends AbstractConstraintTests {

    protected Class getConstraintClass() {
        return EmailConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testString", Boolean.TRUE ),
                "wrong_email",
                new String[] {"testClass.testString.email.error","testClass.testString.email.invalid"},
                new Object[] {"testString",TestClass.class,"wrong_email"}
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE),
                "test@example.com"
        );


        testConstraintDefaultMessage(
                getConstraint( "testString", Boolean.TRUE ),
                "wrong_email",
                "Property [{0}] of class [{1}] with value [{2}] is not a valid e-mail address"
        );

    }

    public void testNullValue() {
        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE),
                null
        );
    }

    public void testBlankString() {
        // must always pass for blank value
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE),
                ""
        );
    }


    public void testCreation() {
        EmailConstraint constraint = new EmailConstraint();
        assertEquals( ConstrainedProperty.EMAIL_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertFalse( constraint.supports( null ));
        assertFalse(  constraint.supports( Long.class ));

        try {
            getConstraint( "testString", "wrong");
            fail("EmailConstraint must throw an exception for non-boolean parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
