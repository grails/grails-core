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

import junit.framework.TestCase;

/**
 * Test cases for InetAddressValidator.
 *
 * @version $Revision: 586676 $
 */
public class InetAddressValidatorTests extends TestCase {

    private InetAddressValidator validator;

    /**
     * Command-line test method.
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(InetAddressValidatorTests.class);
    }

    /**
     * Constructor.
     * @param name
     */
    public InetAddressValidatorTests(String name) {
        super(name);
    }

    protected void setUp() {
        validator = new InetAddressValidator();
    }

    /**
     * Test IPs that point to real, well-known hosts (without actually looking them up).
     */
    public void testInetAddressesFromTheWild() {
        assertTrue("www.apache.org IP should be valid",       validator.isValid("140.211.11.130"));
        assertTrue("www.l.google.com IP should be valid",     validator.isValid("72.14.253.103"));
        assertTrue("fsf.org IP should be valid",              validator.isValid("199.232.41.5"));
        assertTrue("appscs.ign.com IP should be valid",       validator.isValid("216.35.123.87"));
    }

    /**
     * Test valid and invalid IPs from each address class.
     */
    public void testInetAddressesByClass() {
        assertTrue("class A IP should be valid",              validator.isValid("24.25.231.12"));
        assertFalse("illegal class A IP should be invalid",   validator.isValid("2.41.32.324"));

        assertTrue("class B IP should be valid",              validator.isValid("135.14.44.12"));
        assertFalse("illegal class B IP should be invalid",   validator.isValid("154.123.441.123"));

        assertTrue("class C IP should be valid",              validator.isValid("213.25.224.32"));
        assertFalse("illegal class C IP should be invalid",   validator.isValid("201.543.23.11"));

        assertTrue("class D IP should be valid",              validator.isValid("229.35.159.6"));
        assertFalse("illegal class D IP should be invalid",   validator.isValid("231.54.11.987"));

        assertTrue("class E IP should be valid",              validator.isValid("248.85.24.92"));
        assertFalse("illegal class E IP should be invalid",   validator.isValid("250.21.323.48"));
    }

    /**
     * Test reserved IPs.
     */
    public void testReservedInetAddresses() {
        assertTrue("localhost IP should be valid",            validator.isValid("127.0.0.1"));
        assertTrue("broadcast IP should be valid",            validator.isValid("255.255.255.255"));
    }

    /**
     * Test obviously broken IPs.
     */
    public void testBrokenInetAddresses() {
        assertFalse("IP with characters should be invalid",   validator.isValid("124.14.32.abc"));
        assertFalse("IP with three groups should be invalid", validator.isValid("23.64.12"));
        assertFalse("IP with five groups should be invalid",  validator.isValid("26.34.23.77.234"));
    }
}


