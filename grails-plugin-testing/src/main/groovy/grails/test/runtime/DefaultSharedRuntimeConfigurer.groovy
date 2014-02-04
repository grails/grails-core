package grails.test.runtime

class DefaultSharedRuntimeConfigurer implements SharedRuntimeConfigurer {
    @Override
    public String[] getRequiredFeatures() {
        return null
    }

    @Override
    public void configure(TestRuntime runtime) {
        
    }
}
