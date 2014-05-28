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

package grails.test.runtime

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * 
 * Value class for holding information about test plugin class registration or un-registeration (exclusion)
 * 
 * @author Lari Hotari
 * @since 2.4.1
  */
@CompileStatic
@Immutable
class TestPluginUsage {
    Collection<Class<? extends TestPlugin>> pluginClasses
    boolean exclude
    boolean requestActivation
    
    public static Iterable<TestPluginUsage> createForActivating(Class<? extends TestPlugin> pluginClass) {
        createForActivating([pluginClass])
    }
    
    public static Iterable<TestPluginUsage> createForActivating(Collection<Class<? extends TestPlugin>> pluginClasses) {
        [new TestPluginUsage(pluginClasses: pluginClasses, exclude: false, requestActivation: true)]
    }
}