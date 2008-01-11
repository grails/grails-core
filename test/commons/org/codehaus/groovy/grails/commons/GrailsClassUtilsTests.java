/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Graeme Rocher
 * @since 15-Feb-2006
 */
public class GrailsClassUtilsTests extends TestCase {

    public void testGetClassNameRepresentation() {
        assertEquals("MyClass", GrailsClassUtils.getClassNameRepresentation("my-class"));
        assertEquals("MyClass", GrailsClassUtils.getClassNameRepresentation("MyClass"));
    }

    public void testGetNaturalName() throws Exception
    {
        assertEquals("First Name", GrailsClassUtils.getNaturalName("firstName"));
        assertEquals("URL", GrailsClassUtils.getNaturalName("URL"));
        assertEquals("Local URL", GrailsClassUtils.getNaturalName("localURL"));
        assertEquals("URL local", GrailsClassUtils.getNaturalName("URLlocal"));
    }


    public void testGetLogicalName() {
        assertEquals("Test", GrailsClassUtils.getLogicalName("TestController", "Controller"));
        assertEquals("Test", GrailsClassUtils.getLogicalName("org.music.TestController", "Controller"));
    }

    public void testGetLogicalPropertyName() {
        assertEquals("myFunky", GrailsClassUtils.getLogicalPropertyName("MyFunkyController", "Controller"));
        assertEquals("HTML", GrailsClassUtils.getLogicalPropertyName("HTMLCodec", "Codec"));
        assertEquals("payRoll", GrailsClassUtils.getLogicalPropertyName("org.something.PayRollController", "Controller"));
    }

    public void testBooleanMatchesboolean()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Boolean.class, boolean.class));
    }

    public void testbooleanMatchesBoolean()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(boolean.class, Boolean.class));
    }

    public void testIntegerMatchesint()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Integer.class, int.class));
    }

    public void testintMatchesInteger()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(int.class, Integer.class));
    }

    public void testShortMatchesshort()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Short.class, short.class));
    }

    public void testshortMatchesShort()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(short.class, Short.class));
    }

    public void testByteMatchesbyte()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Byte.class, byte.class));
    }

    public void testbyteMatchesByte()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(byte.class, Byte.class));
    }

    public void testCharacterMatcheschar()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Character.class, char.class));
    }

    public void testcharMatchesCharacter()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(char.class, Character.class));
    }

    public void testLongMatcheslong()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Long.class, long.class));
    }

    public void testlongMatchesLong()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(long.class, Long.class));
    }

    public void testFloatMatchesfloat()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Float.class, float.class));
    }

    public void testfloatMatchesFloat()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(float.class, Float.class));
    }

    public void testDoubleMatchesdouble()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(Double.class, double.class));
    }

    public void testdoubleMatchesDouble()
    {
        assertTrue(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(double.class, Double.class));
    }

    public void testAssignableFromOrPrimitiveCompatible()
    {
        assertTrue(GrailsClassUtils.isGroovyAssignableFrom(double.class, Double.class));
        assertTrue(GrailsClassUtils.isGroovyAssignableFrom(Integer.class, int.class));
        assertTrue(GrailsClassUtils.isGroovyAssignableFrom(TestCase.class, GrailsClassUtilsTests.class));
        assertTrue(GrailsClassUtils.isGroovyAssignableFrom(Number.class, int.class));
        //assertTrue(GrailsClassUtils.isGroovyAssignableFrom(double.class, BigInteger.class));
    }

    public void testGetterNames()
    {
        assertEquals("getConstraints",GrailsClassUtils.getGetterName("constraints"));
        assertEquals("getURL",GrailsClassUtils.getGetterName("URL"));
        assertEquals("getUrl", GrailsClassUtils.getGetterName("Url"));
    }
    
    public void testIsGetterOrSetter() {
    	assertTrue(GrailsClassUtils.isSetter("setSomething", new Class[]{String.class}));
    	assertTrue(GrailsClassUtils.isGetter("getSomething", new Class[0]));
    	assertTrue(GrailsClassUtils.isSetter("setURL", new Class[]{String.class}));
    	assertTrue(GrailsClassUtils.isGetter("getURL", new Class[0]));
    	
    	assertFalse(GrailsClassUtils.isGetter("something", new Class[]{String.class}));
    	assertFalse(GrailsClassUtils.isGetter("get", new Class[0]));
    	assertFalse(GrailsClassUtils.isSetter("set", new Class[]{String.class}));
    	assertFalse(GrailsClassUtils.isGetter("somethingElse", new Class[0]));
    	assertFalse(GrailsClassUtils.isSetter("setSomething", new Class[]{String.class, Object.class}));
    	assertFalse(GrailsClassUtils.isGetter("getSomething", new Class[]{Object.class}));
    	
    	assertFalse(GrailsClassUtils.isGetter(null, new Class[]{Object.class}));
    	assertFalse(GrailsClassUtils.isGetter("getSomething",null));
    	assertFalse(GrailsClassUtils.isGetter(null,null));
    }
    
    public void testGetPropertyForGetter() {
    	assertEquals("something", GrailsClassUtils.getPropertyForGetter("getSomething"));
    	assertEquals("URL", GrailsClassUtils.getPropertyForGetter("getURL"));
    }

    public void testGetStaticProperty()
    {
        assertEquals(HttpServletRequest.BASIC_AUTH,
            GrailsClassUtils.getStaticPropertyValue(HttpServletRequest.class,
                    "BASIC_AUTH"));

        assertEquals("hello",
            GrailsClassUtils.getStaticPropertyValue(TestBean.class,
                    "welcomeMessage"));
    }

    public void testIsPublicStatic() throws Exception
    {
        assertTrue(GrailsClassUtils.isPublicStatic(
                HttpServletRequest.class.getDeclaredField("BASIC_AUTH")));

        assertFalse(GrailsClassUtils.isPublicStatic(
                String.class.getDeclaredField("serialVersionUID")));

        assertFalse(GrailsClassUtils.isPublicStatic(
                TestBean.class.getDeclaredField("welcomeMessage")));

    }

    public void testGetPropertyOrStatic()
    {
        TestBean bean = new TestBean();
        assertEquals("hello",
            GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue( bean,
                    "welcomeMessage"));
        assertEquals("marc",
            GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue( bean,
                    "userName"));
        assertEquals("indian",
            GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue( bean,
                    "favouriteFood"));
        assertEquals("Cardiacs",
            GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue( bean,
                    "favouriteArtist"));
    }

    public void testGetFieldValue()
    {
        TestBean bean = new TestBean();

        assertTrue(GrailsClassUtils.isPublicField(bean, "favouriteArtist"));

        assertEquals("Cardiacs",
            GrailsClassUtils.getFieldValue(bean, "favouriteArtist"));

    }
    
    public void testGetScriptName() {
    	assertEquals("grails-class-utils-tests", GrailsClassUtils.getScriptName(getClass()));
    }
    
    public void testGetNameFromScript() {
    	assertEquals("GrailsClassUtilsTests", GrailsClassUtils.getNameFromScript("grails-class-utils-tests"));
    	assertEquals("Grails", GrailsClassUtils.getNameFromScript("grails"));
    }
    
    public void testIsAssignableOrConvertibleFrom() {
    	
    	// test number
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, int.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Integer.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, double.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, Double.class));
    	
    	// test integer
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, int.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Integer.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, double.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, Double.class));
    	
    	// test short
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, int.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Integer.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, double.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class, Double.class));
    	
    	// test byte
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, int.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Integer.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, double.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Byte.class, Double.class));
    	
    	// test long
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, int.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Integer.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, double.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, Double.class));
    	
    	// test float
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, int.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Integer.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, double.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, Double.class));
    	
    	// test double
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, int.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Integer.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Short.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Byte.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Long.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, float.class));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, double.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, Double.class));
    	
    	// test boolean
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Boolean.class, boolean.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Boolean.class, Boolean.class));
    	
    	// test character
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Character.class, char.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Character.class, Character.class));
    	
    	// test object
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, int.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Integer.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Short.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Byte.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Long.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Float.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, double.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Double.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, boolean.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Boolean.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, char.class));
    	assertTrue(GrailsClassUtils.isAssignableOrConvertibleFrom(Object.class, Character.class));
    	
    	// test null
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(null, null));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class, null));
    	assertFalse(GrailsClassUtils.isAssignableOrConvertibleFrom(null, int.class));
    	
    }
    
}
