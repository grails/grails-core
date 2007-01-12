package org.codehaus.groovy.grails.validation;

import groovy.lang.*;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.validation.exceptions.ConstraintException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class ConstrainedPropertyTests extends TestCase {

    private static final Date BEGINNING_OF_TIME = new Date(0);
    private final Date NOW = new Date();
    private final Date ONE_DAY_FROM_NOW = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000));

    private int testValidatorValue = 0;

    public int getTestValidatorValue()
    {
        return testValidatorValue;
    }

    public void setTestValidatorValue(int testValidatorValue)
    {
        this.testValidatorValue = testValidatorValue;
    }

    /* 
     * Test method for 'org.codehaus.groovy.grails.validation.ConstrainedProperty.supportsContraint(String)'
     */
    public void testSupportsContraint() {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", String.class);

        assertTrue(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.LENGTH_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_LENGTH_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_LENGTH_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Collection.class);

        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Number.class);

        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Date.class);
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));


        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Object.class);

        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.LENGTH_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MAX_LENGTH_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MIN_LENGTH_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MAX_SIZE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MIN_SIZE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SIZE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Comparable.class);

        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.BLANK_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.EMAIL_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MATCHES_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Float.class);
        assertTrue(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Double.class);
        assertTrue(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", BigDecimal.class);
        assertTrue(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Integer.class);
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
        
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", Long.class);
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));

        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testProperty", BigInteger.class);
        assertFalse(cp.supportsContraint( ConstrainedProperty.SCALE_CONSTRAINT ));
    }

    public void testGetMin() {
        // validate that getMin returns null if the property has no min constraint and no range constraint
        ConstrainedProperty cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        assertNull(cp.getMin());

        // validate that getMin returns the correct value when the min constraint is defined for the property (but no range constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new Double(123.45));
        assertEquals(new Double(123.45), cp.getMin());
        
        // validate that getMin returns the correct value when the range constraint is defined for the property (but no min constraint is defined)
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(123.45), new Double(678.90)));
        assertEquals(new Double(123.45), cp.getMin());
        
        // validate that getMin returns the maximum of the min constraint and the lower bound of the range constraint
        //   1) validate where the lower bound of the range constraint is greater than the min constraint
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new Double(1.23));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(4.56), new Double(7.89)));
        assertEquals(new Double(4.56), cp.getMin());

        //   2) validate where the min constraint is greater than the lower bound of the range constraint
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new Double(4.56));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(1.23), new Double(7.89)));
        assertEquals(new Double(4.56), cp.getMin());
    }
    
    public void testGetMinSize() {
        // validate that getMinSize returns null if the property has no minSize constraint and no size constraint
        ConstrainedProperty cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        assertNull(cp.getMinSize());

        // validate that getMinSize returns the correct value when the minSize constraint is defined for the property (but no size constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(5));
        assertEquals(5, cp.getMinSize().intValue());

        // validate that getMinSize returns the correct value when the size constraint is defined for the property (but no minSize constraint is defined)
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(10, 20));
        assertEquals(10, cp.getMinSize().intValue());
        
        // validate that getMinSize returns the maximum of the minSize constraint and the lower bound of the size constraint
        //   1) validate where the lower bound of the size constraint is greater than the minSize constraint
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(6));
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(11, 21));
        assertEquals(11, cp.getMinSize().intValue());

        //   2) validate where the minSize constraint is greater than the lower bound of the size constraint
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(12));
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(9, 22));
        assertEquals(12, cp.getMinSize().intValue());
    }

    public void testGetMax() {
        // validate that getMax returns null if the property has no max constraint and no range constraint
        ConstrainedProperty cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        assertNull(cp.getMax());

        // validate that getMax returns the correct value when the max constraint is defined for the property (but no range constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new Double(123.45));
        assertEquals(new Double(123.45), cp.getMax());
        
        // validate that getMax returns the correct value when the range constraint is defined for the property (but no max constraint is defined)
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(123.45), new Double(678.90)));
        assertEquals(new Double(678.90), cp.getMax());
        
        // validate that getMax returns the minimum of the max constraint and the upper bound of the range constraint
        //   1) validate where the upper bound of the range constraint is less than the max constraint
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new Double(7.89));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(1.23), new Double(4.56)));
        assertEquals(new Double(4.56), cp.getMax());

        //   2) validate where the max constraint is less than the upper bound of the range constraint
        cp = new ConstrainedProperty(this.getClass(), "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new Double(4.56));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(1.23), new Double(7.89)));
        assertEquals(new Double(4.56), cp.getMax());
    }
    
    public void testGetMaxSize() {
        // validate that getMaxSize returns null if the property has no maxSize constraint and no size constraint
        ConstrainedProperty cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        assertNull(cp.getMaxSize());

        // validate that getMaxSize returns the correct value when the maxSize constraint is defined for the property (but no size constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(5));
        assertEquals(5, cp.getMaxSize().intValue());

        // validate that getMaxSize returns the correct value when the size constraint is defined for the property (but no maxSize constraint is defined)
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(10, 20));
        assertEquals(20, cp.getMaxSize().intValue());
        
        // validate that getMaxSize returns the minimum of the maxSize constraint and the upper bound of the size constraint
        //   1) validate where the upper bound of the size constraint is less than the maxSize constraint
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(29));
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(11, 21));
        assertEquals(21, cp.getMaxSize().intValue());

        //   2) validate where the maxSize constraint is less than the upper bound of the size constraint
        cp = new ConstrainedProperty(this.getClass(), "testURL", String.class);
        cp.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(12));
        cp.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(9, 22));
        assertEquals(12, cp.getMaxSize().intValue());
    }
    
    public void testBlankConstraint() { 
        // create a constraint tester for a domain class with a String property and a non-blank constraint
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.BLANK_CONSTRAINT, new Boolean(false));

        assertFalse(constraintTester.constrainedProperty.isBlank());

        // validate that an empty (i.e., blank) String yields an error
        constraintTester.testConstraint("", true);

        // validate that a non-empty String does *not* yield an error
        constraintTester.testConstraint("foo", false);

        // validate that a null String does not yield an error
        constraintTester.testConstraint(null, false);

    }

    public void testEmailConstraint() {
        // create a constraint tester for a domain class with a String property and an e-mail constraint
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testEmail", ConstrainedProperty.EMAIL_CONSTRAINT, new Boolean(true));

        // validate that a malformed e-mail address yields an error
        constraintTester.testConstraint("rubbish_email", true);

        // validate that a well-formed e-mail address does *not* yield an error
        constraintTester.testConstraint("avalidemail@hotmail.com", false);

        // validate that a null String does not yield an error
        constraintTester.testConstraint(null, false);
    }
    
    public void testInListConstraint() {
        // create a constraint tester for a domain class with a String property and an inList constraint with three possible values
        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.IN_LIST_CONSTRAINT, list);

        // validate that a value *not* in the list yields an error
        constraintTester.testConstraint("something", true);

        // validate that a value in the list does *not* yield an error
        constraintTester.testConstraint("two", false);

        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }

    public void testLengthConstraint() {
        // create a constraint tester for a domain class with a String property and a length constraint with a range of 5 to 15 characters
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.LENGTH_CONSTRAINT, new IntRange(5, 15));

        // validate that a value *less than* the minimum length yields an error
        constraintTester.testConstraint("tiny", true);

        // validate that a value *equal to* the minimum length does *not* yield an error
        constraintTester.testConstraint("12345", false);

        // validate that a value *between* the minimum and maximum lengths does *not* yield an error
        constraintTester.testConstraint("1234567890", false);
        
        // validate that a value *equal to* the maximum length does *not* yield an error
        constraintTester.testConstraint("123456789012345", false);
        
        // validate that a value *greater than* the minimum value does *not* yield an error
        constraintTester.testConstraint("absolutelytotallytoolong", true);

        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }
    
    public void testMatchesConstraint() {
        // create a constraint tester for a domain class with a String property and a matches (i.e., regex) constraint limiting the values to alpha-characters only
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.MATCHES_CONSTRAINT, "[a-zA-Z]");

        // validate that a value *not* matching the regex yields an error
        constraintTester.testConstraint("$", true);

        // validate that a value matching the regex does *not* yield an error
        constraintTester.testConstraint("j", false);
        
        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }
    
    public void testMinConstraint() {
        // create a constraint tester for a domain class with a Date property and a min constraint equal to the current date/time
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testDate", ConstrainedProperty.MIN_CONSTRAINT, NOW);

        // validate that a value *less than* the minimum value yields an error
        constraintTester.testConstraint(BEGINNING_OF_TIME, true);

        // validate that a value *equal to* the minimum value does *not* yield an error
        constraintTester.testConstraint(NOW, false);
        
        // validate that a value *greater than* the minimum value does *not* yield an error
        constraintTester.testConstraint(ONE_DAY_FROM_NOW, false);

        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }
    
    public void testMaxConstraint() {
        // create a constraint tester for a domain class with a Date property and a max constraint equal to the current date/time
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testDate", ConstrainedProperty.MAX_CONSTRAINT, NOW);

        // validate that a value *less than* the maximum value does *not* yield an error
        constraintTester.testConstraint(BEGINNING_OF_TIME, false);

        // validate that a value *equal to* the maximum value does *not* yield an error
        constraintTester.testConstraint(NOW, false);
        
        // validate that a value *greater than* the maximum value yields an error
        constraintTester.testConstraint(ONE_DAY_FROM_NOW, true);

        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }

    public void testMinSizeConstraintWithArrayProperty() {
        // create a constraint tester for a domain class with an array property and a minSize constraint equal to 2
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testArray", ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(2));

        // validate that an array *less than* the minimum size yields an error
        List list = new ArrayList();
        list.add("one");
        constraintTester.testConstraint(list.toArray(), true);

        // validate that a Collection *equal to* the minimum size does *not* yield an error
        list.add("two");
        constraintTester.testConstraint(list.toArray(), false);
        
        // validate that a Collection *greater than* the minimum size does *not* yield an error
        list.add("three");
        constraintTester.testConstraint(list.toArray(), false);
    }  
    
    public void testMinSizeConstraintWithCollectionProperty() { 
        // create a constraint tester for a domain class with a Collection property and a minSize constraint equal to 2
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testCollection", ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(2));

        // validate that a Collection *less than* the minimum size yields an error
        List list = new ArrayList();
        list.add("one");
        constraintTester.testConstraint(list, true);

        // validate that a Collection *equal to* the minimum size does *not* yield an error
        list.add("two");
        constraintTester.testConstraint(list, false);
        
        // validate that a Collection *greater than* the minimum size does *not* yield an error
        list.add("three");
        constraintTester.testConstraint(list, false);
    }  
    
    public void testMinSizeConstraintWithNumberProperty() {
        // create a constraint tester for a domain class with an Integer property and a minSize constraint equal to zero
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testInteger", ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(0));

        // validate that a value *less than* the minimum value yields an error
        constraintTester.testConstraint(new Integer(Integer.MIN_VALUE), true);

        // validate that a value *equal to* the minimum value does *not* yield an error
        constraintTester.testConstraint(new Integer(0), false);
        
        // validate that a value *greater than* the minimum value does *not* yield an error
        constraintTester.testConstraint(new Integer(Integer.MAX_VALUE), false);
    }    
    
    public void testMinSizeConstraintWithStringProperty() {
        // create a constraint tester for a domain class with a String property and a minSize constraint equal to 5
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(5));

        // validate that a String *less than* the minimum size yields an error
        constraintTester.testConstraint("tiny", true);

        // validate that a String *equal to* the minimum size does *not* yield an error
        constraintTester.testConstraint("12345", false);
        
        // validate that a String *greater than* the minimum size does *not* yield an error
        constraintTester.testConstraint("1234567890", false);
    }  

    public void testMaxSizeConstraintWithArrayProperty() {
        // create a constraint tester for a domain class with an array property and a maxSize constraint equal to 2
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testArray", ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(2));

        // validate that an array *less than* the maximum size does *not* yield an error
        List list = new ArrayList();
        list.add("one");
        constraintTester.testConstraint(list.toArray(), false);

        // validate that a Collection *equal to* the maximum size does *not* yield an error
        list.add("two");
        constraintTester.testConstraint(list.toArray(), false);
        
        // validate that a Collection *greater than* the maximum size yields an error
        list.add("three");
        constraintTester.testConstraint(list.toArray(), true);
    }  
    
    public void testMaxSizeConstraintWithCollectionProperty() { 
        // create a constraint tester for a domain class with a Collection property and a maxSize constraint equal to 2
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testCollection", ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(2));

        // validate that a Collection *less than* the maximum size does *not* yield an error
        List list = new ArrayList();
        list.add("one");
        constraintTester.testConstraint(list, false);

        // validate that a Collection *equal to* the maximum size does *not* yield an error
        list.add("two");
        constraintTester.testConstraint(list, false);
        
        // validate that a Collection *greater than* the maximum size yields an error
        list.add("three");
        constraintTester.testConstraint(list, true);
    }  
    
    public void testMaxSizeConstraintWithNumberProperty() {
        // create a constraint tester for a domain class with an Integer property and a maxSize constraint equal to zero
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testInteger", ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(0));

        // validate that a value *less than* the maximum value does *not* yield an error
        constraintTester.testConstraint(new Integer(Integer.MIN_VALUE), false);

        // validate that a value *equal to* the maximum value does *not* yield an error
        constraintTester.testConstraint(new Integer(0), false);
        
        // validate that a value *greater than* the maximum value yields an error
        constraintTester.testConstraint(new Integer(Integer.MAX_VALUE), true);
    } 

    public void testMaxSizeConstraintWithStringProperty() {
        // create a constraint tester for a domain class with a String property and a maxSize constraint equal to 5
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(5));

        // validate that a String *less than* the minimum size does *not* yield an error
        constraintTester.testConstraint("tiny", false);

        // validate that a String *equal to* the minimum size does *not* yield an error
        constraintTester.testConstraint("12345", false);
        
        // validate that a String *greater than* the minimum size yields an error
        constraintTester.testConstraint("1234567890", true);
    } 
    
    public void testNullableConstraint() {
        // create a constraint tester for a domain class with a String property and a non-nulable constraint
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.NULLABLE_CONSTRAINT, new Boolean(false));

        // validate that a null value yields an error
        constraintTester.testConstraint(null, true);

        // validate that a non-null value does *not* yield an error
        constraintTester.testConstraint("two", false);
        
        // validate that an empty String does *not* yield an error
        constraintTester.testConstraint("", false);
    }       

    public void testScaleConstraint() { 
        // test a Float value that should round down
        assertScaleConstraintForFloatValue(3, 0.1234f, 0.123f);

        // test a Float value that should round up
        assertScaleConstraintForFloatValue(3, 0.1235f, 0.124f);

        // test a Float value that should not change (i.e., should require no rounding)
        assertScaleConstraintForFloatValue(3, 0.12f, 0.120f);

        // test an integral value masquerading as a Float 
        assertScaleConstraintForFloatValue(3, 47f, 47.000f);        

        // test a scale of zero applied to a Float 
        assertScaleConstraintForFloatValue(0, 0.123f, 0f); 
        
        // test a Double value that should round down
        assertScaleConstraintForDoubleValue(3, 0.1234, 0.123);

        // test a Double value that should round up
        assertScaleConstraintForDoubleValue(3, 0.1235, 0.124);

        // test a Double value that should not change (i.e., should require no rounding)
        assertScaleConstraintForDoubleValue(3, 0.12, 0.120);

        // test an integral value masquerading as a Double 
        assertScaleConstraintForDoubleValue(3, 47d, 47.000);

        // test a scale of zero applied to a Double 
        assertScaleConstraintForDoubleValue(0, 0.123, 0d); 
        
        // test a BigDecimal value that should round down
        assertScaleConstraintForBigDecimalValue(3, "0.1234", "0.123");

        // test a BigDecimal value that should round up
        assertScaleConstraintForBigDecimalValue(3, "0.1235", "0.124");

        // test a BigDecimal value that should not change (i.e., should require no rounding)
        assertScaleConstraintForBigDecimalValue(3, "0.12", "0.120");

        // test an integral value masquerading as a BigDecimal 
        assertScaleConstraintForBigDecimalValue(3, "47", "47.000");      
        
        // test a scale of zero applied to a BigDecimal 
        assertScaleConstraintForBigDecimalValue(0, "0.123", "0");      
        
        // validate that a negative scale value yields an exception
        try {
            ConstrainedProperty cp = new ConstrainedProperty(TestClass.class, "testFloat", Float.class);
            cp.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(-1));
            fail("Failed to throw ConstraintException when given a negative scale value.");
        }
        catch(ConstraintException e) {
        }
    }     

    public void testScaleConstraintIgnoresNulls() {
        // create a constraint tester for a domain class with a Float property and a scale constraint
        TestClass testObject = new TestClass();
        ConstraintTester constraintTester = new ConstraintTester(testObject, "testFloat", ConstrainedProperty.SCALE_CONSTRAINT, new Integer(3));

        // apply the constraint (which should never generate errors)
        constraintTester.testConstraint(null, false);
        
        // validate the constraint yielded the expected value
        assertEquals(null, testObject.getTestFloat());
    }

    private void assertScaleConstraintForFloatValue(int scale, float originalValue, float expectedScaledValue) {
        // create a constraint tester for a domain class with a Float property and a scale constraint
        TestClass testObject = new TestClass();
        ConstraintTester constraintTester = new ConstraintTester(testObject, "testFloat", ConstrainedProperty.SCALE_CONSTRAINT, new Integer(scale));

        // apply the constraint (which should never generate errors)
        constraintTester.testConstraint(new Float(originalValue), false);
        
        // validate the constraint yielded the expected value
        assertEquals(expectedScaledValue, testObject.getTestFloat().floatValue(), 0f);
    }
    
    private void assertScaleConstraintForDoubleValue(int scale, double originalValue, double expectedScaledValue) {
        // create a constraint tester for a domain class with a Double property and a scale constraint
        TestClass testObject = new TestClass();
        ConstraintTester constraintTester = new ConstraintTester(testObject, "testDouble", ConstrainedProperty.SCALE_CONSTRAINT, new Integer(scale));

        // apply the constraint (which should never generate errors)
        constraintTester.testConstraint(new Double(originalValue), false);
        
        // validate the constraint yielded the expected value
        assertEquals(expectedScaledValue, testObject.getTestDouble().doubleValue(), 0d);
    }
    
    private void assertScaleConstraintForBigDecimalValue(int scale, String originalValue, String expectedScaledValue) {
        // create a constraint tester for a domain class with a BigDecimal property and a scale constraint
        TestClass testObject = new TestClass();
        ConstraintTester constraintTester = new ConstraintTester(testObject, "testBigDecimal", ConstrainedProperty.SCALE_CONSTRAINT, new Integer(scale));

        // apply the constraint (which should never generate errors)
        constraintTester.testConstraint(new BigDecimal(originalValue), false);
        
        // validate the constraint yielded the expected value
        assertEquals(new BigDecimal(expectedScaledValue), testObject.getTestBigDecimal());
    }
    
    public void testUrlConstraint() {
        // create a constraint tester for a domain class with a String property and a URL constraint
        ConstraintTester constraintTester = new ConstraintTester(new TestClass(), "testURL", ConstrainedProperty.URL_CONSTRAINT, new Boolean(true));

        // validate that an invalid URL value yields an error
        constraintTester.testConstraint("rubbish_url", true);

        // validate that a valid URL value does *not* yield an error
        constraintTester.testConstraint("http://www.google.com", false);
        
        // validate that a null does not yield an error
        constraintTester.testConstraint(null, false);
    }
    
    public void testValidatorConstraint() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", Integer.class);

        GroovyClassLoader cl = new GroovyClassLoader();
        // A trivial validator that allows only even numbers
        Class clazz = cl.parseClass("validate = { return (it & 1) == 0 ? true : 'evennumbervalidator.noteven' }");
        Script script = (Script)clazz.newInstance();
        script.run();
        Closure valClosure = (Closure)script.getBinding().getVariable("validate");

        cp.applyConstraint( ConstrainedProperty.VALIDATOR_CONSTRAINT, valClosure);

        Errors errors = new BindException(this,"testObject");

        validateConstraints( cp, new Integer(5), errors);
        assertTrue(errors.hasErrors());
        FieldError error = errors.getFieldError("testValidatorValue");
        assertNotNull(error);
        assertTrue(error.getCode().equals("constrainedPropertyTests.testValidatorValue.evennumbervalidator.noteven"));

        errors = new BindException(this,"testObject");

        validateConstraints( cp, new Integer( 4), errors);

        assertFalse(errors.hasErrors());
    }

    public void testValidatorConstraintTwoUntypedParameters() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", int.class);

        GroovyClassLoader cl = new GroovyClassLoader();

        Class clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { val, obj -> \n" +
                "assert (val == 5);\n" +
                "assert (obj instanceof ConstrainedPropertyTests)\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));
    }

    public void testValidatorConstraintTwoTypedParameters() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", int.class);

        GroovyClassLoader cl = new GroovyClassLoader();

        Class clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { Object val, Object obj -> \n" +
                "assert (val == 5);\n" +
                "assert (obj instanceof ConstrainedPropertyTests)\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));

        clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "import junit.framework.TestCase;\n" +
                "validate = { int val, TestCase obj -> \n" +
                "assert (val == 5);\n" +
                "assert (obj instanceof ConstrainedPropertyTests)\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));

        clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "import junit.framework.TestCase;\n" +
                "validate = { Number val, TestCase obj -> \n" +
                "assert (val == 5);\n" +
                "assert (obj instanceof ConstrainedPropertyTests)\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));

        clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { Integer val, ConstrainedPropertyTests obj -> \n" +
                "assert (val == 5);\n" +
                "assert (obj instanceof ConstrainedPropertyTests)\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));
    }

    public void testValidatorConstraintOneUntypedParameter() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", int.class);

        GroovyClassLoader cl = new GroovyClassLoader();

        Class clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { val -> \n" +
                "assert (val == 5);\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));
    }

    public void testValidatorConstraintOneTypedParameter() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", int.class);

        GroovyClassLoader cl = new GroovyClassLoader();

        Class clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { Object val -> \n" +
                "assert (val == 5);\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));

        clazz = cl.parseClass("import org.codehaus.groovy.grails.validation.ConstrainedPropertyTests;\n" +
                "validate = { Integer val -> \n" +
                "assert (val == 5);\n" +
                "}");
        doValidatorTestAssertNoErrors(clazz, cp, new Integer(5));
    }

    private void doValidatorTestAssertNoErrors(Class clazz, ConstrainedProperty cp, Object value)
            throws InstantiationException, IllegalAccessException
    {
        Script script = (Script)clazz.newInstance();
        script.run();
        Closure valClosure = (Closure)script.getBinding().getVariable("validate");

        cp.applyConstraint( ConstrainedProperty.VALIDATOR_CONSTRAINT, valClosure);

        Errors errors = new BindException(this,"testObject");

        validateConstraints( cp, value, errors);
        assertFalse(errors.hasErrors());
    }

    public void testValidatorConstraintListReturnType() throws Exception
    {
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testValidatorValue", int.class);

        GroovyClassLoader cl = new GroovyClassLoader();
        // A trivial validator that allows only even numbers
        Class clazz = cl.parseClass("validate = { return (it & 1) == 0 ? true : [ 'evennumbervalidator.noteven', 2, 4, 6] }");
        Script script = (Script)clazz.newInstance();
        script.run();
        Closure valClosure = (Closure)script.getBinding().getVariable("validate");

        cp.applyConstraint( ConstrainedProperty.VALIDATOR_CONSTRAINT, valClosure);

        Errors errors = new BindException(this,"testObject");

        validateConstraints( cp, new Integer(5), errors);
        assertTrue(errors.hasErrors());
        FieldError error = errors.getFieldError("testValidatorValue");
        assertNotNull(error);
        assertTrue(error.getCode().equals("constrainedPropertyTests.testValidatorValue.evennumbervalidator.noteven"));
        System.out.println( error.getArguments().length );
        assertTrue(error.getArguments().length == 6);

        errors = new BindException(this,"testObject");

        validateConstraints( cp, new Integer( 4), errors);

        assertFalse(errors.hasErrors());
    }

    private void validateConstraints(ConstrainedProperty cp, Object value, Errors errors)
    {
        Constraint c = null;
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, value, errors);
        }
    }

    public void testConstraintBuilder() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class groovyClass = gcl.parseClass( "class TestClass {\n" +
                        "Long id\n" +
                        "Long version\n" +
                        "String login\n" +
                        "String email\n" +
                        "static constraints = {\n" +
                            "login(length:5..15,nullable:false,blank:false)\n" +
                            "email(email:true)\n" +
                        "}\n" +
                        "}" );

        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(groovyClass);

        Map constrainedProperties = domainClass.getConstrainedProperties();
        assertTrue(constrainedProperties.size() == 2);
        ConstrainedProperty loginConstraint = (ConstrainedProperty)constrainedProperties.get("login");
        assertTrue(loginConstraint.getAppliedConstraints().size() == 3);

        ConstrainedProperty emailConstraint = (ConstrainedProperty)constrainedProperties.get("email");
        assertEquals(2,emailConstraint.getAppliedConstraints().size());

        GroovyObject go = (GroovyObject)groovyClass.newInstance();
        go.setProperty("email", "rubbish_email");
        Errors errors = new BindException(go, "TestClass");
        emailConstraint.validate(go, go.getProperty("email"), errors );

        assertTrue(errors.hasErrors());
        go.setProperty("email", "valid@email.com");
        errors = new BindException(go, "TestClass");
        emailConstraint.validate(go, go.getProperty("email"), errors );
        assertFalse(errors.hasErrors());
    }
    
    /**
     * Utility class used to test the various constraints.
     */
    private class ConstraintTester {
        
        private BeanWrapper constrainedBean;
        private String constrainedPropertyName;
        private Constraint constraint;
        private ConstrainedProperty constrainedProperty;

        /**
         * Creates and initializes a <code>ConstraintTester</code> object.  Once initilized, you can call <code>testConstraint</code> with
         * various test values for the constrained property to validate the constraint behavior.
         * 
         * @param constrainedObject the object to which the constraint will be applied
         * @param constrainedPropertyName the name of the property (on the object) to which the constraint will be applied
         * @param constraintName the name of the constraint to apply (specified using the constants defined in org.codehaus.groovy.grails.validation.ConstrainedProperty)
         * @param constrainingValue the value of the constraint (e.g., a min value, a regex, etc.)
         */
        public ConstraintTester(Object constrainedObject, String constrainedPropertyName, String constraintName, Object constrainingValue) {
            this.constrainedBean = new BeanWrapperImpl(constrainedObject);

            this.constrainedPropertyName = constrainedPropertyName;

            constrainedProperty = new ConstrainedProperty(constrainedObject.getClass(), constrainedPropertyName, this.constrainedBean.getPropertyType(constrainedPropertyName));
            constrainedProperty.applyConstraint(constraintName, constrainingValue);
            this.constraint = (Constraint) constrainedProperty.getAppliedConstraints().iterator().next();
        }
        
        /**
         * Tests the constraint using the specified property value and asserts the expected results.
         * 
         * @param testValue the property value to use for the test
         * @param isErrorExpected indicates whether the property value is expected to violate the constraint
         */
        public void testConstraint(Object testValue, boolean isErrorExpected) {
            // initialize the property with the test value
            this.constrainedBean.setPropertyValue(this.constrainedPropertyName, testValue);
            
            // run the validation
            Errors errors = new BindException(this.constrainedBean.getWrappedInstance(), "testObject");
            this.constraint.validate(this.constrainedBean.getWrappedInstance(), this.constrainedBean.getPropertyValue(this.constrainedPropertyName), errors); 
            
            // validate that we obtained the expected results
            assertEquals(isErrorExpected, errors.hasErrors());
            if (isErrorExpected) {
                FieldError error = errors.getFieldError(this.constrainedPropertyName);
                assertNotNull(error);              
            }
        }        

    }
    
    /**
     * Simple bean whose instances serve as test objects for the various constraint tests.
     */
    private class TestClass {
        private Object[] testArray;
        private BigDecimal testBigDecimal;
        private Collection testCollection;
        private Date testDate;
        private Double testDouble;
        private String testEmail;
        private Float testFloat;
        private Integer testInteger;
        private String testURL;

        public Object[] getTestArray() {
            return testArray;
        }

        public void setTestArray(Object[] testArray) {
            this.testArray = testArray;
        }     

        public BigDecimal getTestBigDecimal() {
            return testBigDecimal;
        }

        public void setTestBigDecimal(BigDecimal testBigDecimal) {
            this.testBigDecimal = testBigDecimal;
        }
        
        public Collection getTestCollection() {
            return testCollection;
        }

        public void setTestCollection(Collection testCollection) {
            this.testCollection = testCollection;
        }   
        
        public Date getTestDate() {
            return testDate;
        }

        public void setTestDate(Date testDate) {
            this.testDate = testDate;
        }

        public Double getTestDouble() {
            return testDouble;
        }

        public void setTestDouble(Double testDouble) {
            this.testDouble = testDouble;
        }
        
        public String getTestEmail() {
            return testEmail;
        }

        public void setTestEmail(String testEmail) {
            this.testEmail = testEmail;
        }

        public Float getTestFloat() {
            return testFloat;
        }

        public void setTestFloat(Float testFloat) {
            this.testFloat = testFloat;
        }
        
        public Integer getTestInteger() {
            return testInteger;
        }

        public void setTestInteger(Integer testInteger) {
            this.testInteger = testInteger;
        }
        
        public String getTestURL() {
            return testURL;
        }

        public void setTestURL(String testURL) {
            this.testURL = testURL;
        }
    }    
}

