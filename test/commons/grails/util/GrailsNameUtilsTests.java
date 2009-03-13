/* Copyright 2008 the original author or authors.
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
package grails.util;

import junit.framework.TestCase;

/**
 * Test case for {@link GrailsNameUtils}.
 */
public class GrailsNameUtilsTests extends TestCase {

    public void testGetClassNameRepresentation() {
        assertEquals("MyClass", GrailsNameUtils.getClassNameRepresentation("my-class"));
        assertEquals("MyClass", GrailsNameUtils.getClassNameRepresentation("MyClass"));
    }

    public void testGetNaturalName() throws Exception
    {
        assertEquals("First Name", GrailsNameUtils.getNaturalName("firstName"));
        assertEquals("URL", GrailsNameUtils.getNaturalName("URL"));
        assertEquals("Local URL", GrailsNameUtils.getNaturalName("localURL"));
        assertEquals("URL local", GrailsNameUtils.getNaturalName("URLlocal"));
    }


    public void testGetLogicalName() {
        assertEquals("Test", GrailsNameUtils.getLogicalName("TestController", "Controller"));
        assertEquals("Test", GrailsNameUtils.getLogicalName("org.music.TestController", "Controller"));
    }

    public void testGetLogicalPropertyName() {
        assertEquals("myFunky", GrailsNameUtils.getLogicalPropertyName("MyFunkyController", "Controller"));
        assertEquals("HTML", GrailsNameUtils.getLogicalPropertyName("HTMLCodec", "Codec"));
        assertEquals("payRoll", GrailsNameUtils.getLogicalPropertyName("org.something.PayRollController", "Controller"));
    }

    public void testGetScriptName() {
    	assertEquals("grails-name-utils-tests", GrailsNameUtils.getScriptName(getClass()));
    	assertEquals("", GrailsNameUtils.getScriptName(""));
    	assertNull(GrailsNameUtils.getScriptName((String) null));
        assertNull(GrailsNameUtils.getScriptName((Class) null));
    }

    public void testGetNameFromScript() {
    	assertEquals("GrailsClassUtilsTests", GrailsNameUtils.getNameFromScript("grails-class-utils-tests"));
    	assertEquals("Grails", GrailsNameUtils.getNameFromScript("grails"));
        assertEquals("CreateApp", GrailsNameUtils.getNameFromScript("create-app"));
    	assertEquals("", GrailsNameUtils.getNameFromScript(""));
    	assertNull(GrailsNameUtils.getNameFromScript(null));
    }
    
    public void testIsBlank() {
        assertTrue("'null' value should count as blank.", GrailsNameUtils.isBlank(null));
        assertTrue("Empty string should count as blank.", GrailsNameUtils.isBlank(""));
        assertTrue("Spaces should count as blank.", GrailsNameUtils.isBlank("  "));
        assertTrue("A tab should count as blank.", GrailsNameUtils.isBlank("\t"));
        assertFalse("String with whitespace and non-whitespace should not count as blank.", GrailsNameUtils.isBlank("\t  h"));
        assertFalse("String should not count as blank.", GrailsNameUtils.isBlank("test"));
    }
}
