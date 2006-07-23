package org.codehaus.groovy.grails.commons.metaclass;

import java.util.Date;

import junit.framework.TestCase;

public class DynamicMethodsTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicConstructor.isArgumentsMatch(Object[])'
	 */
	public void testIsArgumentsMatch() {
		DynamicConstructor dc = new AbstractDynamicConstructor(new Class[]{String.class, Integer.class}) {
			public Object invoke(Class clazz, Object[] args) {
				return null;
			}			
		};
		
		assertTrue(dc.isArgumentsMatch(new Object[]{"test", new Integer(1)}));
		assertFalse(dc.isArgumentsMatch(new Object[]{"test"}));
		assertFalse(dc.isArgumentsMatch(new Object[]{"test", new Date()}));
		assertFalse(dc.isArgumentsMatch(new Object[]{"test", new Integer(1), "test"}));
	}

	public void testInvokeConstructor() {
		DynamicMethods dm = new AbstractDynamicMethods(){};
		dm.addDynamicConstructor(new AbstractDynamicConstructor(new Class[]{String.class, Integer.class}) {
			public Object invoke(Class clazz, Object[] args) {
				return args[0]+""+args[1];
			}			
		});
		InvocationCallback callback = new InvocationCallback();
		Object result = dm.invokeConstructor(new Object[]{"test", new Integer(1)},callback);
		
		assertTrue(callback.isInvoked());
		assertNotNull(result);
		assertEquals("test1",result);		
	}
}
