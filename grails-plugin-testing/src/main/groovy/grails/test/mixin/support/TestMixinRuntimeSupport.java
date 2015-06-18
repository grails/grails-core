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

package grails.test.mixin.support;

import grails.test.mixin.TestRuntimeAwareMixin;
import grails.test.runtime.TestRuntime;
import grails.test.runtime.TestRuntimeFactory;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObjectSupport;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstract base class for test mixin classes that use the new TestRuntime
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
public abstract class TestMixinRuntimeSupport extends GroovyObjectSupport implements TestRuntimeAwareMixin {

    private TestRuntime currentRuntime;
    private Set<String> features;
    private Class<?> testClass;

    public TestMixinRuntimeSupport(Set<String> features) {
        this.features = Collections.unmodifiableSet(new LinkedHashSet<String>(features));
    }
    
    @SkipMethod
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
    
    @SkipMethod
    public void setRuntime(TestRuntime runtime) {
        this.currentRuntime = runtime;
    }
    
    @SkipMethod
    @Override
    public void setTestClass(Class<?> testClass) {
        this.testClass = testClass;
    }
}