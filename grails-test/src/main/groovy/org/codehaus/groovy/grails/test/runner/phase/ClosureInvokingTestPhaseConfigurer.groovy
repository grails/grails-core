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


    @Override
    void prepare(Binding testExecutionContext, Map<String, Object> testOptions) {
        prepareClosure.call()
    }

    @Override
    void cleanup(Binding testExecutionContext, Map<String, Object> testOptions) {
        prepareClosure.call()
    }
}
