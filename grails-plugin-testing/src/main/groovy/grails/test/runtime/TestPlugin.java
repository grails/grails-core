package grails.test.runtime;

public interface TestPlugin {
    String[] getRequiredFeatures();
    String[] getProvidedFeatures();
    int getOrdinal();
    void onTestEvent(TestEvent event);
}