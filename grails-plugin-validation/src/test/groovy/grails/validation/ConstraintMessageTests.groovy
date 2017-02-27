package grails.validation

import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

class ConstraintMessageTests extends GroovyTestCase {

    def testProperty

    void testMessageCodeOrder() {
        Constraint c = new TestConstraint()
        c.setOwningClass(this.class)
        c.setPropertyName("testProperty")
        def errors = new BeanPropertyBindingResult(this, "TestObjectName")
        String[] codes = ['test']
        Object[] values = []
        c.rejectValueWithDefaultMessage(this, errors, 'default.message', codes, values)
        assertArraysEqual([
                'grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.TestObjectName.testProperty',
                'grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.testProperty',
                'grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error.java.lang.Object',
                'grails.validation.ConstraintMessageTests.testProperty.TestConstraint.error',
                'constraintMessageTests.testProperty.TestConstraint.error.TestObjectName.testProperty',
                'constraintMessageTests.testProperty.TestConstraint.error.testProperty',
                'constraintMessageTests.testProperty.TestConstraint.error.java.lang.Object',
                'constraintMessageTests.testProperty.TestConstraint.error',
                'grails.validation.ConstraintMessageTests.testProperty.test.TestObjectName.testProperty',
                'grails.validation.ConstraintMessageTests.testProperty.test.testProperty',
                'grails.validation.ConstraintMessageTests.testProperty.test.java.lang.Object',
                'grails.validation.ConstraintMessageTests.testProperty.test',
                'constraintMessageTests.testProperty.test.TestObjectName.testProperty',
                'constraintMessageTests.testProperty.test.testProperty',
                'constraintMessageTests.testProperty.test.java.lang.Object',
                'constraintMessageTests.testProperty.test',
                'test.TestObjectName.testProperty',
                'test.testProperty',
                'test.java.lang.Object',
                'test'] as String[], errors.getFieldError().getCodes())

    }

     private void assertArraysEqual(Object[] left, Object[] right) {
        assertEquals(left.length, right.length)
        for (int i = 0; i < left.length; i++) {
            Object l = left[i]
            Object r = right[i]
            assertEquals(l, r)
        }
    }
}

class TestConstraint extends AbstractConstraint {
    void processValidate(Object target, Object propertyValue, Errors errors) {
        super.rejectValue(target, errors, 'default.message', 'testconstraint', [])
    }

    boolean supports(Class type) { true }

    String getName() { "TestConstraint" }
}
