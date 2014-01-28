/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.test.runtime;

import groovy.transform.CompileStatic;

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Internal class that's used as an adapter between JUnit @ClassRule / @Rule fields
 * and the TestRuntime system
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
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
