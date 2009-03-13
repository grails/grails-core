package org.codehaus.groovy.grails.commons.metaclass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

public class DynamicMethodsInterceptorTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.afterConstructor(Object[], Object)'
	 */
	public void testAfterConstructor() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test {}");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new ConstructorInterceptor() {
			public Object beforeConstructor(Object[] args, InvocationCallback callback) {
				return null;
			}
			public Object afterConstructor(Object[] args, Object instantiatedInstance) {				
				assertNotNull(instantiatedInstance);
				assertEquals(testClass,instantiatedInstance.getClass());
				return instantiatedInstance;
			}
			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				return null;
			}	
			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				return null;
			}
			
		});
		
		
		Object instance = go.getMetaClass().invokeConstructor(new Object[0]);
		assertNotNull(instance);
		assertEquals(testClass,instance.getClass());
	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.beforeConstructor(Object[], InvocationCallback)'
	 */
	public void testBeforeConstructor() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test {}");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new ConstructorInterceptor() {
			public Object beforeConstructor(Object[] args, InvocationCallback callback) {
				callback.markInvoked();
				return "success";
			}
			public Object afterConstructor(Object[] args, Object instantiatedInstance) {				
				return instantiatedInstance;
			}
			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				return null;
			}	
			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				return null;
			}
			
		});
		
		
		Object instance = go.getMetaClass().invokeConstructor(new Object[0]);
		assertNotNull(instance);
		assertEquals("success",instance);
	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.beforeInvoke(Object, String, Object[], InvocationCallback)'
	 */
	public void testBeforeInvoke() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test { def invokeMe() {'hello'} }");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new Interceptor() {
			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				callback.markInvoked();
				return "success";
			}	
			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				return result;
			}
			
		});
		
		
		Object result =  go.invokeMethod("invokeMe", null);
		assertNotNull(result);
		assertEquals("success",result);
	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.afterInvoke(Object, String, Object[], Object)'
	 */
	public void testAfterInvoke() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test { def invokeMe() {'hello'} }");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new Interceptor() {
			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				return null;
			}	
			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				assertEquals("hello", result);
				assertEquals("invokeMe", methodName);
				
				return result.toString()+1;
			}
			
		});
		
		
		Object result =  go.invokeMethod("invokeMe", null);
		assertNotNull(result);
		assertEquals("hello1",result);
	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.beforeGet(Object, String, InvocationCallback)'
	 */
	public void testBeforeGet() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test { def prop = 'hello' }");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new PropertyAccessInterceptor(){

			public Object beforeGet(Object object, String property, InvocationCallback callback) {
				callback.markInvoked();
				return "success";
			}

			public void beforeSet(Object object, String property, Object newValue, InvocationCallback callback) {
				// TODO Auto-generated method stub
				
			}

			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				return null;
			}

			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				return null;
			}
			
		});
		
		
		
		Object result =  go.getProperty("prop");
		assertNotNull(result);
		assertEquals("success",result);

	}

	/*
	 * Test method for 'org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor.beforeSet(Object, String, Object, InvocationCallback)'
	 */
	public void testBeforeSet() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		final Class testClass = gcl.parseClass("class Test { def prop = 'hello' }");
		GroovyObject go = (GroovyObject)testClass.newInstance();
		
		ProxyMetaClass pmc = ProxyMetaClass.getInstance(testClass);
		go.setMetaClass(pmc);

		pmc.setInterceptor( new PropertyAccessInterceptor(){

			public Object beforeGet(Object object, String property, InvocationCallback callback) {
				return null;
			}

			public void beforeSet(Object object, String property, Object newValue, InvocationCallback callback) {
				assertEquals("prop", property);
				BeanWrapper bean = new BeanWrapperImpl(object);
				bean.setPropertyValue("prop","success");
				callback.markInvoked();
			}

			public Object beforeInvoke(Object object, String methodName, Object[] arguments, InvocationCallback callback) {
				return null;
			}

			public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
				return null;
			}
			
		});
		
		
		go.setProperty("prop", "newValue");
		Object result =  go.getProperty("prop");
		assertNotNull(result);
		assertEquals("success",result);
	}

}
