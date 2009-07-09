package org.codehaus.groovy.grails.validation;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.Errors;
import org.springframework.validation.BindException;

import java.math.BigDecimal;

/**
 * Test cases for 'scale' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class ScaleConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return ScaleConstraint.class;
    }

    public void testValidation() {
        testFloat(3, 0.1234f, 0.123f);

        // test a Float value that should round up
        testFloat(3, 0.1235f, 0.124f);

        // test a Float value that should not change (i.e., should require no rounding)
        testFloat(3, 0.12f, 0.120f);

        // test an integral value masquerading as a Float
        testFloat(3, 47f, 47.000f);

        // test a scale of zero applied to a Float
        testFloat(0, 0.123f, 0f);

        // test a Double value that should round down
        testDouble(3, 0.1234, 0.123);

        // test a Double value that should round up
        testDouble(3, 0.1235, 0.124);

        // test a Double value that should not change (i.e., should require no rounding)
        testDouble(3, 0.12, 0.120);

        // test an integral value masquerading as a Double
        testDouble(3, 47d, 47.000);

        // test a scale of zero applied to a Double
        testDouble(0, 0.123, 0d);

        // test a BigDecimal value that should round down
        testBigDecimal(3, "0.1234", "0.123");

        // test a BigDecimal value that should round up
        testBigDecimal(3, "0.1235", "0.124");

        // test a BigDecimal value that should not change (i.e., should require no rounding)
        testBigDecimal(3, "0.12", "0.120");

        // test an integral value masquerading as a BigDecimal
        testBigDecimal(3, "47", "47.000");

        // test a scale of zero applied to a BigDecimal
        testBigDecimal(0, "0.123", "0");      

    }

    public void testNullPasses() {
        Constraint constraint = getConstraint( "testBigDecimal", new Integer( 2 ));
        assertEquals( null, proceedValidation( constraint, null));
    }

    public void testValidationOnInvalidField() {
        Constraint constraint = getConstraint( "testString", new Integer( 2 ));
        try {
            proceedValidation( constraint, "123");
            fail("ScaleConstraint must throw an exception when applied to field with unsupported type");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }


    public void testCreation() {
        ScaleConstraint constraint = (ScaleConstraint) getConstraint( "testFloat", new Integer(2) );
        assertEquals( ConstrainedProperty.SCALE_CONSTRAINT, constraint.getName() );
        assertTrue( constraint.supports( BigDecimal.class ));
        assertTrue( constraint.supports( Float.class ));
        assertTrue( constraint.supports( Double.class ));
        assertFalse( constraint.supports( String.class ));
        assertFalse( constraint.supports( Object.class ));
        assertFalse( constraint.supports( null ));

        assertEquals( 2, constraint.getScale() );

        try {
            getConstraint( "testFloat", "wrong");
            fail("EmailConstraint must throw an exception for non-integer parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }

        try {
            getConstraint( "testFloat", new Integer(-1));
            fail("EmailConstraint must throw an exception for negative parameters.");
        } catch( IllegalArgumentException iae ) {
            // Great
        }
    }

    private void testFloat( int scale, float value, float result ) {
        Constraint constraint = getConstraint( "testFloat", new Integer( scale ));
        assertEquals( new Float(result), proceedValidation( constraint, new Float(value) ));
    }

    private void testDouble( int scale, double value, double result ) {
        Constraint constraint = getConstraint( "testDouble", new Integer( scale ));
        assertEquals( new Double(result), proceedValidation( constraint, new Double(value) ));
    }
    private void testBigDecimal( int scale, String value, String result ) {
        Constraint constraint = getConstraint( "testBigDecimal", new Integer( scale ));
        assertEquals( new BigDecimal( result ), proceedValidation( constraint, new BigDecimal( value )));
    }

    private Object proceedValidation( Constraint constraint, Object value ) {
        BeanWrapper constrainedBean = new BeanWrapperImpl( new TestClass() );
        constrainedBean.setPropertyValue( constraint.getPropertyName(), value );
        Errors errors = new BindException( constrainedBean.getWrappedInstance(), constrainedBean.getWrappedClass().getName() );
        assertFalse( errors.hasErrors() );
        constraint.validate( constrainedBean.getWrappedInstance(), value, errors );
        return constrainedBean.getPropertyValue( constraint.getPropertyName() );
    }
}
