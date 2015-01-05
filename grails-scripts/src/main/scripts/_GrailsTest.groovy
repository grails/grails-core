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
import org.codehaus.groovy.grails.cli.fork.testing.ForkedGrailsTestRunner
import org.codehaus.groovy.grails.support.PersistenceContextInterceptorExecutor
import org.codehaus.groovy.grails.test.runner.GrailsProjectTestRunner
import org.codehaus.groovy.grails.test.runner.phase.FunctionalTestPhaseConfigurer
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer

/**
 * Gant script that runs the Grails unit tests
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsRun")
includeTargets << grailsScript("_GrailsClean")

projectTestRunner = new GrailsProjectTestRunner(projectPackager)
projectTestRunner.testExecutionContext = binding
// Miscellaneous 'switches' that affect test operation
testOptions = projectTestRunner.testOptions

// The four test phases that we can run.
unitTests = projectTestRunner.testFeatureDiscovery.unitTests
integrationTests = projectTestRunner.testFeatureDiscovery.integrationTests
functionalTests = projectTestRunner.testFeatureDiscovery.functionalTests
otherTests = projectTestRunner.testFeatureDiscovery.otherTests

// The potential phases for execution, modify this by responding to the TestPhasesStart event
phasesToRun = projectTestRunner.phasesToRun

TEST_PHASE_WILDCARD = GrailsProjectTestRunner.TEST_PHASE_WILDCARD
TEST_TYPE_WILDCARD = GrailsProjectTestRunner.TEST_TYPE_WILDCARD

targetPhasesAndTypes = projectTestRunner.targetPhasesAndTypes // Passed to the test runners to facilitate event publishing
testEventPublisher = projectTestRunner.testEventPublisher // Add a listener to generate our JUnit reports.

// A list of test names. These can be of any of this forms:
//
//   org.example.*
//   org.example.**.*
//   MyService
//   MyService.testSomeMethod
//   org.example.other.MyService.testSomeMethod
//
// The default pattern runs all tests.
testNames = projectTestRunner.testNames

// Controls which result formats are generated. By default both XML
// and plain text files are created. You can override this in your
// own scripts.
reportFormats = projectTestRunner.reportFormats

// If true, only run the tests that failed before.
reRunTests = projectTestRunner.reRunTests

// Where the report files are created.
testReportsDir = grailsSettings.testReportsDir
// Where the test source can be found
testSourceDir = grailsSettings.testSourceDir

junitReportStyleDir = projectTestRunner.junitReportStyleDir

createTestReports = true

testsFailed = false
projectWatcher = null

target(allTests: "Runs the project's tests.") {
    depends(compile, startLogging, packagePlugins, configureServerContextPath)
    Integer exitCode
    if(grailsSettings.forkSettings.test) {
        if(argsMap?.war) {
            projectRunner.warCreator.packageWar()
        }
        def forkedTestRunner = new ForkedGrailsTestRunner(grailsSettings)
        if(grailsSettings.forkSettings.test instanceof Map) {
            forkedTestRunner.configure(grailsSettings.forkSettings.test)
        }
        exitCode = forkedTestRunner.fork(argsMap)?.waitFor()
        if(exitCode != null && exitCode != 0) {
            exit(exitCode)
        }
    }
    else {
        exitCode = projectTestRunner.runAllTests(argsMap) ? 0 : 1
    }
}

/**
 * Compiles and runs all the tests of the given type and then generates
 * the reports for them.
 * @param type The type of the tests to compile (not the test phase!)
 * For example, "unit", "jsunit", "webtest", etc.
 */
processTests = projectTestRunner.&processTests

/**
 * Compiles all the test classes for a particular type of test, for
 * example "unit" or "webtest". Assumes that the source files are in
 * the "test/$type" directory. It also compiles the files to distinct
 * directories for each test type: "$testClassesDir/$type".
 * @param type The type of the tests to compile (not the test phase!)
 * For example, "unit", "jsunit", "webtest", etc.
 */
compileTests = projectTestRunner.projectTestCompiler.&compileTests

runTests = projectTestRunner.&runTests

initPersistenceContext = {
    if (appCtx != null) {
        PersistenceContextInterceptorExecutor.initPersistenceContext(appCtx)
    }
}

destroyPersistenceContext = {
    if (binding.variables.containsKey("appCtx") && appCtx != null) {
        PersistenceContextInterceptorExecutor.destroyPersistenceContext(appCtx)
    }
}

unitTestPhasePreparation = {}
unitTestPhaseCleanUp = {}

/**
 * Initialises a persistence context and bootstraps the application.
 */
def integrationPhaseConfigurer = new IntegrationTestPhaseConfigurer(projectTestRunner.projectTestCompiler, projectLoader)
projectTestRunner.testFeatureDiscovery.configurers.integration = integrationPhaseConfigurer
integrationTestPhasePreparation = {
    integrationPhaseConfigurer.prepare()
}

/**
 * Shuts down the bootstrapped Grails application.
 */
integrationTestPhaseCleanUp = {
    integrationPhaseConfigurer.cleanup()
}

def functionalPhaseConfigurer = new FunctionalTestPhaseConfigurer(projectRunner)
projectTestRunner.testFeatureDiscovery.configurers.functional = functionalPhaseConfigurer
/**
 * Starts up the test server.
 */
functionalTestPhasePreparation = {
    functionalPhaseConfigurer.prepare()
}

/**
 * Shuts down the test server.
 */
functionalTestPhaseCleanUp = {
    functionalPhaseConfigurer.cleanup()
}

otherTestPhasePreparation = {}
otherTestPhaseCleanUp = {}

target(packageTests: "Puts some useful things on the classpath for integration tests.") {
    projectTestRunner.projectTestCompiler.packageTests(false)
}
