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

package grails.test.mixin;

import grails.test.runtime.TestRuntime;

import java.util.Set;

/**
 * Interface for marking the mixin class aware of TestRuntime
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
public interface TestRuntimeAwareMixin {
    /**
     * @return list of features that are required by the test mixin
     */
    public Set<String> getFeatures();
    /**
     * TestRuntime will set it's instance to the mixin class by calling this method
     * 
     * @param runtime
     */
    public void setRuntime(TestRuntime runtime);
    
    /**
     * TestRuntime will set the target test class to the mixin class instance by calling this method
     * 
     * mixins are instantiated in static context in order to support JUnit @ClassRule annotations
     * the instance of the test class is not active when this method is called.
     *  
     * @param testClass
     */
    public void setTestClass(Class<?> testClass);
}
