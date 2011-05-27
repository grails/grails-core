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
package org.codehaus.groovy.grails.resolve.config;

import grails.util.Metadata;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor;
import org.codehaus.groovy.grails.resolve.IvyDependencyManager;

public class DependencyConfigurationContext {

    final public IvyDependencyManager dependencyManager;
    final public String pluginName;
    final public boolean inherited;
    final public boolean exported;

    private DependencyConfigurationContext(IvyDependencyManager dependencyManager, String pluginName, boolean inherited) {
        this.dependencyManager = dependencyManager;
        this.pluginName = pluginName;
        this.inherited = inherited;
        if (pluginName != null) {
            DependencyDescriptor pluginDependencyDescriptor = dependencyManager.getPluginDependencyDescriptor(pluginName);
            exported = Metadata.getCurrent().getInstalledPlugins().containsKey(pluginName) ||
                        !(pluginDependencyDescriptor instanceof EnhancedDefaultDependencyDescriptor) ||
                        ((EnhancedDefaultDependencyDescriptor) pluginDependencyDescriptor).getExported();
        }
        else {
            exported = true;
        }
    }

    static public DependencyConfigurationContext forApplication(IvyDependencyManager dependencyManager) {
        return new DependencyConfigurationContext(dependencyManager, null, false);
    }

    static public DependencyConfigurationContext forPlugin(IvyDependencyManager dependencyManager, String pluginName) {
        return new DependencyConfigurationContext(dependencyManager, pluginName, false);
    }

    public DependencyConfigurationContext createInheritedContext() {
        return new DependencyConfigurationContext(dependencyManager, pluginName, true);
    }
}
