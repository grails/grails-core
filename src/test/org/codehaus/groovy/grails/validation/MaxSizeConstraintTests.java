package org.codehaus.groovy.grails.validation;

import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Test cases for 'maxSize' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MaxSizeConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return MaxSizeConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testString", new Integer( 10 )),
                "12345678901",
                new String[] {"testClass.testString.maxSize.error","testClass.testString.maxSize.exceeded"},
                new Object[] {"testString",TestClass.class,"12345678901",new Integer( 10 )}
        );

        testConstraintFailed(
                getConstraint( "testArray", new Integer(2)),
                new String[] {"one","two","three"}
        );

        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");

        testConstraintFailed(
                getConstraint( "testCollection", new Integer(2)),
                list
        );

        testConstraintPassed(
                getConstraint( "testCollection", new Integer(3)),
                list
        );

        testConstraintPassed(
                getConstraint( "testString", new Integer(5)),
                "12345"
        );

        // must always pass for null value
        testConstraintPassed(
                getConstraint( "testString", new Integer(5)),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", new Integer(5) ),
                "123456",
                "Property [{0}] of class [{1}] with value [{2}] exceeds the maximum size of [{3}]"
        );

    }

    public void testCreation() {
        MaxSizeConstraint constraint = (MaxSizeConstraint) getConstraint( "testString", new Integer(10) );
        assertEquals( ConstrainedProperty.MAX_SIZE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertTrue( constraint.supports( Object[].class ));
        assertTrue( constraint.supports( List.class ));
        assertTrue( constraint.supports( Set.class ));
        assertTrue( constraint.supports( Collection.class ));
        assertFalse( constraint.supports( Integer.class ));
        assertFalse( constraint.supports( Number.class ));
        assertEquals( 10, constraint.getMaxSize());

        try {
            getConstraint( "testString", "wrong");
            fail("MaxSizeConstraint must throw an exception for non-integer parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }
}
