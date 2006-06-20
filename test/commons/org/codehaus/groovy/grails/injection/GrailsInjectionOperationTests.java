package org.codehaus.groovy.grails.injection;

//import groovy.lang.GroovyClassLoader;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.core.io.Resource;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class GrailsInjectionOperationTests extends AbstractDependencyInjectionSpringContextTests {

	//private GroovyClassLoader cl;
	private Resource resource;
	private GrailsInjectionOperation injectionOperation;
	
	
	/**
	 * @param operation the operation to set
	 */
	public void setOperation(GrailsInjectionOperation injectionOperation) {
		this.injectionOperation = injectionOperation;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void testIsResourceDomainClass() throws Exception {
		assertNotNull(resource);
		assertTrue(GrailsResourceUtils.isDomainClass(resource.getURL()));
	}
	/*
	 * Test method for 'org.codehaus.groovy.grails.injection.GrailsInjectionOperation.call(SourceUnit, GeneratorContext, ClassNode)'
	 */
	public void testCallSourceUnitGeneratorContextClassNode() throws Exception {
		
		assertNotNull(resource);
		GrailsApplication ga = new DefaultGrailsApplication(new Resource[]{resource},injectionOperation);
		
		assertNotNull(ga);
	}

	protected String[] getConfigLocations() {
		return new String[] { "org/codehaus/groovy/grails/injection/injection-resources-tests.xml" };
	}

}
