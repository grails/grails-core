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
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionStatus
import org.apache.commons.logging.LogFactory


Ant.property(environment: "env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
grailsApp = null
appCtx = null
result = new TestResult()
compilationFailures = []

includeTargets << new File("${grailsHome}/scripts/Bootstrap.groovy")

generateLog4jFile = true

target('default': "Run a Grails applications unit tests") {
    depends(classpath, checkVersion, configureProxy)

    testApp()
}

testDir = "${basedir}/test/reports"

def processResults = {
    if (result) {
        if (result.errorCount() > 0 || result.failureCount() > 0 || compilationFailures.size > 0) {
            event("StatusFinal", ["Tests failed: ${result.errorCount()} errors, ${result.failureCount()} failures, ${compilationFailures.size} compilation errors. View reports in $testDir"])
            exit(1)
        }
        else {
            event("StatusFinal", ["Tests passed. View reports in $testDir"])
            exit(0)
        }

    }
    else {
        event("StatusFinal", ["Tests passed. View reports in $testDir"])
        exit(0)
    }
}

unitOnly = false
integrationOnly = false

target(testApp: "The test app implementation target") {
    depends(packageApp)

    if(config.grails.testing.reports.destDir) {
        testDir = config.grails.testing.reports.destDir
    }

    Ant.mkdir(dir: testDir)
    Ant.mkdir(dir: "${testDir}/html")
    Ant.mkdir(dir: "${testDir}/plain")

    if(args?.indexOf('-unit') >-1) {
        args -= '-unit'
        unitOnly = true
    }
    if(args?.indexOf('-integration') >-1) {
        args -= '-integration'
        integrationOnly = true
    }


    compileTests()
	packageTests()

    try {
    	event("AllTestsStart", ["Starting test-app"])
        if(!integrationOnly) {
            runUnitTests()
        }
        if(!unitOnly) {
            runIntegrationTests()
        }   
        event("AllTestsEnd", ["Finishing test-app"])
        produceReports()
    }
    catch (Exception ex) {
        ex.printStackTrace()
    }
    finally {
        processResults()
    }
}
target(packageTests:"Puts some useful things on the classpath") {
    Ant.copy(todir:testDirPath) {
		fileset(dir:"${basedir}", includes:"application.properties")
	}					
	Ant.copy(todir:testDirPath, failonerror:false) {
		fileset(dir:"${basedir}/grails-app/conf", includes:"**", excludes:"*.groovy, log4j*, hibernate, spring")
		fileset(dir:"${basedir}/grails-app/conf/hibernate", includes:"**/**")
		fileset(dir:"${basedir}/src/java") {
			include(name:"**/**")
			exclude(name:"**/*.java")
		}
		fileset(dir:"${basedir}/test/unit") {
            include(name:"**/**")
            exclude(name:"**/*.java")
            exclude(name:"**/*.groovy)")
        }
        fileset(dir:"${basedir}/test/integration") {
            include(name:"**/**")
            exclude(name:"**/*.java")
            exclude(name:"**/*.groovy)")
        }
	}           
	
}
target(compileTests: "Compiles the test cases") {
    event("CompileStart", ['tests'])

    def destDir = testDirPath
    Ant.mkdir(dir: destDir)
    try {
        Ant.groovyc(destdir: destDir,
                projectName:grailsAppName,
                classpathref: "grails.classpath",
                resourcePattern: "file:${basedir}/**/grails-app/**/*.groovy",
                compilerClasspath.curry(true))
    }
    catch (Exception e) {
        event("StatusFinal", ["Compilation Error: ${e.message}"])
        exit(1)
    }

	classLoader = new URLClassLoader([new File(destDir).toURI().toURL()] as URL[],getClass().classLoader.rootLoader)
	Thread.currentThread().contextClassLoader = classLoader

    event("CompileEnd", ['tests'])
}

target(produceReports: "Outputs aggregated xml and html reports") {
    Ant.junitreport(todir: "${testDir}") {
        fileset(dir: testDir) {
            include(name: "TEST-*.xml")
        }
        report(format: "frames", todir: "${testDir}/html")
    }
}


def populateTestSuite = {suite, testFiles, classLoader, ctx, String base ->
    for (r in testFiles) {
        try {
            def fileName = r.URL.toString()
            def endIndex = -8
            if (fileName.endsWith(".java")) {
                endIndex = -6
            }
            def className = fileName[fileName.indexOf(base) + base.size()..endIndex].replace('/' as char, '.' as char)
            def c = classLoader.loadClass(className)
            if (TestCase.isAssignableFrom(c) && !Modifier.isAbstract(c.modifiers)) {
                suite.addTest(new GrailsTestSuite(ctx, c))
            }
            else {
                event("StatusUpdate", ["Test ${r.filename} is not a valid test case. It does not implement junit.framework.TestCase or is abstract!"])
            }
        } catch (Exception e) {
            compilationFailures << r.file.name
            event("StatusFinal", ["Error loading test: ${e.message}"])
            exit(1)
        }
    }
}
def runTests = {suite, TestResult result, Closure callback ->
    for (TestSuite test in suite.tests()) {
        new File("${testDir}/TEST-${test.name}.xml").withOutputStream {xmlOut ->
            new File("${testDir}/plain/TEST-${test.name}.txt").withOutputStream {plainOut ->

                def savedOut = System.out
                def savedErr = System.err

                try {
                    def outBytes = new ByteArrayOutputStream()
                    def errBytes = new ByteArrayOutputStream()
                    System.out = new PrintStream(outBytes)
                    System.err = new PrintStream(errBytes)
                    def xmlOutput = new XMLJUnitResultFormatter(output: xmlOut)
                    def plainOutput = new PlainJUnitResultFormatter(output: plainOut)
                    def junitTest = new JUnitTest(test.name)
                    plainOutput.startTestSuite(junitTest)
                    xmlOutput.startTestSuite(junitTest)
                    savedOut.println "Running test ${test.name}..."
                    def start = System.currentTimeMillis()
                    def runCount = 0
                    def failureCount = 0
                    def errorCount = 0

                    for (i in 0..<test.testCount()) {
                        def thisTest = new TestResult()
                        thisTest.addListener(xmlOutput)
                        thisTest.addListener(plainOutput)
                        def t = test.testAt(i)
                        System.out.println "--Output from ${t.name}--"
                        System.err.println "--Output from ${t.name}--"
                        
                        callback(test, {
                            savedOut.print "                    ${t.name}..."
                            event("TestStart", [test, t, thisTest])
                            test.runTest (t, thisTest)
                            event("TestEnd", [test, t, thisTest])
                            thisTest
                        })
                        runCount += thisTest.runCount()
                        failureCount += thisTest.failureCount()
                        errorCount += thisTest.errorCount()
                        
                        if (thisTest.errorCount() > 0 || thisTest.failureCount() > 0) {
                            savedOut.println "FAILURE"
                            thisTest.errors().each {result.addError(t, it.thrownException())}
                            thisTest.failures().each {result.addFailure(t, it.thrownException())}
                        }
                        else {savedOut.println "SUCCESS"}
                    } 
                    junitTest.setCounts(runCount, failureCount, errorCount);
                    junitTest.setRunTime(System.currentTimeMillis() - start)
                    
                    def outString = outBytes.toString()
                    def errString = errBytes.toString()
                    new File("${testDir}/TEST-${test.name}-out.txt").write(outString)
                    new File("${testDir}/TEST-${test.name}-err.txt").write(errString)
                    plainOutput.setSystemOutput(outString)
                    plainOutput.setSystemError(errString)
                    plainOutput.endTestSuite(junitTest)
                    xmlOutput.setSystemOutput(outString)
                    xmlOutput.setSystemError(errString)
                    xmlOutput.endTestSuite(junitTest)
                } finally {
                    System.out = savedOut
                    System.err = savedErr
                }

            }
        }
    }
}
target(runUnitTests: "Run Grails' unit tests under the test/unit directory") {
    try {
        loadApp()

        def testFiles = resolveTestResources {"test/unit/${it}.groovy"}
        testFiles.addAll(resolveTestResources {"test/unit/${it}.java"})
        testFiles = testFiles.findAll {it.exists()}
        if (testFiles.size() == 0) {
            event("StatusUpdate", ["No tests found in test/unit to execute"])
            return
        }

        def suite = new TestSuite()
		classLoader.rootLoader.addURL(new File("test/unit").toURI().toURL())
        populateTestSuite(suite, testFiles, classLoader, appCtx, "test/unit/")
        if (suite.testCount() > 0) {

            event("TestSuiteStart", ["unit"])
            int testCases = suite.countTestCases()
            println "-------------------------------------------------------"
            println "Running ${testCases} Unit Test${testCases > 1 ? 's' : ''}..."

            def start = new Date()
            runTests(suite, result) {test, invocation ->
                for(cls in grailsApp.allArtefacts) {
                    def emc = new ExpandoMetaClass(cls, true, true)
                    emc.initialize()
                    def log = LogFactory.getLog(cls)
                    emc.getLog = {-> log }
                    GroovySystem.metaClassRegistry.setMetaClass(cls, emc)
                }
                invocation()
            }
            def end = new Date()

            event("TestSuiteEnd", ["unit",suite])
            event("StatusUpdate", ["Unit Tests Completed in ${end.time - start.time}ms"])
            println "-------------------------------------------------------"
        }
    }
    catch (Exception e) {
        event("StatusFinal", ["Error running unit tests: ${e.toString()}"])
        e.printStackTrace()
    }
}

target(runIntegrationTests: "Runs Grails' tests under the test/integration directory") {
    try {
        // allow user to specify test to run like this...
        //   grails test-app Author
        //   grails test-app AuthorController
        def testFiles = resolveTestResources {"test/integration/${it}.groovy"}
        testFiles.addAll(resolveTestResources {"test/integration/${it}.java"})

        if (testFiles.size() == 0) {
            event("StatusUpdate", ["No tests found in test/integration to execute"])
            return
        }

        if(integrationOnly) {
            loadApp()
        }
        configureApp()
        def app = appCtx.getBean(GrailsApplication.APPLICATION_ID)
        if (app.parentContext == null) {
            app.applicationContext = appCtx
        }
        def classLoader = app.classLoader
        def suite = new TestSuite()

        populateTestSuite(suite, testFiles, classLoader, appCtx, "test/integration/")
        if (suite.testCount() > 0) {
            int testCases = suite.countTestCases()
            println "-------------------------------------------------------"
            println "Running ${testCases} Integration Test${testCases > 1 ? 's' : ''}..."

            event("TestSuiteStart", ["integration"])

            def beanNames = appCtx.getBeanNamesForType(PersistenceContextInterceptor)
            def interceptor = null
            if (beanNames.size() > 0) interceptor = appCtx.getBean(beanNames[0])


            try {
                interceptor?.init()

                def start = new Date()
                
                def savedOut = System.out
                runTests(suite, result) {test, invocation ->
                    name = test.name[0..-6]
                    def webRequest = GWU.bindMockWebRequest(appCtx)
                    webRequest.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)

                    // @todo this is horrible and dirty, should find a better way
                    if (name.endsWith("Controller")) {
                        webRequest.controllerName = GCU.getLogicalPropertyName(name, "Controller")
                    }
                    else {
                        // Provide a default 'current' controller name.
                        webRequest.controllerName = "test"
                    }

					def callable = { status ->
                        invocation()                 
                        status?.setRollbackOnly()
                    }
					if(test.isTransactional()) {
                        if (appCtx.transactionManager) {
                            def template = new TransactionTemplate(appCtx.transactionManager)
                    	    template.execute( callable as TransactionCallback )
                        } else {
                            System.out = savedOut                            
                            println "Error: There is no test datasource defined and integration test ${test.name} does not set transactional = false"
                            println "Tests aborted"
                            exit(1)
                        }
					}
					else {
						callable.call()
					}
                    RequestContextHolder.setRequestAttributes(null);
                }
                def end = new Date()
                
                event("TestSuiteEnd", ["integration",suite])
                println "Integration Tests Completed in ${end.time - start.time}ms"
                println "-------------------------------------------------------"

            }
            finally {
                interceptor?.destroy()
            }
        }
    }
    catch (Throwable e) {
        event("StatusUpdate", ["Error executing tests ${e.message}"])
        e.printStackTrace(System.out)
        event("StatusFinal", ["Error running tests: ${e.toString()}"])
        exit(1)
    }
    finally {
        shutdownApp()
    }
}

def resolveTestResources(patternResolver) {
    def testNames = getTestNames(args)

    if (!testNames) {
        testNames = config.grails.testing.patterns ?: ['**/*']
    }

    def testResources = []
    testNames.each {
        def testFiles = resolveResources(patternResolver(it))
        testResources.addAll(testFiles.findAll {it.exists()})
    }
    testResources
}

def getTestNames(testNamesString) {
    // If a list of test class names is provided, split it into ant
    // file patterns.
    def nameSuffix = 'Tests'
    if (config.grails.testing.nameSuffix) {
        nameSuffix = config.grails.testing.nameSuffix
    }

    if (testNamesString) {
        testNamesString = testNamesString.split(/\s+/).collect {
            // If the test name includes a package, replace it with the
            // corresponding file path.
            if (it.indexOf('.') != -1) {
                it = it.replace('.' as char, '/' as char)
            }
            else {
                // Allow the test class to be in any package.
                it = "**/$it"
            }
            return "${it}${nameSuffix}"
        }
    }

    return testNamesString
}
