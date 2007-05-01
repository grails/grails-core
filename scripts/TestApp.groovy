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
import org.apache.tools.ant.taskdefs.optional.junit.*

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Run a Grails applications unit tests") {
	depends( classpath, checkVersion )
	grailsEnv = "test"
	packageApp()
	testApp()
}

testDir = "${basedir}/target/test-reports"

task(testApp:"The test app implementation task") {
	//runCompiledTests()
	Ant.mkdir(dir:testDir)
	Ant.mkdir(dir:"${testDir}/html")
	Ant.mkdir(dir:"${testDir}/plain")

	runGrailsTests()
}

task(runCompiledTests:"Runs the tests located under src/test which are compiled then executed") {
	compileTests()
	Ant.junit(fork:true, forkmode:"once") {
		jvmarg(value:"-Xmx256M")

		formatter(type:"xml")
		batchtest(todir:"${basedir}/target/test-reports") {
			fileset(dir:"${basedir}/target/test-classes", includes:"**/*Tests.class")
		}
	}
	Ant.junitreport(tofile:"${testDir}/TEST-results.xml") {
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
	    // allow user to specify test to run like this...
	    // grails test-app Author
	    // grails test-app AuthorController
	    def testCaseToRun = '*'
	    if (args) {
	        testCaseToRun = "${args}Tests"
	    }
		def testFiles = resolveResources("grails-tests/${testCaseToRun}.groovy")
		if(testFiles.size() == 0) {
            event("StatusFinal", [ "No tests found in grails-test to execute"])
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

        result = new TestResult()
		try {
			interceptor?.init()

			suite.tests().each { test ->
				def thisTest = new TestResult()
				new File("${testDir}/TEST-${test.name}.xml").withOutputStream { xmlOut ->
					new File("${testDir}/plain/TEST-${test.name}.txt").withOutputStream { plainOut ->
						def xmlOutput = new XMLJUnitResultFormatter(output:xmlOut)
						def plainOutput = new PlainJUnitResultFormatter(output:plainOut)
						def junitTest = new JUnitTest(test.name)
						thisTest.addListener(xmlOutput)
						thisTest.addListener(plainOutput)

						plainOutput.startTestSuite(junitTest)
						xmlOutput.startTestSuite(junitTest)
						print "Running test ${test.name}..."
						suite.runTest(test, thisTest)
						plainOutput.endTestSuite(junitTest)
						xmlOutput.endTestSuite(junitTest)
						if(thisTest.errorCount() > 0 || thisTest.failureCount() > 0) {
							println "FAILURE"
							thisTest.errors().each { result.addError(test, it.thrownException())  }
							thisTest.failures().each { result.addFailure(test, it.thrownException()) }
						}
						else { println "SUCCESS"}
						app.domainClasses.each { dc ->
							dc.clazz.executeUpdate("delete from ${dc.clazz.name}")
						}
					}
				}
				interceptor?.flush()
			}
		}
		finally {
			interceptor?.destroy()
		}
		
		Ant.junitreport {
			fileset(dir:testDir) {
				include(name:"TEST-*.xml")
			}
			report(format:"frames", todir:"${basedir}/target/test-reports/html")
		}


	}
	catch(Throwable e) {
        event("StatusUpdate", [ "Error executing tests ${e.message}"])
		e.printStackTrace(System.out)   
        event("StatusFinal", ["Error running tests: ${e.toString()}"])
		exit(1)
	}
	finally {
		if(result) { 
			if(result.errorCount() > 0 || result.failureCount() > 0) {
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
