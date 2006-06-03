package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyClassLoader;

import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty;

public class ConstraintsEvaluatingPropertyTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.validation.metaclass.ConstraintsDynamicProperty.get(Object)'
	 */
	public void testGet() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class groovyClass = gcl.parseClass("package org.codehaus.groovy.grails.validation\n" +
				"class Test {\n" +
				"@Property String name\n" +
				"}");
		
		ConstraintsEvaluatingDynamicProperty cp = new ConstraintsEvaluatingDynamicProperty();
		
		Map constraints = (Map)cp.get(groovyClass.newInstance());
		
		assertNotNull(constraints);
		assertFalse(constraints.isEmpty());
	}

}
