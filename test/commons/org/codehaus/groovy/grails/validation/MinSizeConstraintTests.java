package org.codehaus.groovy.grails.validation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;

/**
 * Test cases for 'minSize' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class MinSizeConstraintTests extends AbstractConstraintTests{
    protected Class getConstraintClass() {
        return MinSizeConstraint.class;
    }

    public void testValidation() {
        // 9 characters but 10 is minimum
        testConstraintMessageCodes(
                getConstraint( "testString", new Integer( 10 )),
                "123456789",
                new String[] {"testClass.testString.minSize.error","testClass.testString.minSize.notmet"},
                new Object[] {"testString",TestClass.class,"123456789",new Integer( 10 )}
        );

        testConstraintFailed(
                getConstraint( "testArray", new Integer(4)),
                new String[] {"one","two","three"}
        );

        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");

        testConstraintFailed(
                getConstraint( "testCollection", new Integer(4)),
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
                "1234",
                "Property [{0}] of class [{1}] with value [{2}] is less than the minimum size of [{3}]"
        );

    }

    public void testCreation() {
        MinSizeConstraint constraint = (MinSizeConstraint) getConstraint( "testString", new Integer(10) );
        assertEquals( ConstrainedProperty.MIN_SIZE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( String.class ));
        assertTrue( constraint.supports( Object[].class ));
        assertTrue( constraint.supports( List.class ));
        assertTrue( constraint.supports( Set.class ));
        assertTrue( constraint.supports( Collection.class ));
        assertFalse( constraint.supports( Integer.class ));
        assertFalse( constraint.supports( Number.class ));
        assertEquals( 10, constraint.getMinSize());

        try {
            getConstraint( "testString", "wrong");
            fail("MinSizeConstraint must throw an exception for non-integer parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }

}
