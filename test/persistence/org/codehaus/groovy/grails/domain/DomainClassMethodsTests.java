package org.codehaus.groovy.grails.domain;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethods;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodsMetaClass;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.metaclass.DataBindingDynamicConstructor;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import junit.framework.TestCase;

public class DomainClassMethodsTests extends TestCase {

	public void testDataBindingConstructor() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class c = gcl.parseClass("class Test { Integer number }");
		
		GroovyObject go = (GroovyObject)c.newInstance();
		
		DynamicMethods methods = new AbstractDynamicMethods(c) {
			
		};
		MetaClass mc = new DynamicMethodsMetaClass(c,methods,true);
		go.setMetaClass(mc);
		methods.addDynamicConstructor(new DataBindingDynamicConstructor());
		
		Map args = new HashMap();
		args.put("number", "1");
		
		GroovyObject instance = (GroovyObject)go.getMetaClass().invokeConstructor(new Object[]{args});
		assertNotNull(instance);
		assertNotNull(instance.getProperty("number"));
		assertEquals(new Integer(1), instance.getProperty("number"));
		
	}
}
