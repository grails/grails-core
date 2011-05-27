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

import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor;
import java.util.Map;

class PluginDependenciesConfigurer extends AbstractDependenciesConfigurer {

    public PluginDependenciesConfigurer(DependencyConfigurationContext context) {
        super(context);
    }

    @Override
    protected void addDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor) {
        if (context.pluginName != null) {
            descriptor.setTransitivelyIncluded(true);
        }

        getDependencyManager().registerPluginDependency(scope, descriptor);
    }

    @Override
    protected void handleExport(EnhancedDefaultDependencyDescriptor descriptor, Boolean export) {
        if (export != null) {
            descriptor.setExported(export);
        }
        else {
            descriptor.setExport(context.exported);
        }
    }

    @Override
    protected void preprocessDependencyProperties(Map<Object, Object> dependency) {
        Object groupValue = dependency.get("group");
        if (groupValue == null || groupValue.toString().equals("")) {
            dependency.put("group", "org.grails.plugins");
        }
    }
}
