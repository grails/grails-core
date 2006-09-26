package org.codehaus.groovy.grails.injection;

//import groovy.lang.GroovyClassLoader;

import java.util.HashSet;
import java.util.Set;

import groovy.lang.GroovyObject;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class GrailsInjectionOperationTests extends AbstractDependencyInjectionSpringContextTests {

	//private GroovyClassLoader cl;
	private Resource[] resources;
	private GrailsInjectionOperation injectionOperation;
	private GrailsApplication ga;
	
	
	/**
	 * @param operation the operation to set
	 */
	public void setOperation(GrailsInjectionOperation injectionOperation) {
		this.injectionOperation = injectionOperation;
	}


	protected void onSetUp() throws Exception {
		super.onSetUp();
		
		resources = new PathMatchingResourcePatternResolver().getResources("classpath:org/codehaus/groovy/grails/injection/grails-app/domain/*.groovy");
		try  {
		ga = new DefaultGrailsApplication(resources,injectionOperation);		
		}
		catch(Exception e) {
			if(e instanceof MultipleCompilationErrorsException) {
				MultipleCompilationErrorsException mcee = (MultipleCompilationErrorsException)e;
				
				mcee.getErrorCollector().getException(0).printStackTrace(System.out);
			}
			throw e;
		}
	}


	public void testIsResourceDomainClass() throws Exception {
		assertNotNull(resources);
		assertTrue(GrailsResourceUtils.isDomainClass(resources[0].getURL()));
	}
	/*
	 * Test method for 'org.codehaus.groovy.grails.injection.GrailsInjectionOperation.call(SourceUnit, GeneratorContext, ClassNode)'
	 */
	public void testCallSourceUnitGeneratorContextClassNode() throws Exception {
		
		assertNotNull(resources);
	
		
		GroovyObject testObject = (GroovyObject)ga.getClassLoader()
													.loadClass("TestInjection")
													.newInstance();
		testObject.setProperty(GrailsDomainClassProperty.IDENTITY,  new Long(5));
		
		Long id = (Long)testObject.getProperty(GrailsDomainClassProperty.IDENTITY);
		assertEquals(new Long(5), id);
		
		testObject.setProperty(GrailsDomainClassProperty.VERSION,  new Long(5));
		
		Long version = (Long)testObject.getProperty(GrailsDomainClassProperty.VERSION);
		assertEquals(new Long(5), version);		
		
	}
	
	
	public void testToStringInjection() throws Exception {
		GroovyObject testObject = (GroovyObject)ga.getClassLoader()
		.loadClass("TestInjection")
		.newInstance();
		
		testObject.setProperty(GrailsDomainClassProperty.IDENTITY,  new Long(5));
		assertEquals("TestInjection : 5", testObject.toString());
		
		GroovyObject testObject2 = (GroovyObject)ga.getClassLoader()
		.loadClass("PresetIdObject")
		.newInstance();
		
		assertEquals("custom toString()", testObject2.toString());
	}
	
	public void testAssociationInjection() throws Exception {
		GroovyObject testObject = (GroovyObject)ga.getClassLoader()
		.loadClass("TestInjection")
		.newInstance();
		
		Set presets = new HashSet();
		presets.add("blah");
		testObject.setProperty("presets", presets);
		
		assertEquals(presets,testObject.getProperty("presets"));
	}
	
	public void testLoadedLastAssication() throws Exception {
		GroovyObject testObject = (GroovyObject)ga.getClassLoader()
		.loadClass("ZLoadedLast")
		.newInstance();		
		
		BeanWrapper bean = new BeanWrapperImpl(testObject);
		
		assertTrue(bean.isReadableProperty(GrailsDomainClassProperty.IDENTITY));
		assertTrue(bean.isWritableProperty(GrailsDomainClassProperty.IDENTITY));
	}
	
	protected String[] getConfigLocations() {
		return new String[] { "org/codehaus/groovy/grails/injection/injection-resources-tests.xml" };
	}

}
