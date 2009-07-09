package org.codehaus.groovy.grails.validation;

import groovy.lang.IntRange;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Test cases for 'size' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class SizeConstraintTests extends AbstractConstraintTests{
    protected Class getConstraintClass() {
        return SizeConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint( "testString", new IntRange( 2, 5 )),
                "123456",
                new String[] {"testClass.testString.size.error","testClass.testString.size.toobig"},
                new Object[] {"testString",TestClass.class,"123456",new Integer(2),new Integer(5)}
        );

        testConstraintMessageCodes(
                getConstraint( "testString", new IntRange( 2, 5 )),
                "1",
                new String[] {"testClass.testString.size.error","testClass.testString.size.toosmall"},
                new Object[] {"testString",TestClass.class,"1",new Integer(2),new Integer(5)}
        );

        testConstraintPassed(
                getConstraint( "testArray", new IntRange(2, 5)),
                new String[] {"one","two","three"}
        );

        List list = new ArrayList();
        list.add("one");
        testConstraintFailed(
                getConstraint( "testArray", new IntRange(2, 5)),
                list
        );
        list.add("two");
        testConstraintPassed(
                getConstraint( "testArray", new IntRange(2, 5)),
                list
        );

        // must always pass on null value
        testConstraintPassed(
                getConstraint( "testArray", new IntRange(2, 5)),
                null
        );

        testConstraintDefaultMessage(
                getConstraint( "testString", new IntRange( 1, 5 ) ),
                "123456",
                "Property [{0}] of class [{1}] with value [{2}] does not fall within the valid size range from [{3}] to [{4}]"
        );
    }

    public void testCreation() {
        SizeConstraint constraint = (SizeConstraint) getConstraint( "testInteger", new IntRange(1,5) );
        assertEquals( ConstrainedProperty.SIZE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( List.class ));
        assertTrue(  constraint.supports( Collection.class ));
        assertTrue( constraint.supports( Double[].class ));
        assertFalse( constraint.supports( Object.class ));
        assertFalse( constraint.supports( null ));
        assertFalse( constraint.supports( Integer.class ));
        assertFalse( constraint.supports( Number.class ));
        assertEquals( new IntRange(1,5), constraint.getRange() );

        try {
            getConstraint( "testInteger", "wrong");
            fail("SizeConstraint must throw an exception for non-range parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

    }

}
