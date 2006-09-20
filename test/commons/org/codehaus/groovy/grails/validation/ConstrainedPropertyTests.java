package org.codehaus.groovy.grails.validation;

import groovy.lang.*;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.util.*;

public class ConstrainedPropertyTests extends TestCase {

    private Date testDate = new Date();
    private String testEmail = "rubbish_email";
    private String testURL = "rubbish_url";
    private int testValidatorValue = 0;

    public int getTestValidatorValue()
    {
        return testValidatorValue;
    }

    public void setTestValidatorValue(int testValidatorValue)
    {
        this.testValidatorValue = testValidatorValue;
    }

    /**
     * @return Returns the testDate.
     */
    public Date getTestDate() {
        return testDate;
    }

    /**
     * @param testDate The testDate to set.
     */
    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }

    /**
     * @return Returns the testEmail.
     */
    public String getTestEmail() {
        return testEmail;
    }

    /**
     * @param testEmail The testEmail to set.
     */
    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    /**
     * @return Returns the testURL.
     */
    public String getTestURL() {
        return testURL;
    }

    /**
     * @param testURL The testURL to set.
     */
    public void setTestURL(String testURL) {
        this.testURL = testURL;
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
        assertFalse(cp.supportsContraint( ConstrainedProperty.URL_CONSTRAINT ));

    }

    /*
     * Test method for 'org.codehaus.groovy.grails.validation.ConstrainedProperty.applyConstraint(String, Object)'
     */
    public void testApplyConstraint() {

        // test validate email
        ConstrainedProperty cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testEmail", String.class);

        cp.applyConstraint( ConstrainedProperty.EMAIL_CONSTRAINT, new Boolean(true) );
        assertTrue(cp.getAppliedConstraints().size() == 1);
        Errors errors = new BindException(this,"testObject");
        Constraint c = null;
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testEmail, errors);
        }
        assertTrue(errors.hasErrors());
        FieldError error = errors.getFieldError("testEmail");
        assertNotNull(error);
        assertEquals("rubbish_email",error.getRejectedValue());

        this.testEmail = "avalidemail@hotmail.com";
        errors = new BindException(this,"testObject");
        c.validate(this, this.testEmail,errors);
        assertFalse(errors.hasErrors());

        // test validate url
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testURL", String.class);
        cp.applyConstraint( ConstrainedProperty.URL_CONSTRAINT, new Boolean(true) );

        assertTrue(cp.getAppliedConstraints().size() == 1);
        errors = new BindException(this,"testObject");

        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testURL, errors);
        }
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testURL");
        assertNotNull(error);
        assertEquals(this.testURL,error.getRejectedValue());

        this.testURL = "http://www.google.com";
        errors = new BindException(this,"testObject");
        c.validate(this, this.testURL,errors);
        assertFalse(errors.hasErrors());

        // test blank constraint
        cp.applyConstraint( ConstrainedProperty.URL_CONSTRAINT, null );
        cp.applyConstraint( ConstrainedProperty.BLANK_CONSTRAINT, new Boolean(false) );

        assertTrue(cp.getAppliedConstraints().size() == 1);
        errors = new BindException(this,"testObject");
        this.testURL = "";
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testURL, errors);
        }
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testURL");
        System.out.println(error);
        assertNotNull(error);

        // test nullable constraint
        cp.applyConstraint( ConstrainedProperty.BLANK_CONSTRAINT, new Boolean(true) );
        cp.applyConstraint( ConstrainedProperty.NULLABLE_CONSTRAINT, new Boolean(false) );

        errors = new BindException(this,"testObject");
        this.testURL = null;
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testURL, errors);
        }
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testURL");
        System.out.println(error);
        assertNotNull(error);

        // test inList constraint
        cp.applyConstraint( ConstrainedProperty.NULLABLE_CONSTRAINT, new Boolean(true) );
        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");
        this.testURL = "something";
        cp.applyConstraint( ConstrainedProperty.IN_LIST_CONSTRAINT, list );

        errors = new BindException(this,"testObject");
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testURL, errors);
        }
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testURL");
        System.out.println(error);
        assertNotNull(error);
		// Validate that the error includes the correct error code
		assertTrue(Arrays.asList(error.getCodes()).contains("constrainedPropertyTests.testURL.not.inList"));
		
        this.testURL = "two";
        errors = new BindException(this,"testObject");
        c.validate(this, this.testURL,errors);
        assertFalse(errors.hasErrors());

        // test length constraint
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testURL", String.class);
        cp.applyConstraint( ConstrainedProperty.LENGTH_CONSTRAINT, new IntRange(5,15) );

        errors = new BindException(this,"testObject");
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testURL, errors);
        }

        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testURL");
        System.out.println(error);
        assertNotNull(error);

        this.testURL = "absolutelytotallytoolong";
        errors = new BindException(this,"testObject");
        c.validate(this, this.testURL,errors);
        assertTrue(errors.hasErrors());

        // test min constraint
        // ---------------------------------------------------------------------
        final Date BEGINNING_OF_TIME = new Date(0);
        final Date NOW = new Date();
        final Date ONE_DAY_FROM_NOW = new Date(System.currentTimeMillis() + 86400000);

        this.testDate = BEGINNING_OF_TIME;
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testDate", String.class);
        cp.applyConstraint( ConstrainedProperty.MIN_CONSTRAINT, NOW);

        errors = new BindException(this,"testObject");
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testDate, errors);
        }

        // validate that a value *less than* the minimum value yields an error
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testDate");
        System.out.println(error);
        assertNotNull(error);

        // validate that a value *equal to* the minimum value does *not* yield an error
        this.testDate = NOW;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertFalse(errors.hasErrors());

        // validate that a value *greater than* the minimum value does *not* yield an error
        this.testDate = ONE_DAY_FROM_NOW;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertFalse(errors.hasErrors());

        // validate that a null value yields an error
        this.testDate = null;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testDate");
        System.out.println(error);
        assertNotNull(error);

        // test max constraint
        // ---------------------------------------------------------------------
        this.testDate = BEGINNING_OF_TIME;
        cp = new ConstrainedProperty(ConstrainedPropertyTests.class,"testDate", String.class);
        cp.applyConstraint( ConstrainedProperty.MAX_CONSTRAINT, NOW);

        errors = new BindException(this,"testObject");
        for (Iterator i = cp.getAppliedConstraints().iterator(); i.hasNext();) {
            c = (Constraint) i.next();
            c.validate(this, this.testDate, errors);
        }

        // validate that a value *less than* the maximum value does *not* yield an error
        assertFalse(errors.hasErrors());

        // validate that a value *equal to* the maximum value does *not* yield an error
        this.testDate = NOW;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertFalse(errors.hasErrors());

        // validate that a value *greater than* the maximum value yields an error
        this.testDate = ONE_DAY_FROM_NOW;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testDate");
        System.out.println(error);
        assertNotNull(error);

        // validate that a null value yields an error
        this.testDate = null;
        errors = new BindException(this,"testObject");
        c.validate(this, this.testDate,errors);
        assertTrue(errors.hasErrors());
        error = errors.getFieldError("testDate");
        System.out.println(error);
        assertNotNull(error);
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
                        "@Property Long id\n" +
                        "@Property Long version\n" +
                        "@Property String login\n" +
                        "@Property String email\n" +
                        "@Property constraints = {\n" +
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
        assertTrue(emailConstraint.getAppliedConstraints().size() == 1);

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

