/*
 * Copyright 2011 GoPivotal, Inc. All Rights Reserved
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
package org.codehaus.groovy.grails.commons.metaclass;

import java.util.Date;

import junit.framework.TestCase;

public class DynamicMethodsTests extends TestCase {

    /*
     * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicConstructor.isArgumentsMatch(Object[])'
     */
    public void testIsArgumentsMatch() {
        DynamicConstructor dc = new AbstractDynamicConstructor(new Class[]{String.class, Integer.class}) {
            @SuppressWarnings("rawtypes")
            public Object invoke(Class clazz, Object[] args) {
                return null;
            }
        };

        assertTrue(dc.isArgumentsMatch(new Object[]{"test", 1}));
        assertFalse(dc.isArgumentsMatch(new Object[]{"test"}));
        assertFalse(dc.isArgumentsMatch(new Object[]{"test", new Date()}));
        assertFalse(dc.isArgumentsMatch(new Object[]{"test", 1, "test"}));
    }

    public void testInvokeConstructor() {
        DynamicMethods dm = new AbstractDynamicMethods() {/*empty*/};
        dm.addDynamicConstructor(new AbstractDynamicConstructor(new Class[]{String.class, Integer.class}) {
            @SuppressWarnings("rawtypes")
            public Object invoke(Class clazz, Object[] args) {
                return args[0]+""+args[1];
            }
        });
        InvocationCallback callback = new InvocationCallback();
        Object result = dm.invokeConstructor(new Object[]{"test", 1},callback);

        assertTrue(callback.isInvoked());
        assertNotNull(result);
        assertEquals("test1",result);
    }
}
