package org.codehaus.groovy.grails.validation;

import junit.framework.TestCase;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.Errors;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

/**
 * Abstract class for all constraint tests.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public abstract class AbstractConstraintTests extends TestCase {

    protected Constraint getConstraint( String field, Object parameter ) {
        Constraint constraint = null;
        try {
            constraint = ( Constraint ) getConstraintClass().newInstance();
        } catch( Exception e ) {
            fail( "Cannot instantiate constraint class [" + getConstraintClass().getName() + "]" );
        }
        constraint.setOwningClass( TestClass.class );
        constraint.setPropertyName( field );
        constraint.setParameter( parameter );
        return constraint;
    }

    protected void testConstraintDefaultMessage( Constraint constraint, Object value, String message ) {
        Errors errors = testConstraintFailed( constraint, value );
        assertEquals( message, errors.getFieldError( constraint.getPropertyName() ).getDefaultMessage() );
    }

    protected void testConstraintMessageCode( Constraint constraint, Object value, String code ) {
        Errors errors = testConstraintFailed( constraint, value );
        checkCode( errors.getFieldError( constraint.getPropertyName()), code );
    }

    protected void testConstraintMessageCode( Constraint constraint, Object value, String code, Object[] args ) {
        Errors errors = testConstraintFailed( constraint, value );
        FieldError fieldError = errors.getFieldError( constraint.getPropertyName() );
        checkCode( fieldError, code );
        checkArguments( args, fieldError.getArguments() );
    }

    protected void testConstraintMessageCodes( Constraint constraint, Object value, String[] code, Object[] args ) {
        Errors errors = testConstraintFailed( constraint, value );
        FieldError fieldError = errors.getFieldError( constraint.getPropertyName() );
        for( int j = 0; j < code.length; j++ ) {
            checkCode( fieldError, code[j] );
        }
        checkArguments( args, fieldError.getArguments() );
    }

    protected Errors testConstraintFailed(Constraint constraint, Object value) {
        Errors errors = validateConstraint(constraint, value);
        assertEquals(true, errors.hasErrors());
        return errors;
    }

    protected Errors testConstraintFailedAndVetoed(Constraint constraint, Object value) {
        Errors errors = validateConstraint(constraint, value, Boolean.TRUE);
        assertEquals(true, errors.hasErrors());
        return errors;
    }

    protected void testConstraintPassed(Constraint constraint, Object value) {
        Errors errors = validateConstraint(constraint, value);
        assertEquals(false, errors.hasErrors());
    }

    protected void testConstraintPassedAndVetoed(Constraint constraint, Object value) {
        Errors errors = validateConstraint(constraint, value, Boolean.TRUE);
        assertEquals(false, errors.hasErrors());
    }

    protected Errors validateConstraint(Constraint constraint, Object value) {
        return validateConstraint(constraint, value, null);
    }

    protected Errors validateConstraint(Constraint constraint, Object value, Boolean shouldVeto) {
        BeanWrapper constrainedBean = new BeanWrapperImpl(new TestClass());
        constrainedBean.setPropertyValue(constraint.getPropertyName(), value);
        Errors errors = new BindException(constrainedBean.getWrappedInstance(), constrainedBean.getWrappedClass().getName());
        if(!(constraint instanceof VetoingConstraint) || shouldVeto == null) {
            constraint.validate(constrainedBean.getWrappedInstance(), value, errors);
        } else {
            boolean vetoed = ((VetoingConstraint) constraint).validateWithVetoing(constrainedBean.getWrappedInstance(), value, errors);
            if(shouldVeto.booleanValue() && !vetoed) fail("Constraint should veto");
            else if(!shouldVeto.booleanValue() && vetoed) fail("Constraint shouldn't veto");
        }
        return errors;
    }

    private void checkCode( FieldError error, String code ) {
        String[] codes = error.getCodes();
        boolean result = false;
        for( int i = 0; i < codes.length; i++ ) {
            if( code.equals( codes[i] ) ) {
                result = true;
                break;
            }
        }
        assertTrue( "Code " + code + " is not found in error", result );
    }

    private void checkArguments( Object[] left, Object[] right ) {
        assertEquals( left.length, right.length );
        for( int i = 0; i < left.length; i++ ) {
            assertEquals( left[i], right[i] );
        }
    }

    protected abstract Class getConstraintClass();
}
