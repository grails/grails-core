package sharedruntimetest

import grails.test.runtime.SharedRuntimeConfigurer
import grails.test.runtime.TestRuntime;


class MySharedRuntimeConfigurer implements SharedRuntimeConfigurer {
    @Override
    public String[] getRequiredFeatures() {
        return null
    }

    @Override
    public void configure(TestRuntime runtime) {
        SharedRuntimeCheck.configurerCounter++
        runtime.putValue("hello", "world")
    }
}
