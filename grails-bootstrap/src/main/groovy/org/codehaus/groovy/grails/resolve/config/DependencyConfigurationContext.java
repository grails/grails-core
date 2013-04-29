/*
 * Copyright 2011 the original author or authors.
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
import grails.util.BuildSettings;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor;
import org.codehaus.groovy.grails.resolve.IvyDependencyManager;
import org.codehaus.groovy.grails.resolve.GrailsCoreDependencies;

public class DependencyConfigurationContext {

    final public IvyDependencyManager dependencyManager;
    final public String pluginName;
    final public boolean inherited;
    final public boolean exported;
    private boolean offline;
    private ExcludeRule[] excludeRules;
    private String parentScope;

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

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
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

    /**
     * Gives access to the grails core dependencies.
     *
     * @throws IllegalStateException If the dependency manager is unable to provide this information
     */
    public GrailsCoreDependencies getGrailsCoreDependencies() {
        BuildSettings buildSettings = dependencyManager.getBuildSettings();
        if (buildSettings == null) {
            throw new IllegalStateException("Cannot ask for grails core dependencies if the dependency manager was configured without build settings, as it was in this case.");
        }

        return buildSettings.getCoreDependencies();
    }
    public void setExcludeRules(ExcludeRule[] excludeRules) {
        this.excludeRules = excludeRules;
    }

    public ExcludeRule[] getExcludeRules() {
        return excludeRules;
    }

    public void setParentScope(String scope) {
        parentScope = scope;
    }
    public String getParentScope() {
        return parentScope;
    }
}
