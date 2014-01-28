package grails.test.runtime;

import groovy.transform.CompileStatic;

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@CompileStatic
class TestRuntimeJunitAdapter {
    static {
        ExpandoMetaClass.enableGlobally()
    }

    public TestRule newRule() {
        return new TestRule() {
            Statement apply(Statement statement, Description description) {
                return new Statement() {
                    public void evaluate() throws Throwable {
                        TestRuntime runtime = TestRuntimeFactory.getRuntimeForTestClass(description.testClass)
                        runtime.before(description)
                        try {
                            statement.evaluate()
                        } catch (Throwable t) {
                            try {
                                runtime.after(description, t)
                            } catch (Throwable t2) {
                                // ignore
                            } finally {
                                // throw original exception
                                throw t
                            }
                        }
                        runtime.after(description, null)
                    }
                }
            }
        }
    }

    public TestRule newClassRule() {
        return new TestRule() {
            Statement apply(Statement statement, Description description) {
                return new Statement() {
                    public void evaluate() throws Throwable {
                        TestRuntime runtime = TestRuntimeFactory.getRuntimeForTestClass(description.testClass)
                        runtime.beforeClass(description)
                        try {
                            statement.evaluate()
                        } catch (Throwable t) {
                            try {
                                runtime.afterClass(description, t)
                            } catch (Throwable t2) {
                                // ignore
                            } finally {
                                // throw original exception
                                throw t
                            }
                        }
                        runtime.afterClass(description, null)
                    }
                }
            }
        }
    }
    
    public void setUp(Object testInstance) {
        TestRuntime runtime = TestRuntimeFactory.getRuntimeForTestClass(testInstance.getClass())
        runtime.setUp(testInstance)
    }
    
    public void tearDown(Object testInstance) {
        TestRuntime runtime = TestRuntimeFactory.getRuntimeForTestClass(testInstance.getClass())
        runtime.tearDown(testInstance)
    }
}
