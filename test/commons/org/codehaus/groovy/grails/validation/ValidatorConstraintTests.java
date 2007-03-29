package org.codehaus.groovy.grails.validation;

import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import junit.framework.TestCase;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Test cases for custom 'validator' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class ValidatorConstraintTests extends TestCase {
    private static final String PROP_NAME = "firstName";
    private GroovyShell shell = new GroovyShell();

    public void testBooleanReturn() {
        Errors errors = proccedValidation( "{val,obj -> return false}" );
        assertTrue( errors.hasErrors() );
        FieldError error = errors.getFieldError( PROP_NAME );
        System.out.println( error );
        assertTrue( checkErrorCode( error, "testClass.firstName.validator.invalid" ) );
        Object[] arguments = error.getArguments();
        assertEquals( 3, arguments.length );
        assertTrue( arguments[0] instanceof String );
        assertEquals( PROP_NAME, arguments[0] );
        assertTrue( arguments[1] instanceof Class );
        assertEquals( TestClass.class, arguments[1] );
        assertTrue( arguments[2] instanceof String );
        assertEquals( "John", arguments[2] );
        errors = proccedValidation( "{val,obj -> return true}" );
        assertFalse( errors.hasErrors() );
    }

    public void testStringReturn() {
        Errors errors = proccedValidation( "{val,obj -> return 'test.message'}" );
        assertTrue( errors.hasErrors() );
        FieldError error = errors.getFieldError( PROP_NAME );
        System.out.println( error );
        assertTrue( checkErrorCode( error, "testClass.firstName.test.message" ) );
        Object[] arguments = error.getArguments();
        assertEquals( 3, arguments.length );
        assertTrue( arguments[0] instanceof String );
        assertEquals( PROP_NAME, arguments[0] );
        assertTrue( arguments[1] instanceof Class );
        assertEquals( TestClass.class, arguments[1] );
        assertTrue( arguments[2] instanceof String );
        assertEquals( "John", arguments[2] );
        try {
            proccedValidation( "{val, obj -> return 123L}" );
            fail("Validator constraint must throw an exception about wrong closure return");
        } catch( IllegalArgumentException iae ) {
            // Greate
        }
    }

    public void testListReturn() {
        Errors errors = proccedValidation( "{val,obj -> return ['test.message', 'arg', 123L]}" );
        assertTrue( errors.hasErrors() );
        FieldError error = errors.getFieldError( PROP_NAME );
        System.out.println( error );
        assertTrue( checkErrorCode( error, "testClass.firstName.test.message" ) );
        Object[] arguments = error.getArguments();
        assertEquals( 5, arguments.length );
        assertTrue( arguments[0] instanceof String );
        assertEquals( PROP_NAME, arguments[0] );
        assertTrue( arguments[1] instanceof Class );
        assertEquals( TestClass.class, arguments[1] );
        assertTrue( arguments[2] instanceof String );
        assertEquals( "John", arguments[2] );
        assertTrue( arguments[3] instanceof String );
        assertEquals( "arg", arguments[3] );
        assertTrue( arguments[4] instanceof Long );
        assertEquals( new Long(123), arguments[4] );
        try {
            proccedValidation( "{val, obj -> return [123L,'arg1','arg2']}" );
            fail("Validator constraint must throw an exception about wrong closure return");
        } catch( IllegalArgumentException iae ) {
            // Greate
        }
    }

    public void testConstraintCreation() {
        Constraint validatorConstraint = new ValidatorConstraint();
        assertEquals( ConstrainedProperty.VALIDATOR_CONSTRAINT, validatorConstraint.getName() );
        assertTrue( validatorConstraint.supports( TestClass.class ));
        assertFalse( validatorConstraint.supports( null ));

        validatorConstraint.setOwningClass( TestClass.class );
        validatorConstraint.setPropertyName( PROP_NAME );

        try {
            validatorConstraint.setParameter( "Test");
            fail("ValidatorConstraint must throw an exception for non-closure parameter.");
        } catch ( IllegalArgumentException iae ) {
            // Great since validator constraint only applicable for Closure parameter
        }

        Closure validator = (Closure) shell.evaluate( "{ param1, param2, param3 -> return true}" );
        try {
            validatorConstraint.setParameter( validator );
            fail("ValidatorConstraint must throw exception about closure with more that 2 params");
        } catch ( IllegalArgumentException iae ) {
            // Great since validator constraint only applicable for Closure's with 1 or 2 params
        }

        validator = (Closure) shell.evaluate( "{ Long param1, param2 -> return true}" );
        try {
            validatorConstraint.setParameter( validator );
            fail("ValidatorConstraint must throw exception about wrong closure param type");
        } catch ( IllegalArgumentException iae ) {
            // Great
        }

        validator = (Closure) shell.evaluate( "{ param1, String param2 -> return true}" );
        try {
            validatorConstraint.setParameter( validator );
            fail("ValidatorConstraint must throw exception about wrong closure param type");
        } catch ( IllegalArgumentException iae ) {
            // Great
        }
    }

    private boolean checkErrorCode( FieldError error, String code ) {
        String[] codes = error.getCodes();
        for( int i = 0; i < codes.length; i++ ) {
            if( code.equals( codes[i] ) ) return true;
        }
        return false;
    }

    private Errors proccedValidation( String closure ) {
        TestClass test = new TestClass( "John" );
        Errors errors = new BindException( test, test.getClass().getName() );
        Constraint validatorConstraint = getValidatorConstraint( closure );
        validatorConstraint.validate( new TestClass( "John" ), "John", errors);
        return errors;
    }

    private Constraint getValidatorConstraint( String closure ) {
        Constraint validatorConstraint = new ValidatorConstraint();
        Closure validator = (Closure) shell.evaluate( closure );
        validatorConstraint.setOwningClass( TestClass.class );
        validatorConstraint.setPropertyName( PROP_NAME );
        validatorConstraint.setParameter( validator );
        return validatorConstraint;
    }

}

class TestClass {
    String firstName;

    public TestClass( ) {}

    public TestClass( String firstName ) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName( String firstName ) {
        this.firstName = firstName;
    }
}
