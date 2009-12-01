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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import grails.util.GrailsUtil

import org.codehaus.groovy.grails.test.junit3.JUnit3GrailsTestType
import org.codehaus.groovy.grails.test.junit3.JUnit3GrailsTestTypeMode
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory
import org.codehaus.groovy.grails.test.report.junit.JUnitReportProcessor

import org.codehaus.groovy.grails.test.GrailsTestType
import org.codehaus.groovy.grails.test.GrailsTestTargetPattern
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.event.GrailsTestEventConsoleReporter

/**
 * Gant script that runs the Grails unit tests
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsRun")
includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsClean")

// Miscellaneous 'switches' that affect test operation
testOptions = [:]

// The four test phases that we can run.
unitTests = [ "unit" ]
integrationTests = [ "integration" ]
functionalTests = []
otherTests = [ "cli" ]

// The potential phases for execution, modify this by responding to the TestPhasesStart event
phasesToRun = ["unit", "integration", "functional", "other"]

TEST_PHASE_WILDCARD = ' _ALL_PHASES_ '
TEST_TYPE_WILDCARD = ' _ALL_TYPES_ '
targetPhasesAndTypes = [:]

// Passed to the test runners to facilitate event publishing
testEventPublisher = new GrailsTestEventPublisher(event)

// Add a listener to write test status updates to the console
eventListener.addGrailsBuildListener(new GrailsTestEventConsoleReporter(System.out))

// Add a listener to generate our JUnit reports.
eventListener.addGrailsBuildListener(new JUnitReportProcessor())

// A list of test names. These can be of any of this forms:
//
//   org.example.*
//   org.example.**.*
//   MyService
//   MyService.testSomeMethod
//   org.example.other.MyService.testSomeMethod
//
// The default pattern runs all tests.
testNames = buildConfig.grails.testing.patterns ?: ['**.*']
testTargetPatterns = null // created in allTests()

// Controls which result formats are generated. By default both XML
// and plain text files are created. You can override this in your
// own scripts.
reportFormats = [ "xml", "plain" ]

// If true, only run the tests that failed before.
reRunTests = false

// Where the report files are created.
testReportsDir = grailsSettings.testReportsDir
// Where the test source can be found
testSourceDir = grailsSettings.testSourceDir

// Set up an Ant path for the tests.
ant.path(id: "grails.test.classpath", testClasspath)

createTestReports = true
compilationFailures = []

testHelper = null
testsFailed = false

target(allTests: "Runs the project's tests.") {
    def dependencies = [compile, packagePlugins]
    if (testOptions.clean) dependencies = [clean] + dependencies
    depends(*dependencies)
    
    packageFiles(basedir)

    ant.mkdir(dir: testReportsDir)
    ant.mkdir(dir: "${testReportsDir}/html")
    ant.mkdir(dir: "${testReportsDir}/plain")

    // If we are to run the tests that failed, replace the list of
    // test names with the failed ones.
    if (reRunTests) testNames = getFailedTests()
    
    testTargetPatterns = testNames.collect { new GrailsTestTargetPattern(it) } as GrailsTestTargetPattern[]
    
    event("TestPhasesStart", [phasesToRun])
    
    // Handle pre 1.2 style testing configuration
    def convertedPhases = [:]
    phasesToRun.each { phaseName ->
        def types = binding."${phaseName}Tests"
        if (types) {
            convertedPhases[phaseName] = types.collect { rawType ->
                if (rawType instanceof CharSequence) {
                    def mode = (phaseName == 'integration') ? JUnit3GrailsTestTypeMode.WITH_GRAILS_ENVIRONMENT : JUnit3GrailsTestTypeMode.NOT_WITH_GRAILS_ENVIRONMENT
                    def rawTypeString = rawType.toString()
                    new JUnit3GrailsTestType(rawTypeString, rawTypeString, mode)
                } else {
                    rawType
                }
            }
        }
    }

    // Using targetPhasesAndTypes, filter down convertedPhases into filteredPhases
    filteredPhases = null
    if (targetPhasesAndTypes.size() == 0) {
        filteredPhases = convertedPhases // no type or phase targeting was applied
    } else {
        filteredPhases = [:]
        convertedPhases.each { phaseName, types ->
            if (targetPhasesAndTypes.containsKey(phaseName) || targetPhasesAndTypes.containsKey(TEST_PHASE_WILDCARD)) {
                def targetTypesForPhase = (targetPhasesAndTypes[phaseName] ?: []) + (targetPhasesAndTypes[TEST_PHASE_WILDCARD] ?: [])
                types.each { type ->
                    if (type.name in targetTypesForPhase || TEST_TYPE_WILDCARD in targetTypesForPhase) {
                        if (!filteredPhases.containsKey(phaseName)) filteredPhases[phaseName] = []
                        filteredPhases[phaseName] << type
                    }
                }
            }
        }
    }
    
    try {
        // Process the tests in each phase that is configured to run.
        filteredPhases.each { phase, types ->
            currentTestPhaseName = phase
            
            // Add a blank line before the start of this phase so that it
            // is easier to distinguish
            println()

            event("StatusUpdate", ["Starting $phase test phase"])
            event("TestPhaseStart", [phase])

            "${phase}TestPhasePreparation"()

            // Now run all the tests registered for this phase.
            types.each(processTests)

            // Perform any clean up required.
            this."${phase}TestPhaseCleanUp"()

            event("TestPhaseEnd", [phase])
            currentTestPhaseName = null
        }
    } finally {
        String msg = testsFailed ? "\nTests FAILED" : "\nTests PASSED"
        if (createTestReports) {
            event("TestProduceReports", [])
            msg += " - view reports in ${testReportsDir}"
        }
        event("StatusFinal", [msg])
        event("TestPhasesEnd", [])
    }

    testsFailed ? 1 : 0
}

/**
 * Compiles and runs all the tests of the given type and then generates
 * the reports for them.
 * @param type The type of the tests to compile (not the test phase!)
 * For example, "unit", "jsunit", "webtest", etc.
 */
processTests = { GrailsTestType type ->
    currentTestTypeName = type.name
    
    def relativePathToSource = type.relativeSourcePath
    def dest = null
    if (relativePathToSource) {
        def source = new File("${testSourceDir}", relativePathToSource)
        if (!source.exists()) return // no source, no point continuing

        dest = new File(grailsSettings.testClassesDir, relativePathToSource)
        compileTests(type, source, dest)
    }
    
    runTests(type, dest)
    currentTestTypeName = null
}

/**
 * Compiles all the test classes for a particular type of test, for
 * example "unit" or "webtest". Assumes that the source files are in
 * the "test/$type" directory. It also compiles the files to distinct
 * directories for each test type: "$testClassesDir/$type".
 * @param type The type of the tests to compile (not the test phase!)
 * For example, "unit", "jsunit", "webtest", etc.
 */
compileTests = { GrailsTestType type, File source, File dest ->
    event("TestCompileStart", [type])

    ant.mkdir(dir: dest.path)
    try {
        def classpathId = "grails.test.classpath"
        ant.groovyc(destdir: dest, encoding:"UTF-8", classpathref: classpathId) {
            javac(classpathref: classpathId, debug: "yes")
            src(path: source)
        }
    } catch (Exception e) {
        event("StatusFinal", ["Compilation error compiling [$type.name] tests: ${e.message}"])
        exit 1
    }

    event("TestCompileEnd", [type])
}

runTests = { GrailsTestType type, File compiledClassesDir ->
    def testCount = type.prepare(testTargetPatterns, compiledClassesDir, binding)
    
    if (testCount) {
        try {
            event("TestSuiteStart", [type])
            println ""
            println "-------------------------------------------------------"
            println "Running ${testCount} $type.name test${testCount > 1 ? 's' : ''}..."

            def start = new Date()
            def result = type.run(testEventPublisher)
            def end = new Date()
            
            event("StatusUpdate", ["Tests Completed in ${end.time - start.time}ms"])

            if (result.failCount > 0) testsFailed = true
            
            println "-------------------------------------------------------"
            println "Tests passed: ${result.passCount}"
            println "Tests failed: ${result.failCount}"
            println "-------------------------------------------------------"
            event("TestSuiteEnd", [type])
        } catch (Exception e) {
            event("StatusFinal", ["Error running $type.name tests: ${e.toString()}"])
            GrailsUtil.deepSanitize(e)
            e.printStackTrace()
            testsFailed = true
        } finally {
            type.cleanup()
        }
    }
}

unitTestPhasePreparation = {}
unitTestPhaseCleanUp = {}

/**
 * Initialises a persistence context and bootstraps the application.
 */
integrationTestPhasePreparation = {
    packageTests()
    bootstrap()

    // Get the Grails application instance created by the bootstrap
    // process.
    def app = appCtx.getBean(GrailsApplication.APPLICATION_ID)
    if (app.parentContext == null) {
        app.applicationContext = appCtx
    }

    appCtx.getBeansOfType(PersistenceContextInterceptor).values()*.init()

    def servletContext = classLoader.loadClass("org.springframework.mock.web.MockServletContext").newInstance()
    GrailsConfigUtils.configureServletContextAttributes(servletContext, app, pluginManager, appCtx) 
    GrailsConfigUtils.executeGrailsBootstraps(app, appCtx, servletContext)
}

/**
 * Shuts down the bootstrapped Grails application.
 */
integrationTestPhaseCleanUp = {
    // Kill any context interceptor we might have.
    appCtx.getBeansOfType(PersistenceContextInterceptor).values()*.destroy()

    shutdownApp()
}

/**
 * Starts up the test server.
 */
functionalTestPhasePreparation = {
    packageApp()
    runApp()
}

/**
 * Shuts down the test server.
 */
functionalTestPhaseCleanUp = {
    stopServer()
}

otherTestPhasePreparation = {}
otherTestPhaseCleanUp = {}


target(packageTests: "Puts some useful things on the classpath for integration tests.") {
    ant.copy(todir: new File(grailsSettings.testClassesDir, "integration").path) {
        fileset(dir: "${basedir}", includes: metadataFile.name)
    }
    ant.copy(todir: grailsSettings.testClassesDir.path, failonerror: false) {
        fileset(dir: "${basedir}/grails-app/conf", includes: "**", excludes: "*.groovy, log4j*, hibernate, spring")
        fileset(dir: "${basedir}/grails-app/conf/hibernate", includes: "**/**")
        fileset(dir: "${grailsSettings.sourceDir}/java") {
            include(name: "**/**")
            exclude(name: "**/*.java")
        }
        fileset(dir: "${testSourceDir}/unit") {
            include(name: "**/**")
            exclude(name: "**/*.java")
            exclude(name: "**/*.groovy")
        }
        fileset(dir: "${testSourceDir}/integration") {
            include(name: "**/**")
            exclude(name: "**/*.java")
            exclude(name: "**/*.groovy")
        }
    }
}

def getFailedTests() {
    File file = new File("${testReportsDir}/TESTS-TestSuites.xml")
    if (file.exists()) {
        def xmlParser = new XmlParser().parse(file)
        def failedTests = xmlParser.testsuite.findAll { it.'@failures' =~ /.*[1-9].*/ || it.'@errors' =~ /.*[1-9].*/}

        return failedTests.collect {
            String testName = it.'@name'
            testName = testName.replace('Tests', '')
            def pkg = it.'@package'
            if (pkg) {
                testName = pkg + '.' + testName
            }
            return testName
        }
    } else {
        return []
    }
}
