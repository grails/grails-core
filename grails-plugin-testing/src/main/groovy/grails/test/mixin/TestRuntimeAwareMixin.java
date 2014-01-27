package grails.test.mixin;

import grails.test.runtime.TestRuntime;

import java.util.Set;

public interface TestRuntimeAwareMixin {
    public Set<String> getFeatures();
    public void setRuntime(TestRuntime runtime);
    public void setTestClass(Class<?> testClass);
}
