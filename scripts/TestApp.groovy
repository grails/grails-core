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
	depends( classpath, checkVersion )
	grailsEnv = "test"
	packageApp()
	testApp()
}            

task(testApp:"The test app implementation task") {               
	//runCompiledTests()	
	runGrailsTests()
}                     

task(runCompiledTests:"Runs the tests located under src/test which are compiled then executed") {
	compileTests()
	Ant.mkdir(dir:"${basedir}/target/test-reports")
	Ant.mkdir(dir:"${basedir}/target/test-reports/html")	
	Ant.junit(fork:true, forkmode:"once") {
		jvmarg(value:"-Xmx256M")

		formatter(type:"xml")
		batchtest(todir:"${basedir}/target/test-reports") {
			fileset(dir:"${basedir}/target/test-classes", includes:"**/*Tests.class")
		}
	} 
	Ant.junitreport {
		fileset(dir:"${basedir}/target/test-reports") {
			include(name:"TEST-*.xml")
			report(format:"frames", todir:"${basedir}/target/test-reports/html")
		}
	}
	event("StatusFinal", ["Compiled tests complete"])
}

task(runGrailsTests:"Runs Grails' tests under the grails-test directory") {
	def result = null
	try {        
		def testFiles = resolveResources("grails-tests/*.groovy")
		if(testFiles.size() == 0) {
			println "No tests found in grails-test to execute"
			exit(0)
		}
				
		def ctx = GU.bootstrapGrailsFromClassPath()
		
		def app = ctx.getBean(GrailsApplication.APPLICATION_ID)
		def classLoader = app.classLoader

        def resources = app.resourceLoader.resources as ArrayList
        testFiles.each() { resources << it }
		app.resourceLoader.resources = resources
        		
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
	catch(Throwable e) {
		println "Error executing tests ${e.message}"
		e.printStackTrace(System.out)   
        event("StatusFinal", ["Error running tests: ${e.toString()}"])
		exit(1)
	}
	finally {
		if(result) { 
			if(result.errorCount() > 0 || result.failureCount() > 0) {
				println "Test Failures!!!"
            	event("StatusFinal", ["Tests failed: ${result.errorCount()} errors, ${result.failureCount()} failures"])
				exit(1)
			}
			else {
            	event("StatusFinal", ["Tests passed"])
				exit(0)
			}			       

		}  
		else {
            event("StatusFinal", ["Tests passed"])
			exit(0)
		}
	}
}
