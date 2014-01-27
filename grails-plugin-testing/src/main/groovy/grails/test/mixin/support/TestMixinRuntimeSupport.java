package grails.test.mixin.support;

import grails.test.mixin.TestRuntimeAwareMixin;
import grails.test.runtime.TestRuntime;
import grails.test.runtime.TestRuntimeFactory;
import groovy.lang.GroovyObjectSupport;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class TestMixinRuntimeSupport extends GroovyObjectSupport implements TestRuntimeAwareMixin {
    private TestRuntime currentRuntime;
    private Set<String> features;
    private Class<?> testClass;

    public TestMixinRuntimeSupport(Set<String> features) {
        this.features = Collections.unmodifiableSet(new LinkedHashSet<String>(features));
    }
    
    public Set<String> getFeatures() {
        return features;
    }

    public TestRuntime getRuntime() {
        if(currentRuntime == null && testClass != null) {
            TestRuntimeFactory.getRuntimeForTestClass(testClass);
        }
        if(currentRuntime == null) {
            throw new IllegalStateException("Current TestRuntime instance is null.");
        } else if (currentRuntime.isClosed()) {
            throw new IllegalStateException("Current TestRuntime instance is closed.");
        }
        return currentRuntime;
    }
    
    public void setRuntime(TestRuntime runtime) {
        this.currentRuntime = runtime;
    }
    
    @Override
    public void setTestClass(Class<?> testClass) {
        this.testClass = testClass;
    }
}