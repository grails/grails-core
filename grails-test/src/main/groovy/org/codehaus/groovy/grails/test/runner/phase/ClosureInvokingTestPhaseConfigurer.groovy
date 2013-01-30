package org.codehaus.groovy.grails.test.runner.phase

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 */
@CompileStatic
class ClosureInvokingTestPhaseConfigurer implements TestPhaseConfigurer {

    Closure prepareClosure
    Closure cleanupClosure

    ClosureInvokingTestPhaseConfigurer(Closure prepareClosure, Closure cleanupClosure) {
        this.prepareClosure = prepareClosure
        this.cleanupClosure = cleanupClosure
    }

    void prepare() {
        prepareClosure.call()
    }

    void cleanup() {
        cleanupClosure.call()
    }
}
