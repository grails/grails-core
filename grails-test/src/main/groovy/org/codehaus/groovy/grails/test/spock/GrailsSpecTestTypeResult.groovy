package org.codehaus.groovy.grails.test.spock

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.test.GrailsTestTypeResult

/**
 * @author Luke Daley
 * @author Graeme Rocher
 */

@CompileStatic
class GrailsSpecTestTypeResult implements GrailsTestTypeResult {
    int runCount = 0
    int failCount = 0

    int getPassCount() {
        runCount - failCount
    }
}
