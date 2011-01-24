/* Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.config

import org.codehaus.groovy.grails.resolve.IvyDependencyManager

class DependencyConfigurationContext {
    
    final IvyDependencyManager dependencyManager
    final String currentPluginBeingConfigured
    final boolean inherited
    
    private DependencyConfigurationContext(IvyDependencyManager dependencyManager, String currentPluginBeingConfigured, boolean inherited) {
        this.dependencyManager = dependencyManager
        this.currentPluginBeingConfigured = currentPluginBeingConfigured
        this.inherited = inherited
    }
    
    static forApplication(IvyDependencyManager dependencyManager) {
        new DependencyConfigurationContext(dependencyManager, null, false)
    }
    
    static forPlugin(IvyDependencyManager dependencyManager, String pluginName) {
        new DependencyConfigurationContext(dependencyManager, pluginName, false)
    }
    
    DependencyConfigurationContext createInheritedContext() {
        new DependencyConfigurationContext(dependencyManager, currentPluginBeingConfigured, true)
    }
    
}