/* Copyright 2004-2005 Graeme Rocher
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

import java.util.Date;
import java.net.URL;
import java.net.URI;

/**
 * Tests for the GrailsDomainConfigurationUtil class
 *
 *
 * @author Graeme Rocher
 * @since 0.5
 *        <p/>
 *        Created: Apr 27, 2007
 *        Time: 11:24:21 AM
 */
public class GrailsDomainConfigurationUtilTests extends TestCase {

    public void testIsBasicType() {
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(boolean.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(long.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(int.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(short.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(char.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(double.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(float.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(byte.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Boolean.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Long.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Integer.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Short.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Character.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Double.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Float.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Byte.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Date.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(URL.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(URI.class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(boolean[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(long[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(int[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(short[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(char[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(double[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(float[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(byte[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Boolean[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Long[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Integer[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Short[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Character[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Double[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Float[].class));
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Byte[].class));        
    }
}
