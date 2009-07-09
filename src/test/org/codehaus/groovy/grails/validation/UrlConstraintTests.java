package org.codehaus.groovy.grails.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for 'url' constraint.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class UrlConstraintTests extends AbstractConstraintTests {
    protected Class getConstraintClass() {
        return UrlConstraint.class;
    }

    public void testValidation() {
        testConstraintMessageCodes(
                getConstraint("testURL", Boolean.TRUE),
                "wrong_url",
                new String[]{"testClass.testURL.url.error", "testClass.testURL.url.invalid"},
                new Object[]{"testURL", TestClass.class, "wrong_url"}
        );

        testConstraintPassed(
                getConstraint("testURL", Boolean.TRUE),
                "http://www.google.com"
        );

        testConstraintPassed(
                getConstraint("testURL", Boolean.TRUE),
                "https://www.google.com/"
        );

        testConstraintPassed(
                getConstraint("testURL", Boolean.TRUE),
                "https://www.google.com/answers.py?test=1&second=2"
        );

        testConstraintFailed(
                getConstraint("testURL", Boolean.TRUE),
                "http://localhost/tau_gwi_00/clif/cb/19"
        );

        testConstraintPassed(
                getConstraint("testURL", "localhost"),
                "http://localhost/tau_gwi_00/clif/cb/19"
        );

        testConstraintPassed(
                getConstraint("testURL", "localhost(:(\\d{1,5}))?"),
                "http://localhost:8080/tau_gwi_00/clif/cb/19"
        );

        testConstraintPassed(
                getConstraint("testURL", ".*\\.localdomain"),
                "http://localhost.localdomain/myApp/?test=1&second=2"
        );

        testConstraintPassed(
                getConstraint("testURL", ".*\\.localdomain"),
                "http://mytest.localdomain/myApp/?test=1&second=2"
        );

        List regexps = new ArrayList();
        regexps.add("localhost");
        regexps.add("my-machine");

        // now should pass for 'localhost' and 'my-machine'
        testConstraintPassed(
                getConstraint("testURL", regexps),
                "https://localhost/myApp/?test=1&second=2"
        );

        testConstraintPassed(
                getConstraint("testURL", regexps),
                "https://my-machine/myApp/?test=1&second=2"
        );

        // and fail for 'another-machine'
        testConstraintFailed(
                getConstraint("testURL", regexps),
                "https://another-machine/myApp/?test=1&second=2"
        );

        // but still pass for IANA TLD's
        testConstraintPassed(
                getConstraint("testURL", regexps),
                "http://www.google.com/"
        );

        // must always pass when constraint is turned off
        testConstraintPassed(
                getConstraint("testURL", Boolean.FALSE),
                "wrong_url"
        );

        // must always pass on null values
        testConstraintPassed(
                getConstraint("testURL", Boolean.TRUE),
                null
        );

        testConstraintDefaultMessage(
                getConstraint("testURL", Boolean.TRUE),
                "wrong_url",
                "Property [{0}] of class [{1}] with value [{2}] is not a valid URL"
        );

    }

    public void testCreation() {
        UrlConstraint constraint = (UrlConstraint) getConstraint("testString", Boolean.FALSE);
        assertEquals(ConstrainedProperty.URL_CONSTRAINT, constraint.getName());
        assertTrue(constraint.supports(String.class));
        assertFalse(constraint.supports(Float.class));
        assertFalse(constraint.supports(Double.class));
        assertFalse(constraint.supports(Object.class));
        assertFalse(constraint.supports(null));

        try {
            getConstraint("testString", Boolean.TRUE);
        } catch (IllegalArgumentException iae) {
            fail("UrlConstraint should allow boolean parameters.");
        }

        try {
            getConstraint("testString", "localhost");
        } catch (IllegalArgumentException iae) {
            fail("UrlConstraint should allow string parameters.");
        }

        try {
            List regexps = new ArrayList();
            regexps.add("aaa");
            regexps.add("bbb");
            getConstraint("testString", regexps);
        } catch (IllegalArgumentException iae) {
            fail("UrlConstraint should allow list parameters.");
        }

        try {
            getConstraint("testString", new Double(1.0));
            fail("UrlConstraint must throw an exception for non-boolean, non-string and non-list parameters.");
        } catch (IllegalArgumentException iae) {
            // Great
        }

    }
}
