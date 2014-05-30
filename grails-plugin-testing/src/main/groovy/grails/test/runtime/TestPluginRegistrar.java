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

/**
 * Interface supported by TestMixin target classes and SharedRuntimeConfigurer classes registering or unregistering
 * TestPlugin classes
 * 
 * @author Lari Hotari
 * @since 2.4.1
 */
public interface TestPluginRegistrar {
    /**
     * @return list of TestPlugin classes that should be registered or unregistered (excluded) when this test Mixin is used
     * 
     */
    public Iterable<TestPluginUsage> getTestPluginUsages();
}
