/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.test.runner

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.project.creation.GrailsProjectCleaner
import org.codehaus.groovy.grails.project.packaging.GrailsProjectPackager
import org.codehaus.groovy.grails.test.GrailsTestTargetPattern
import org.codehaus.groovy.grails.test.GrailsTestType
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.junit4.JUnit4GrailsTestType
import org.codehaus.groovy.grails.test.report.junit.JUnitReportProcessor
import org.codehaus.groovy.grails.test.runner.phase.TestFeatureDiscoverySupport
import org.codehaus.groovy.grails.test.runner.phase.TestPhaseConfigurer
import org.codehaus.groovy.grails.test.support.GrailsTestMode

/**
 *
 * The default test runner that runs Grails tests. By default configured to run only unit tests. To run other kinds of tests (functional, integration etc.) additional {@link TestPhaseConfigurer} instances need
 * to be registered with the #testFeatureDiscovery property
 *
 *
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */
class GrailsProjectTestRunner extends BaseSettingsApi{

    private static final GrailsConsole CONSOLE = GrailsConsole.getInstance()

    public static final String TEST_PHASE_AND_TYPE_SEPARATOR = ':'

    // Miscellaneous 'switches' that affect test operation
    public static final String TEST_PHASE_WILDCARD = ' _ALL_PHASES_ '
    public static final String TEST_TYPE_WILDCARD = ' _ALL_TYPES_ '

    Map testOptions = [echoOut:false, echoErr:false]

    // The potential phases for execution, modify this by responding to the TestPhasesStart event
    TestFeatureDiscoverySupport testFeatureDiscovery = new TestFeatureDiscoverySupport()

    List<String> phasesToRun = ["unit", "integration", "functional", "other"]
    Map<String, List<String>> targetPhasesAndTypes = [:].withDefault { [] }

    // Passed to the test runners to facilitate event publishing
    GrailsTestEventPublisher testEventPublisher

    // A list of test names. These can be of any of this forms:
    //
    //   org.example.*
    //   org.example.**.*
    //   MyService
    //   MyService.testSomeMethod
    //   org.example.other.MyService.testSomeMethod
    //
    // The default pattern runs all tests.
    List<String> testNames
    Collection<GrailsTestTargetPattern> testTargetPatterns = null // created in allTests()

    // Controls which result formats are generated. By default both XML
    // and plain text files are created. You can override this in your
    // own scripts.
    List<String> reportFormats = [ "xml", "plain" ]

    // If true, only run the tests that failed before.
    boolean reRunTests = false

    // Where the report files are created.
    File testReportsDir
    // Where the test source can be found
    File testSourceDir




    File junitReportStyleDir
    boolean createTestReports = true
    boolean testsFailed = false
    GrailsProjectWatcher projectWatcher = null
    GrailsProjectPackager projectPackager
    AntBuilder ant
    GrailsProjectTestCompiler projectTestCompiler
    Binding testExecutionContext = new Binding()

//    @CompileStatic        TODO: Report Groovy bug. Uncommenting causes java.lang.ArrayIndexOutOfBoundsException during compilation
    GrailsProjectTestRunner(GrailsProjectPackager projectPackager) {
        super(projectPackager.buildSettings, projectPackager.buildEventListener, projectPackager.isInteractive)

        this.projectPackager = projectPackager
        this.projectTestCompiler = new GrailsProjectTestCompiler(projectPackager.buildEventListener, projectPackager.pluginBuildSettings, projectPackager.classLoader)
        this.ant = projectPackager.ant
        // The 'styledir' argument to the 'junitreport' ant task (null == default provided by Ant)
        if (buildSettings.grailsHome) {
            junitReportStyleDir = new File(buildSettings.grailsHome, "src/resources/tests")
            if (!junitReportStyleDir.exists()) {
                junitReportStyleDir = new File(buildSettings.grailsHome, "grails-resources/src/grails/home/tests")
            }

        }

        testReportsDir = buildSettings.testReportsDir
        testSourceDir = buildSettings.testSourceDir

        // Add a listener to generate our JUnit reports.
        buildEventListener.addGrailsBuildListener(new JUnitReportProcessor())
        testEventPublisher = new GrailsTestEventPublisher(buildEventListener)

        this.testNames = lookupTestPatterns()

        // initialize the default binding
        final context = testExecutionContext
        initialiseContext(context)
    }

    @CompileStatic
    void initialiseContext(Binding context) {
        context.setVariable("grailsSettings", this.projectPackager.buildSettings)
        context.setVariable("testOptions", testOptions)
        context.setVariable("classLoader", Thread.currentThread().contextClassLoader)
        context.setVariable("resolveResources", { String pattern ->
            resolveResources(pattern)
        })
        context.setVariable("testReportsDir", this.testReportsDir)
        context.setVariable("reportFormats", reportFormats)
        context.setVariable("ant", ant)
        context.setVariable("unitTests", testFeatureDiscovery.unitTests)
        context.setVariable("integrationTests", testFeatureDiscovery.integrationTests)
        testFeatureDiscovery.testExecutionContext = context
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected List<String> lookupTestPatterns() {
        buildSettings.config.grails.testing.patterns ?: ['**.*']
    }


    @CompileStatic
    void runAllTests(Map<String, String> argsMap, boolean triggerEvents = true) {

        // The test targeting patterns
        List<String> testTargeters = []

        // The params that target a phase and/or type
        List<String> phaseAndTypeTargeters = []

        // Separate the type/phase targeters from the test targeters
        for (String p in argsMap["params"]) {
            List<String> destination = p.contains(TEST_PHASE_AND_TYPE_SEPARATOR) ? phaseAndTypeTargeters : testTargeters
            destination << p
        }

        // If we are targeting tests, set testNames (from _GrailsTest)
        if (testTargeters) testNames = testTargeters

        // treat pre 1.2 phase targeting args as '[phase]:' for backwards compatibility
        for (type in ["unit", "integration", "functional", "other"]) {
            if (argsMap[type]) {
                phaseAndTypeTargeters << "${type}${TEST_PHASE_AND_TYPE_SEPARATOR}"
                argsMap.remove(type) // these are not test "options"
            }
        }

        // process the phaseAndTypeTargeters, populating the targetPhasesAndTypes map from _GrailsTest
        for (String t in phaseAndTypeTargeters) {
            def parts = t.split(TEST_PHASE_AND_TYPE_SEPARATOR, 2)
            String targetPhase = parts[0] ?: TEST_PHASE_WILDCARD
            String targetType = parts[1] ?: TEST_TYPE_WILDCARD

            targetPhasesAndTypes[targetPhase] << targetType
        }

        // Any switch style args are "test options" (from _GrailsTest)
        argsMap.each { String key, value ->
            if (key != 'params') {
                testOptions[key] = value
            }
        }

        if (argsMap["xml"]) {
            reportFormats.clear()
            reportFormats <<  "xml"
            createTestReports = false
        }
        else {
            createTestReports = !argsMap["no-reports"]
        }

        reRunTests = argsMap["rerun"]

        runAllTests(triggerEvents)
    }

    /**
     * Run all tests in a Grails application
     *
     * @param triggerEvents Whether to trigger events on start and finish of the test run (optional)
     **/
    void runAllTests(boolean triggerEvents = true) {
        if (triggerEvents)
            buildEventListener.triggerEvent("AllTestsStart")
        projectPackager.packageApplication()
        if (testOptions.clean) {
            new GrailsProjectCleaner(buildSettings, buildEventListener).cleanAll()
        }


        ant.mkdir(dir: testReportsDir)
        ant.mkdir(dir: "${testReportsDir}/html")
        ant.mkdir(dir: "${testReportsDir}/plain")

        // If we are to run the tests that failed, replace the list of
        // test names with the failed ones.
        if (reRunTests) testNames = getFailedTests()

        // add the test classes to the classpath
        if(projectPackager.classLoader instanceof URLClassLoader) {

            projectPackager.classLoader.addURL(buildSettings.testClassesDir.toURI().toURL())
        }

        testTargetPatterns = testNames.collect { String it -> new GrailsTestTargetPattern(it) } as GrailsTestTargetPattern[]

        buildEventListener.triggerEvent("TestPhasesStart", phasesToRun)


        // Handle pre 1.2 style testing configuration
        def convertedPhases = [:]
        phasesToRun.each { String phaseName ->
            def types = testFeatureDiscovery.findTestType(phaseName)
            if (types) {
                convertedPhases[phaseName] = types.collect { rawType ->
                    if (rawType instanceof CharSequence) {
                        def rawTypeString = rawType.toString()
                        if (phaseName == 'integration') {
                            def mode = new GrailsTestMode(autowire: true, wrapInTransaction: true, wrapInRequestEnvironment: true)
                            new JUnit4GrailsTestType(rawTypeString, rawTypeString, mode)
                        }
                        else {
                            new JUnit4GrailsTestType(rawTypeString, rawTypeString)
                        }
                    }
                    else {
                        rawType
                    }
                }
            }
        }

        // Using targetPhasesAndTypes, filter down convertedPhases into filteredPhases
        Map filteredPhases = null
        if (targetPhasesAndTypes.size() == 0) {
            filteredPhases = convertedPhases // no type or phase targeting was applied
        }
        else {
            filteredPhases = [:]
            convertedPhases.each { phaseName, types ->
                if (targetPhasesAndTypes.containsKey(phaseName) || targetPhasesAndTypes.containsKey(TEST_PHASE_WILDCARD)) {
                    def targetTypesForPhase = (targetPhasesAndTypes[phaseName] ?: []) + (targetPhasesAndTypes[TEST_PHASE_WILDCARD] ?: [])
                    types.each { GrailsTestType type ->
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

                TestPhaseConfigurer configurer = testFeatureDiscovery.findPhaseConfigurer(phase)
                try {

                    testExecutionContext.setVariable("currentTestPhaseName", phase)
                    // Add a blank line before the start of this phase so that it
                    // is easier to distinguish
                    buildEventListener.triggerEvent("TestPhaseStart", phase)

                    configurer?.prepare()

                    // Now run all the tests registered for this phase.
                    for(GrailsTestType type in types) {
                        processTests(type)
                    }
                }
                finally {
                    // Perform any clean up required.
                    configurer?.cleanup()
                    testExecutionContext.setVariable("currentTestPhaseName", null)
                }

                buildEventListener.triggerEvent("TestPhaseEnd", phase)
            }
        }
        catch(Throwable e) {
            testsFailed = true
            throw e
        }
        finally {
            String label = testsFailed ? "Tests FAILED" : "Tests PASSED"
            String msg = ""
            if (createTestReports) {
                buildEventListener.triggerEvent("TestProduceReports", testExecutionContext)
                msg += " - view reports in ${testReportsDir}"
            }
            if (testsFailed) {
                CONSOLE.error(label, msg)
            }
            else {
                CONSOLE.addStatus("$label$msg")
            }
            buildEventListener.triggerEvent("TestPhasesEnd", testExecutionContext)
            buildEventListener.triggerEvent("AllTestsEnd")
        }

        testsFailed ? 1 : 0
        if (triggerEvents)
            buildEventListener.triggerEvent("AllTestsEnd", testsFailed)
    }

    def getFailedTests() {
        File file = new File("${testReportsDir}/TESTS-TestSuites.xml")
        if (!file.exists()) {
            return []
        }

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
    }

    /**
     * Compiles and runs all the tests of the given type and then generates
     * the reports for them.
     * @param type The type of the tests to compile (not the test phase!)
     * For example, "unit", "jsunit", "webtest", etc.
     */
    @CompileStatic
    void processTests ( GrailsTestType type ) {
        testExecutionContext.setVariable("currentTestTypeName", type.name)

        try {
            def relativePathToSource = type.relativeSourcePath
            File dest = null
            if (relativePathToSource) {
                def source = new File("${testSourceDir}", relativePathToSource)
                if (!source.exists()) return // no source, no point continuing

                dest = new File(buildSettings.testClassesDir, relativePathToSource)
                projectTestCompiler.compileTests(type, source, dest)
            }

            runTests(type, dest)
        } finally {
            testExecutionContext.setVariable("currentTestTypeName", null)
        }
    }


    @CompileStatic
    void runTests ( GrailsTestType type, File compiledClassesDir ) {
        def testCount = type.prepare(testTargetPatterns.toArray(new GrailsTestTargetPattern[testTargetPatterns.size()]), compiledClassesDir, testExecutionContext)

        if (testCount) {
            try {
                buildEventListener.triggerEvent("TestSuiteStart", type.name)
                CONSOLE.updateStatus "Running ${testCount} $type.name test${testCount > 1 ? 's' : ''}..."

                def start = new Date()
                def result = type.run(testEventPublisher)
                def end = new Date()

                def delta = (end.time - start.time) / 1000
                def minutes = (delta / 60).toInteger()
                def seconds = (delta - minutes * 60).toInteger()

                testCount = result.passCount + result.failCount
                CONSOLE.addStatus "Completed $testCount $type.name test${testCount > 1 ? 's' : ''}, ${result.failCount} failed in ${minutes}m ${seconds}s"
                CONSOLE.lastMessage = ""

                if (result.failCount > 0) testsFailed = true
                buildEventListener.triggerEvent("TestSuiteEnd", type.name)

            }
            catch (e) {
                CONSOLE.error "Error running $type.name tests: ${e.message}", e
                testsFailed = true
            }
            finally {
                type.cleanup()
            }
        }
    }
}
