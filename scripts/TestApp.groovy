/*
 * Copyright 2004-2005 the original author or authors.
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

/**
 * Gant script that runs the Grails unit tests
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import grails.util.GrailsUtil as GU;  
import grails.util.GrailsWebUtil as GWU
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.*
import java.lang.reflect.Modifier;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;        
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator as GRC;




Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Run a Grails applications unit tests") {      
	depends( classpath )
	grailsEnv = "test"
	packageApp()
	testApp()
}            

task(testApp:"The test app implementation task") {               
	def result = null
	try {        
		def testFiles = resolveResources("grails-tests/*.groovy")
		if(testFiles.size() == 0) {
			println "No tests found in grails-test to execute"
			System.exit(-1)
		}
				
		def ctx = GU.bootstrapGrailsFromClassPath()
		
		def app = ctx.getBean(GrailsApplication.APPLICATION_ID)
		def classLoader = app.classLoader
		
		def suite = new TestSuite()
		
		GWU.bindMockWebRequest(ctx)
		                   

		testFiles.each { r ->
			def c = classLoader.parseClass(r.file)
			if(TestCase.isAssignableFrom(c) && !Modifier.isAbstract(c.modifiers)) {
				suite.addTest(new GrailsTestSuite(ctx.beanFactory, c))
			}			
		}
		
		def beanNames = ctx.getBeanNamesForType(PersistenceContextInterceptor)
		def interceptor = null
		if(beanNames.size() > 0)interceptor = ctx.getBean(beanNames[0])
							
		
		try {
			interceptor?.init()      
			result = TestRunner.run(suite)
			
		}   
		finally {
			interceptor?.destroy()
		} 							
		
	}   
	catch(Exception e) {
		println "Error executing tests ${e.message}"
		e.printStackTrace(System.out)   
		println "Tests failed with exception!!"
		System.exit(1)
	}
	finally {
		if(result) { 
			if(result.errorCount() > 0 || result.failureCount() > 0) {
				println "Test Failures!!!"
				System.exit(1)				
			}			       

		}
	}	
}
