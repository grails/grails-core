package org.codehaus.groovy.grails.validation;

import java.util.List;
import java.util.ArrayList;

/**
 * Test cases for 'inList' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class InListConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return InListConstraint.class;
    }

    private List getTestList() {
        List result = new ArrayList();
        result.add("one");
        result.add("two");
        result.add("three");
        return result;
    }

    public void testValidation() {
        List avail = getTestList();

        testConstraintMessageCodes(
                getConstraint( "testString", avail ),
                "four",
                new String[] {"testClass.testString.inList.error","testClass.testString.not.inList"},
                new Object[] {"testString",TestClass.class,"four",avail}
        );

        testConstraintFailed(
                getConstraint( "testString", avail ),
                "four"
        );

        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testString", avail ),
                null
        );

        // A blank string must also be allowed
        testConstraintPassed(
                getConstraint( "testString", avail ),
                "   "
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", avail ),
                "a",
                "Property [{0}] of class [{1}] with value [{2}] is not contained within the list [{3}]"
        );

    }

    public void testConstraintCreation() {
        InListConstraint constraint = new InListConstraint();
        assertEquals( ConstrainedProperty.IN_LIST_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertTrue( constraint.supports( Long.class ));
        assertTrue( constraint.supports( Object.class ));
        assertFalse( constraint.supports( null ));

        constraint = (InListConstraint) getConstraint( "testString", getTestList() );
        assertEquals( constraint.getList(), getTestList() );
        
        try {
            getConstraint( "testString", "wrong");
            fail("InListConstraint must throw an exception for non-list parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }

}
