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
 * Interface to TestPlugin
 * 
 * The TestRuntime is event based and handles events by TestPlugins.
 * TestPlugins should be stateless. All state should be held in TestRuntime variables.
 * 
 * Dependencies between plugins are expressed by "features". 
 * A "feature" is identified by a String value. For example "grailsApplication".
 * 
 * A plugin can require or provide features. 
 * A TestRuntime instance is requested with an array of required features. The {@link TestRuntimeFactory} contains
 * a registry of all plugins and does the dependency resolution to find out what plugins should be added to the runtime
 * and what the order is.
 * The order based on dependencies is the most significant. 
 * Plugins with the same dependency order are sorted by the "getOrdinal()" value (highest value comes first).
 * 
 * It's possible to override "features" by providing a plugin with a higher ordinal value than another plugin.
 * The overriding plugin might have to provide all of the features of the other plugin when the plugin provides several features.
 * Custom plugins can be registered to the single {@link TestRuntimeFactory} in static initializer blocks of the test class.
 * 
 * The plugin class can also implement TestEventInterceptor. In that case the instance will be also registered as an event interceptor.
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
public interface TestPlugin {
    /**
     * @return array of required features 
     */
    String[] getRequiredFeatures();
    /**
     * @return array of provided features
     */
    String[] getProvidedFeatures();
    /**
     * Priority order of the plugin
     * default value is 0
     * @return value used for sorting plugins with same dependency order. It is also used for picking a plugin when multiple plugins implement a single feature. Highest value wins.
     */
    int getOrdinal();
    /**
     * this method gets called for all events that occur in the TestRuntime
     * @param event, see {@link TestEvent} for details
     */
    void onTestEvent(TestEvent event);
    /**
     * this method is called when the state of the given runtime should be cleaned.
     * @param runtime that is to be closed
     */
    void close(TestRuntime runtime);
}