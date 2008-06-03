package org.codehaus.groovy.grails.validation;

/**
 * Test cases for 'creditCard' constraint. Uses data from Apache Jakarta Commons Validator tests.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class CreditCardConstraintTests extends AbstractConstraintTests {
    private static final String VALID_VISA = "4417123456789113";
    private static final String VALID_SHORT_VISA = "4222222222222";
    private static final String VALID_AMEX = "378282246310005";
    private static final String VALID_MASTERCARD = "5105105105105100";
    private static final String VALID_DISCOVER = "6011000990139424";

    protected Class getConstraintClass() {
        return CreditCardConstraint.class;
    }

    public void testValidate() {
        testConstraintMessageCodes(
                getConstraint( "testString", Boolean.TRUE ),
                "1234512",
                new String[] {"testClass.testString.creditCard.error", "testClass.testString.creditCard.invalid"},
                new Object[] {"testString", TestClass.class, "1234512"}
        );

        // too short number
        testConstraintFailed(
                getConstraint( "testString", Boolean.TRUE ),
                "123456789012"
        );
        // too long number
        testConstraintFailed(
                getConstraint( "testString", Boolean.TRUE ),
                "12345678901234567890"
        );
        // non-digit symbols in number
        testConstraintFailed(
                getConstraint( "testString", Boolean.TRUE ),
                "4417q23456w89113"
        );
        // non-digit symbols in number
        testConstraintFailed(
                getConstraint( "testString", Boolean.TRUE ),
                "4417q23456w89113"
        );
        // wrong number (luhn check)
        testConstraintFailed(
                getConstraint( "testString", Boolean.TRUE ),
                "4417123456789112"
        );


        // null value should always pass validation
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                null
        );

        // blank value should always pass validation
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                " "
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                VALID_VISA
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                VALID_SHORT_VISA
        );

        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                VALID_AMEX
        );
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                VALID_MASTERCARD
        );
        testConstraintPassed(
                getConstraint( "testString", Boolean.TRUE ),
                VALID_DISCOVER
        );

        // must always pass when parameter is false
        testConstraintPassed(
                getConstraint( "testString", Boolean.FALSE ),
                "123"
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", Boolean.TRUE),
                "12345",
                "Property [{0}] of class [{1}] with value [{2}] is not a valid credit card number"
        );
    }

    public void testConstraintCreation() {
        CreditCardConstraint constraint = new CreditCardConstraint();
        assertEquals( ConstrainedProperty.CREDIT_CARD_CONSTRAINT, constraint.getName());
        assertTrue( constraint.supports( String.class ));
        assertFalse( constraint.supports( null ));
        assertFalse( constraint.supports( Long.class ));

        try {
            getConstraint( "testString", "wrong");
            fail("CreditCardConstraint must throw an exception for non-boolean parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
