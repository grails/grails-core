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

import grails.util.GrailsWebUtil as GWU

import java.lang.reflect.Modifier
import junit.framework.TestCase
import junit.framework.TestResult
import junit.framework.TestSuite
import org.apache.commons.logging.LogFactory
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import junit.framework.Test
import grails.util.GrailsUtil
import grails.util.GrailsNameUtils

/**
 * Gant script that runs the Grails unit tests
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsTest")

TEST_PHASE_AND_TYPE_SEPARATOR = ':'

target('default': "Run a Grails applications unit tests") {
    depends(checkVersion, configureProxy, parseArguments, cleanTestReports)

    // The test targeting patterns
    def testTargeters = []

    // The params that target a phase and/or type
    def phaseAndTypeTargeters = []

    // Separate the type/phase targeters from the test targeters
    for (p in argsMap["params"]) {
        def destination = p.contains(TEST_PHASE_AND_TYPE_SEPARATOR) ? phaseAndTypeTargeters : testTargeters
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
    for (t in phaseAndTypeTargeters) {
        def parts = t.split(TEST_PHASE_AND_TYPE_SEPARATOR, 2)
        def targetPhase = parts[0] ?: TEST_PHASE_WILDCARD
        def targetType = parts[1] ?: TEST_TYPE_WILDCARD

        if (!targetPhasesAndTypes.containsKey(targetPhase)) targetPhasesAndTypes[targetPhase] = []
        targetPhasesAndTypes[targetPhase] << targetType
    }

    // Any switch style args are "test options" (from _GrailsTest)
    for (a in argsMap) {
        if (a.key != 'params') {
            testOptions[a.key] = a.value
        }
    }

    if (argsMap["xml"]) {
        reportFormats = [ "xml" ]
        createTestReports = false
    }
    else {
        createTestReports = !argsMap["no-reports"]
    }

    reRunTests = argsMap["rerun"]

    allTests()
}
