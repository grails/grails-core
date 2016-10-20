/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.validation.routines;

import junit.framework.TestCase;

/**
 * Tests for the EmailValidator.
 */
public class EmailValidatorTests extends TestCase {

    private EmailValidator validator = EmailValidator.getInstance();

    public void testEmail() {
        assertTrue(validator.isValid("jsmith@apache.org"));
    }

    public void testEmailWithNumericAddress() {
        assertTrue(validator.isValid("someone@[216.109.118.76]"));
        assertTrue(validator.isValid("someone@yahoo.com"));
    }

    public void testEmailExtension() {
        assertTrue(validator.isValid("jsmith@apache.org"));
        assertTrue(validator.isValid("jsmith@apache.com"));
        assertTrue(validator.isValid("jsmith@apache.net"));
        assertTrue(validator.isValid("jsmith@apache.info"));
        assertFalse(validator.isValid("jsmith@apache."));
        assertFalse(validator.isValid("jsmith@apache.c"));
        assertTrue(validator.isValid("someone@yahoo.museum"));
        assertFalse(validator.isValid("someone@yahoo.mu-seum"));
    }

    /**
     * <p>Tests the e-mail validation with a dash in
     * the address.</p>
     */
    public void testEmailWithDash() {
        assertTrue(validator.isValid("andy.noble@data-workshop.com"));
        assertFalse(validator.isValid("andy-noble@data-workshop.-com"));
        assertFalse(validator.isValid("andy-noble@data-workshop.c-om"));
        assertFalse(validator.isValid("andy-noble@data-workshop.co-m"));
    }

    /**
     * Tests the e-mail validation with a dot at the end of
     * the address.
     */
    public void testEmailWithDotEnd()  {
        assertFalse(validator.isValid("andy.noble@data-workshop.com."));
    }

    /**
     * Tests the e-mail validation with an RCS-noncompliant character in
     * the address.
     */
    public void testEmailWithBogusCharacter()  {

        assertFalse(validator.isValid("andy.noble@\u008fdata-workshop.com"));

        // The ' character is valid in an email username.
        assertTrue(validator.isValid("andy.o'reilly@data-workshop.com"));

        // But not in the domain name.
        assertFalse(validator.isValid("andy@o'reilly.data-workshop.com"));

        // The + character is valid in an email username.
        assertTrue(validator.isValid("foo+bar@i.am.not.in.us.example.com"));

        // But not in the domain name
        assertFalse(validator.isValid("foo+bar@example+3.com"));

        // Domains with only special characters aren't allowed (VALIDATOR-286)
        assertFalse(validator.isValid("test@%*.com"));
        assertFalse(validator.isValid("test@^&#.com"));

    }

    /**
     * Tests the email validation with commas.
     */
    public void testEmailWithCommas()  {
        assertFalse(validator.isValid("joeblow@apa,che.org"));

        assertFalse(validator.isValid("joeblow@apache.o,rg"));

        assertFalse(validator.isValid("joeblow@apache,org"));

    }

    /**
     * Tests the email validation with spaces.
     */
    public void testEmailWithSpaces()  {
        assertFalse(validator.isValid("joeblow @apache.org")); // TODO - this should be valid?

        assertFalse(validator.isValid("joeblow@ apache.org"));

        assertTrue(validator.isValid(" joeblow@apache.org")); // TODO - this should be valid?

        assertTrue(validator.isValid("joeblow@apache.org "));

        assertFalse(validator.isValid("joe blow@apache.org "));

        assertFalse(validator.isValid("joeblow@apa che.org "));

    }

    /**
     * Tests the email validation with ascii control characters.
     * (i.e. Ascii chars 0 - 31 and 127)
     */
    public void testEmailWithControlChars()  {
        for (char c = 0; c < 32; c++) {
            assertFalse("Test control char " + ((int)c), validator.isValid("foo" + c + "bar@domain.com"));
        }
        assertFalse("Test control char 127", validator.isValid("foo" + ((char)127) + "bar@domain.com"));
    }

    /**
     * VALIDATOR-296 - A / or a ! is valid in the user part,
     *  but not in the domain part
     */
    public void testEmailWithSlashes() {
        assertTrue(
                "/ and ! valid in username",
                validator.isValid("joe!/blow@apache.org")
        );
        assertFalse(
                "/ not valid in domain",
                validator.isValid("joe@ap/ache.org")
        );
        assertFalse(
                "! not valid in domain",
                validator.isValid("joe@apac!he.org")
        );
    }

    /**
     * Write this test according to parts of RFC, as opposed to the type of character
     * that is being tested.
     */
    public void testEmailUserName()  {

        assertTrue(validator.isValid("joe1blow@apache.org"));

        assertTrue(validator.isValid("joe$blow@apache.org"));

        assertTrue(validator.isValid("joe-@apache.org"));

        assertTrue(validator.isValid("joe_@apache.org"));

        assertTrue(validator.isValid("joe+@apache.org")); // + is valid unquoted

        assertTrue(validator.isValid("joe!@apache.org")); // ! is valid unquoted

        assertTrue(validator.isValid("joe*@apache.org")); // * is valid unquoted

        assertTrue(validator.isValid("joe'@apache.org")); // ' is valid unquoted

        assertTrue(validator.isValid("joe%45@apache.org")); // % is valid unquoted

        assertTrue(validator.isValid("joe?@apache.org")); // ? is valid unquoted

        assertTrue(validator.isValid("joe&@apache.org")); // & ditto

        assertTrue(validator.isValid("joe=@apache.org")); // = ditto

        assertTrue(validator.isValid("+joe@apache.org")); // + is valid unquoted

        assertTrue(validator.isValid("!joe@apache.org")); // ! is valid unquoted

        assertTrue(validator.isValid("*joe@apache.org")); // * is valid unquoted

        assertTrue(validator.isValid("'joe@apache.org")); // ' is valid unquoted

        assertTrue(validator.isValid("%joe45@apache.org")); // % is valid unquoted

        assertTrue(validator.isValid("?joe@apache.org")); // ? is valid unquoted

        assertTrue(validator.isValid("&joe@apache.org")); // & ditto

        assertTrue(validator.isValid("=joe@apache.org")); // = ditto

        assertTrue(validator.isValid("+@apache.org")); // + is valid unquoted

        assertTrue(validator.isValid("!@apache.org")); // ! is valid unquoted

        assertTrue(validator.isValid("*@apache.org")); // * is valid unquoted

        assertTrue(validator.isValid("'@apache.org")); // ' is valid unquoted

        assertTrue(validator.isValid("%@apache.org")); // % is valid unquoted

        assertTrue(validator.isValid("?@apache.org")); // ? is valid unquoted

        assertTrue(validator.isValid("&@apache.org")); // & ditto

        assertTrue(validator.isValid("=@apache.org")); // = ditto


        //UnQuoted Special characters are invalid

        assertFalse(validator.isValid("joe.@apache.org")); // . not allowed at end of local part

        assertFalse(validator.isValid(".joe@apache.org")); // . not allowed at start of local part

        assertFalse(validator.isValid(".@apache.org")); // . not allowed alone

        assertTrue(validator.isValid("joe.ok@apache.org")); // . allowed embedded

        assertFalse(validator.isValid("joe..ok@apache.org")); // .. not allowed embedded

        assertFalse(validator.isValid("..@apache.org")); // .. not allowed alone

        assertFalse(validator.isValid("joe(@apache.org"));

        assertFalse(validator.isValid("joe)@apache.org"));

        assertFalse(validator.isValid("joe,@apache.org"));

        assertFalse(validator.isValid("joe;@apache.org"));


        //Quoted Special characters are valid
        assertTrue(validator.isValid("\"joe.\"@apache.org"));

        assertTrue(validator.isValid("\".joe\"@apache.org"));

        assertTrue(validator.isValid("\"joe+\"@apache.org"));

        assertTrue(validator.isValid("\"joe!\"@apache.org"));

        assertTrue(validator.isValid("\"joe*\"@apache.org"));

        assertTrue(validator.isValid("\"joe'\"@apache.org"));

        assertTrue(validator.isValid("\"joe(\"@apache.org"));

        assertTrue(validator.isValid("\"joe)\"@apache.org"));

        assertTrue(validator.isValid("\"joe,\"@apache.org"));

        assertTrue(validator.isValid("\"joe%45\"@apache.org"));

        assertTrue(validator.isValid("\"joe;\"@apache.org"));

        assertTrue(validator.isValid("\"joe?\"@apache.org"));

        assertTrue(validator.isValid("\"joe&\"@apache.org"));

        assertTrue(validator.isValid("\"joe=\"@apache.org"));

        assertTrue(validator.isValid("\"..\"@apache.org"));

        // escaped quote character valid in quoted string
        assertTrue(validator.isValid("\"john\\\"doe\"@apache.org"));

        assertTrue(validator.isValid("john56789.john56789.john56789.john56789.john56789.john56789.john@example.com"));

        assertFalse(validator.isValid("john56789.john56789.john56789.john56789.john56789.john56789.john5@example.com"));

        assertTrue(validator.isValid("\\>escape\\\\special\\^characters\\<@example.com"));

        assertTrue(validator.isValid("Abc\\@def@example.com"));

        assertFalse(validator.isValid("Abc@def@example.com"));

        assertTrue(validator.isValid("space\\ monkey@example.com"));
    }

    /**
     * Tests the e-mail validation with a user at a TLD
     *
     * http://tools.ietf.org/html/rfc5321#section-2.3.5
     * (In the case of a top-level domain used by itself in an
     * email address, a single string is used without any dots)
     */
    public void testEmailAtTLD() {
        EmailValidator val = EmailValidator.getInstance(true);
        assertTrue(val.isValid("test@com"));
    }
}
