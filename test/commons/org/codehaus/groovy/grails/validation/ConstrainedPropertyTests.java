package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.IntRange;
import groovy.lang.ObjectRange;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class ConstrainedPropertyTests extends TestCase {

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
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_SIZE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.SIZE_CONSTRAINT ));

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

        assertTrue(cp.supportsContraint( ConstrainedProperty.IN_LIST_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NOT_EQUAL_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.NULLABLE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MAX_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.MIN_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.RANGE_CONSTRAINT ));
        assertTrue(cp.supportsContraint( ConstrainedProperty.VALIDATOR_CONSTRAINT ));

        assertFalse(cp.supportsContraint( ConstrainedProperty.MAX_SIZE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.MIN_SIZE_CONSTRAINT ));
        assertFalse(cp.supportsContraint( ConstrainedProperty.SIZE_CONSTRAINT ));
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
        ConstrainedProperty cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        assertNull(cp.getMin());

        // validate that getMin returns the correct value when the min constraint is defined for the property (but no range constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new Double(123.45));
        assertEquals(new Double(123.45), cp.getMin());
        
        // validate that getMin returns the correct value when the range constraint is defined for the property (but no min constraint is defined)
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(123.45), new Double(678.90)));
        assertEquals(new Double(123.45), cp.getMin());
        
        // validate that getMin returns the maximum of the min constraint and the lower bound of the range constraint
        //   1) validate where the lower bound of the range constraint is greater than the min constraint
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new Double(1.23));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(4.56), new Double(7.89)));
        assertEquals(new Double(4.56), cp.getMin());

        //   2) validate where the min constraint is greater than the lower bound of the range constraint
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
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
        ConstrainedProperty cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        assertNull(cp.getMax());

        // validate that getMax returns the correct value when the max constraint is defined for the property (but no range constraint is defined)
        cp.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new Double(123.45));
        assertEquals(new Double(123.45), cp.getMax());
        
        // validate that getMax returns the correct value when the range constraint is defined for the property (but no max constraint is defined)
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(123.45), new Double(678.90)));
        assertEquals(new Double(678.90), cp.getMax());
        
        // validate that getMax returns the minimum of the max constraint and the upper bound of the range constraint
        //   1) validate where the upper bound of the range constraint is less than the max constraint
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
        cp.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new Double(7.89));
        cp.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new Double(1.23), new Double(4.56)));
        assertEquals(new Double(4.56), cp.getMax());

        //   2) validate where the max constraint is less than the upper bound of the range constraint
        cp = new ConstrainedProperty(TestClass.class, "testDouble", Double.class);
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
    
    public void testConstraintBuilder() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class groovyClass = gcl.parseClass( "class TestClass {\n" +
                        "Long id\n" +
                        "Long version\n" +
                        "String login\n" +
                        "String other\n" +
                        "String email\n" +
                        "static constraints = {\n" +
                            "login(size:5..15,nullable:false,blank:false)\n" +
                            "other(blank:false,size:5..15,nullable:false)\n" +
                            "email(email:true)\n" +
                        "}\n" +
                        "}" );

        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(groovyClass);

        Map constrainedProperties = domainClass.getConstrainedProperties();
        assertTrue(constrainedProperties.size() == 3);
        ConstrainedProperty loginConstraint = (ConstrainedProperty)constrainedProperties.get("login");
        Collection appliedConstraints = loginConstraint.getAppliedConstraints();
        assertTrue(appliedConstraints.size() == 3);

        // Check the order of the constraints for the 'login' property...
        int index = 0;
        String[] constraintNames = new String[] { "size", "nullable", "blank" };
        for (Iterator iter = appliedConstraints.iterator(); iter.hasNext();) {
            Constraint c = (Constraint) iter.next();
            assertEquals(constraintNames[index], c.getName());
            index++;
        }

        // ...and for the 'other' property.
        appliedConstraints = ((ConstrainedProperty) constrainedProperties.get("other")).getAppliedConstraints();
        index = 0;
        constraintNames = new String[] { "blank", "size", "nullable" };
        for (Iterator iter = appliedConstraints.iterator(); iter.hasNext();) {
            Constraint c = (Constraint) iter.next();
            assertEquals(constraintNames[index], c.getName());
            index++;
        }
        
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
    
}

