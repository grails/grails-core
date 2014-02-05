package grails.test.runtime


class DefaultSharedRuntimeConfigurer implements SharedRuntimeConfigurer, TestEventInterceptor {
    @Override
    public String[] getRequiredFeatures() {
        return null
    }

    @Override
    public void configure(TestRuntime runtime) {
        
    }

    @Override
    public void eventPublished(TestEvent event) {
        
    }

    @Override
    public void eventsProcessed(TestEvent event, List<TestEvent> consequenceEvents) {
        
    }

    @Override
    public void eventDelivered(TestEvent event) {
        
    }

    @Override
    public void mutateDeferredEvents(TestEvent initialEvent, List<TestEvent> deferredEvents) {
        
    }
}
