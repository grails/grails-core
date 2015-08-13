package org.grails.validation;

import grails.validation.AbstractConstraintTests;
import grails.validation.ConstrainedProperty;
import grails.validation.TestClass;

/**
 * Test cases for 'email' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class EmailConstraintTests extends AbstractConstraintTests {

    @Override
    protected Class<?> getConstraintClass() {
        return EmailConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint("testString", Boolean.TRUE),
                "wrong_email",
                new String[] {"testClass.testString.email.error","testClass.testString.email.invalid"},
                new Object[] {"testString",TestClass.class,"wrong_email"});

        testConstraintPassed(
                getConstraint("testString", Boolean.TRUE),
                "test@example.com");

        testConstraintDefaultMessage(
                getConstraint("testString", Boolean.TRUE),
                "wrong_email",
                "Property [{0}] of class [{1}] with value [{2}] is not a valid e-mail address");
    }

    public void testNullValue() {
        // must always pass for null value
        testConstraintPassed(
                getConstraint("testString", Boolean.TRUE),
                null);
    }

    public void testBlankString() {
        // must always pass for blank value
        testConstraintPassed(
                getConstraint("testString", Boolean.TRUE),
                "");
    }

    /**
     * #9184 - com.org.apache.commons.validator.routines.EmailValidator claims
     * to not validate TLDs, but delegates to org.apache.commons.validator.routines.DomainValidator
     * which does so.
     */
    public void testUnrecognizedTld() {
        testConstraintFailed(
                getConstraint("testString", Boolean.TRUE),
                "somperson@someagency.agency"
        );

        testConstraintPassed(
                getConstraint("testString", Boolean.TRUE),
                "somperson@someagency.io"
        );

        // This is the example that "should" pass from the EmailValidator javadoc
        testConstraintFailed(
                getConstraint("testString", Boolean.TRUE),
                "nobody@noplace.somedog"
        );
    }


    public void testCreation() {
        EmailConstraint constraint = new EmailConstraint();
        assertEquals(ConstrainedProperty.EMAIL_CONSTRAINT, constraint.getName());
        assertTrue(constraint.supports(String.class));
        assertFalse(constraint.supports(null));
        assertFalse(constraint.supports(Long.class));

        try {
            getConstraint("testString", "wrong");
            fail("EmailConstraint must throw an exception for non-boolean parameters.");
        } catch (IllegalArgumentException iae) {
            // Great
        }
    }
}
