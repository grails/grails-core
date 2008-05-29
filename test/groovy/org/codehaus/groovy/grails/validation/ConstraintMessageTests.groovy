package org.codehaus.groovy.grails.validation

import junit.framework.TestCase
import org.springframework.validation.Errors
import org.springframework.validation.BeanPropertyBindingResult

class ConstraintMessageTests extends GroovyTestCase {

    def testProperty

    public void testMessageCodeOrder() {
        Constraint c = new TestConstraint()
        c.setOwningClass(this.class)
        c.setPropertyName("testProperty")
        def errors = new BeanPropertyBindingResult(this, "TestObjectName");
        String[] codes = ['test']
        Object[] values = []
        c.rejectValueWithDefaultMessage(this, errors, 'default.message', codes, values);
        assertArraysEqual([
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.TestObjectName.testProperty',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.testProperty',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.java.lang.Object',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error',                   
                'constraintMessageTests.testProperty.TestConstraint.error.TestObjectName.testProperty',
                'constraintMessageTests.testProperty.TestConstraint.error.testProperty',
                'constraintMessageTests.testProperty.TestConstraint.error.java.lang.Object',
                'constraintMessageTests.testProperty.TestConstraint.error',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.test.TestObjectName.testProperty',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.test.testProperty',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.test.java.lang.Object',
                'org.codehaus.groovy.grails.validation.ConstraintMessageTests.testProperty.test',
                'constraintMessageTests.testProperty.test.TestObjectName.testProperty',
                'constraintMessageTests.testProperty.test.testProperty',
                'constraintMessageTests.testProperty.test.java.lang.Object',
                'constraintMessageTests.testProperty.test',
                'test.TestObjectName.testProperty',
                'test.testProperty',
                'test.java.lang.Object',
                'test'] as String[], errors.getFieldError().getCodes())

    }

     private void assertArraysEqual( Object[] left, Object[] right ) {
        assertEquals( left.length, right.length );
        for( int i = 0; i < left.length; i++ ) {
            Object l = left[i]
            Object r = right[i]
            assertEquals( l, r );
        }
    }

}

class TestConstraint extends AbstractConstraint {
    void processValidate(Object target, Object propertyValue, Errors errors) {
        super.rejectValue(target, errors, 'default.message', 'testconstraint', []);
    }

    public boolean supports(Class type) {
        return true;
    }

    public String getName() {
        return "TestConstraint"
    }
}