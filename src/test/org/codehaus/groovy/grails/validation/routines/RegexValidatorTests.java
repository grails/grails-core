/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.validation.routines;

import java.util.regex.PatternSyntaxException;

import junit.framework.TestCase;

/**
 * Test Case for RegexValidator.
 *
 * @version $Revision: 595023 $ $Date: 2007-11-14 22:49:23 +0300 (Ср, 14 ноя 2007) $
 * @since Validator 1.4
 */
public class RegexValidatorTests extends TestCase {

    private static final String REGEX         = "^([abc]*)(?:\\-)([DEF]*)(?:\\-)([123]*)$";

    private static final String COMPONENT_1 = "([abc]{3})";
    private static final String COMPONENT_2 = "([DEF]{3})";
    private static final String COMPONENT_3 = "([123]{3})";
    private static final String SEPARATOR_1  = "(?:\\-)";
    private static final String SEPARATOR_2  = "(?:\\s)";
    private static final String REGEX_1 = "^" + COMPONENT_1 + SEPARATOR_1 + COMPONENT_2 + SEPARATOR_1 + COMPONENT_3 + "$";
    private static final String REGEX_2 = "^" + COMPONENT_1 + SEPARATOR_2 + COMPONENT_2 + SEPARATOR_2 + COMPONENT_3 + "$";
    private static final String REGEX_3 = "^" + COMPONENT_1 + COMPONENT_2 + COMPONENT_3 + "$";
    private static final String[] MULTIPLE_REGEX = new String[] {REGEX_1, REGEX_2, REGEX_3};

    /**
     * Constrct a new test case.
     * @param name The name of the test
     */
    public RegexValidatorTests(String name) {
        super(name);
    }

    /**
     * Set Up.
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Tear Down.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test instance methods with single regular expression.
     */
    public void testSingle() {
        RegexValidator sensitive   = new RegexValidator(REGEX);
        RegexValidator insensitive = new RegexValidator(REGEX, false);

        // isValid()
        assertEquals("Sensitive isValid() valid",     true,   sensitive.isValid("ac-DE-1"));
        assertEquals("Sensitive isValid() invalid",   false,  sensitive.isValid("AB-de-1"));
        assertEquals("Insensitive isValid() valid",   true,   insensitive.isValid("AB-de-1"));
        assertEquals("Insensitive isValid() invalid", false,  insensitive.isValid("ABd-de-1"));

        // validate()
        assertEquals("Sensitive validate() valid",     "acDE1", sensitive.validate("ac-DE-1"));
        assertEquals("Sensitive validate() invalid",   null,    sensitive.validate("AB-de-1"));
        assertEquals("Insensitive validate() valid",   "ABde1", insensitive.validate("AB-de-1"));
        assertEquals("Insensitive validate() invalid", null,    insensitive.validate("ABd-de-1"));

        // match()
        checkArray("Sensitive match() valid",     new String[] {"ac", "DE", "1"}, sensitive.match("ac-DE-1"));
        checkArray("Sensitive match() invalid",   null,                           sensitive.match("AB-de-1"));
        checkArray("Insensitive match() valid",   new String[] {"AB", "de", "1"}, insensitive.match("AB-de-1"));
        checkArray("Insensitive match() invalid", null,                           insensitive.match("ABd-de-1"));
        assertEquals("validate one", "ABC", (new RegexValidator("^([A-Z]*)$")).validate("ABC"));
        checkArray("match one", new String[] {"ABC"}, (new RegexValidator("^([A-Z]*)$")).match("ABC"));
    }

    /**
     * Test with multiple regular expressions (case sensitive).
     */
    public void testMultipleSensitive() {

        // ------------ Set up Sensitive Validators
        RegexValidator multiple   = new RegexValidator(MULTIPLE_REGEX);
        RegexValidator single1   = new RegexValidator(REGEX_1);
        RegexValidator single2   = new RegexValidator(REGEX_2);
        RegexValidator single3   = new RegexValidator(REGEX_3);

        // ------------ Set up test values
        String value = "aac FDE 321";
        String expect = "aacFDE321";
        String[] array = new String[] {"aac", "FDE", "321"};

        // isValid()
        assertEquals("Sensitive isValid() Multiple", true,  multiple.isValid(value));
        assertEquals("Sensitive isValid() 1st",      false, single1.isValid(value));
        assertEquals("Sensitive isValid() 2nd",      true,  single2.isValid(value));
        assertEquals("Sensitive isValid() 3rd",      false, single3.isValid(value));

        // validate()
        assertEquals("Sensitive validate() Multiple", expect, multiple.validate(value));
        assertEquals("Sensitive validate() 1st",      null,   single1.validate(value));
        assertEquals("Sensitive validate() 2nd",      expect, single2.validate(value));
        assertEquals("Sensitive validate() 3rd",      null,   single3.validate(value));

        // match()
        checkArray("Sensitive match() Multiple", array, multiple.match(value));
        checkArray("Sensitive match() 1st",      null,  single1.match(value));
        checkArray("Sensitive match() 2nd",      array, single2.match(value));
        checkArray("Sensitive match() 3rd",      null,  single3.match(value));

        // All invalid
        value = "AAC*FDE*321";
        assertEquals("isValid() Invalid",  false, multiple.isValid(value));
        assertEquals("validate() Invalid", null,  multiple.validate(value));
        assertEquals("match() Multiple",   null,  multiple.match(value));
    }

    /**
     * Test with multiple regular expressions (case in-sensitive).
     */
    public void testMultipleInsensitive() {

        // ------------ Set up In-sensitive Validators
        RegexValidator multiple = new RegexValidator(MULTIPLE_REGEX, false);
        RegexValidator single1   = new RegexValidator(REGEX_1, false);
        RegexValidator single2   = new RegexValidator(REGEX_2, false);
        RegexValidator single3   = new RegexValidator(REGEX_3, false);

        // ------------ Set up test values
        String value = "AAC FDE 321";
        String expect = "AACFDE321";
        String[] array = new String[] {"AAC", "FDE", "321"};

        // isValid()
        assertEquals("isValid() Multiple", true,  multiple.isValid(value));
        assertEquals("isValid() 1st",      false, single1.isValid(value));
        assertEquals("isValid() 2nd",      true,  single2.isValid(value));
        assertEquals("isValid() 3rd",      false, single3.isValid(value));

        // validate()
        assertEquals("validate() Multiple", expect, multiple.validate(value));
        assertEquals("validate() 1st",      null,   single1.validate(value));
        assertEquals("validate() 2nd",      expect, single2.validate(value));
        assertEquals("validate() 3rd",      null,   single3.validate(value));

        // match()
        checkArray("match() Multiple", array, multiple.match(value));
        checkArray("match() 1st",      null,  single1.match(value));
        checkArray("match() 2nd",      array, single2.match(value));
        checkArray("match() 3rd",      null,  single3.match(value));

        // All invalid
        value = "AAC*FDE*321";
        assertEquals("isValid() Invalid",  false, multiple.isValid(value));
        assertEquals("validate() Invalid", null,  multiple.validate(value));
        assertEquals("match() Multiple",   null,  multiple.match(value));
    }

    /**
     * Test Null value
     */
    public void testNullValue() {

        RegexValidator validator = new RegexValidator(REGEX);
        assertEquals("Instance isValid()",  false, validator.isValid(null));
        assertEquals("Instance validate()", null,  validator.validate(null));
        assertEquals("Instance match()",    null,  validator.match(null));
    }

    /**
     * Test exceptions
     */
    public void testMissingRegex() {

        // Single Regular Expression - null
        try {
            new RegexValidator((String)null);
            fail("Single Null - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Single Null", "Regular expression[0] is missing", e.getMessage());
        }

        // Single Regular Expression - Zero Length
        try {
            new RegexValidator("");
            fail("Single Zero Length - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Single Zero Length", "Regular expression[0] is missing", e.getMessage());
        }

        // Multiple Regular Expression - Null array
        try {
            new RegexValidator((String[])null);
            fail("Null Array - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Null Array", "Regular expressions are missing", e.getMessage());
        }

        // Multiple Regular Expression - Zero Length array
        try {
            new RegexValidator(new String[0]);
            fail("Zero Length Array - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Zero Length Array", "Regular expressions are missing", e.getMessage());
        }

        // Multiple Regular Expression - Array has Null
        String[] expressions = new String[] {"ABC", null};
        try {
            new RegexValidator(expressions);
            fail("Array has Null - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Array has Null", "Regular expression[1] is missing", e.getMessage());
        }

        // Multiple Regular Expression - Array has Zero Length
        expressions = new String[] {"", "ABC"};
        try {
            new RegexValidator(expressions);
            fail("Array has Zero Length - expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Array has Zero Length", "Regular expression[0] is missing", e.getMessage());
        }
    }

    /**
     * Test exceptions
     */
    public void testExceptions() {
        String invalidRegex = "^([abCD12]*$";
        try {
            new RegexValidator(invalidRegex);
        } catch (PatternSyntaxException e) {
            // expected
        }
    }

    /**
     * Test toString() method
     */
    public void testToString() {
        RegexValidator single = new RegexValidator(REGEX);
        assertEquals("Single", "RegexValidator{" + REGEX + "}", single.toString());

        RegexValidator multiple = new RegexValidator(new String[] {REGEX, REGEX});
        assertEquals("Multiple", "RegexValidator{" + REGEX + "," + REGEX + "}", multiple.toString());
    }

    /**
     * Compare two arrays
     * @param label Label for the test
     * @param expect Expected array
     * @param result Actual array
     */
    private void checkArray(String label, String[] expect, String[] result) {

        // Handle nulls
        if (expect == null || result == null) {
            if (expect == null && result == null) {
                return; // valid, both null
            } else {
                fail(label + " Null expect=" + expect + " result=" + result);
            }
        }

        // Check Length
        if (expect.length != result.length) {
            fail(label + " Length expect=" + expect.length + " result=" + result.length);
        }

        // Check Values
        for (int i = 0; i < expect.length; i++) {
            assertEquals(label +" value[" + i + "]", expect[i], result[i]);
        }
    }

}
