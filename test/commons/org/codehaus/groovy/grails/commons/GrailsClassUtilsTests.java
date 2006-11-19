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

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Graeme Rocher
 * @since 15-Feb-2006
 */
public class GrailsClassUtilsTests extends TestCase {

    public void testGetNaturalName() throws Exception
    {
        assertEquals("First Name", GrailsClassUtils.getNaturalName("firstName"));
        assertEquals("URL", GrailsClassUtils.getNaturalName("URL"));
        assertEquals("Local URL", GrailsClassUtils.getNaturalName("localURL"));
        assertEquals("URL local", GrailsClassUtils.getNaturalName("URLlocal"));
    }

    public void testIsDomainClass() throws Exception
    {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class c = gcl.parseClass("class Test { Long id;Long version;}\n");

        assertTrue(GrailsClassUtils.isDomainClass(c));
    }

    public void testIsController() throws Exception
    {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class c = gcl.parseClass("class TestController { }\n");

        assertTrue(GrailsClassUtils.isControllerClass(c));
    }

    public void testIsTagLib() throws Exception
    {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class c = gcl.parseClass("class TestTagLib { }\n");

        assertTrue(GrailsClassUtils.isTagLibClass(c));
    }

    public void testIsBootStrap() throws Exception
    {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class c = gcl.parseClass("class TestBootStrap { }\n");

        assertTrue(GrailsClassUtils.isBootstrapClass(c));
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
        assertEquals(GrailsClassUtils.getGetterName("constraints"), "getConstraints");
        assertEquals(GrailsClassUtils.getGetterName("URL"), "getURL");
        assertEquals(GrailsClassUtils.getGetterName("Url"), "getUrl");
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
}
