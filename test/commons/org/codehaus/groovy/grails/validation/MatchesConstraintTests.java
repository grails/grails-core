package org.codehaus.groovy.grails.validation;

/**
 * Test cases for 'matches' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MatchesConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return MatchesConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testString", "[a-zA-Z]"),
                "$",
                new String[] {"testClass.testString.matches.error","testClass.testString.matches.invalid"},
                new Object[] {"testString",TestClass.class,"$","[a-zA-Z]"}
        );

        testConstraintPassed(
                getConstraint( "testString", "[a-zA-Z]+"),
                "asdfdf"
        );

        // must always pass for null values
        testConstraintPassed(
                getConstraint( "testString", "[a-zA-Z]+" ),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", "[a-zA-Z]+" ),
                "$",
                "Property [{0}] of class [{1}] with value [{2}] does not match the required pattern [{3}]"
        );

    }

    public void testCreation() {
        MatchesConstraint constraint = new MatchesConstraint();
        assertEquals( ConstrainedProperty.MATCHES_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertFalse( constraint.supports( null ));
        assertFalse(  constraint.supports( Long.class ));

        constraint = (MatchesConstraint) getConstraint( "testString", "[a-z]");
        assertEquals( "[a-z]", constraint.getRegex());
        
        try {
            getConstraint( "testString", new Long(123));
            fail("MatchesConstraint must throw an exception for non-string parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
