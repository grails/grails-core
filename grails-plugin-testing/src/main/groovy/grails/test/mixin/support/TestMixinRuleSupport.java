package grails.test.mixin.support;

import java.util.Set;

import grails.test.mixin.ClassRuleFactory;
import grails.test.mixin.Junit3TestCaseSupport;
import grails.test.mixin.RuleFactory;
import grails.test.runtime.TestRuntime;
import grails.test.runtime.TestRuntimeFactory;
import groovy.lang.GroovyObjectSupport;

import org.junit.rules.TestRule;

public abstract class TestMixinRuleSupport extends GroovyObjectSupport implements ClassRuleFactory, RuleFactory, Junit3TestCaseSupport {
    private TestRuntime currentRuntime;
    private Set<String> features;

    public TestMixinRuleSupport(Set<String> features) {
        this.features = features;
    }

    protected TestRuntime getRuntime() {
        if(currentRuntime == null || currentRuntime.isClosed()) {
            currentRuntime = TestRuntimeFactory.getRuntime(features);
        }
        return currentRuntime;
    }
    
    public TestRule newRule(Object targetInstance) {
        return getRuntime().newRule(targetInstance);
    }

    public TestRule newClassRule(Class<?> targetClass) {
        return getRuntime().newClassRule(targetClass);
    }

    public void setUp(Object testInstance) {
        getRuntime().setUp(testInstance);
    }

    public void tearDown(Object testInstance) {
        getRuntime().tearDown(testInstance);
    }
}
