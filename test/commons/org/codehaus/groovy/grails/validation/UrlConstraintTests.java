package org.codehaus.groovy.grails.validation;

/**
 * Test cases for 'url' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class UrlConstraintTests extends AbstractConstraintTests{
    protected Class getConstraintClass() {
        return UrlConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testURL", Boolean.TRUE ),
                "wrong_url",
                new String[] {"testClass.testURL.url.error","testClass.testURL.url.invalid"},
                new Object[] {"testURL",TestClass.class,"wrong_url"}
        );

        testConstraintPassed(
                getConstraint( "testURL", Boolean.TRUE ),
                "http://www.google.com"
        );

        testConstraintPassed(
                getConstraint( "testURL", Boolean.TRUE ),
                "https://www.google.com/"
        );

        testConstraintPassed(
                getConstraint( "testURL", Boolean.TRUE ),
                "https://www.google.com/answers.py?test=1&second=2"
        );

        // must always pass when constraint is turned off
        testConstraintPassed(
                getConstraint( "testURL", Boolean.FALSE ),
                "wrong_url"
        );

        // must always pass on null values
        testConstraintPassed(
                getConstraint( "testURL", Boolean.TRUE ),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testURL", Boolean.TRUE ),
                "wrong_url",
                "Property [{0}] of class [{1}] with value [{2}] is not a valid URL"
        );

    }

    public void testCreation() {
        UrlConstraint constraint = (UrlConstraint) getConstraint( "testString", Boolean.FALSE );
        assertEquals( ConstrainedProperty.URL_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertFalse( constraint.supports( Float.class ));
        assertFalse( constraint.supports( Double.class ));
        assertFalse( constraint.supports( Object.class ));
        assertFalse( constraint.supports( null ));

        try {
            getConstraint( "testString", "wrong");
            fail("UrlConstraint must throw an exception for non-boolean parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

    }
}
