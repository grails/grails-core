package grails.test.mixin.support;

import grails.test.mixin.ClassRuleFactory;
import grails.test.mixin.Junit3TestCaseSupport;
import grails.test.mixin.RuleFactory;
import grails.test.runtime.TestRuntime;
import groovy.lang.GroovyObjectSupport;

import org.junit.rules.TestRule;

public class TestMixinRuleSupport extends GroovyObjectSupport implements ClassRuleFactory, RuleFactory, Junit3TestCaseSupport {
    protected TestRuntime runtime;

    public TestMixinRuleSupport(TestRuntime runtime) {
        this.runtime = runtime;
    }

    public TestRule newRule(Object targetInstance) {
        return runtime.newRule(targetInstance);
    }

    public TestRule newClassRule(Class<?> targetClass) {
        return runtime.newClassRule(targetClass);
    }

    public void setUp(Object testInstance) {
        runtime.setUp(testInstance);
    }

    public void tearDown(Object testInstance) {
        runtime.tearDown(testInstance);
    }
}
