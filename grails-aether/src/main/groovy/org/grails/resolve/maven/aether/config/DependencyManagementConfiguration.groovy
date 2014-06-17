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
package org.grails.resolve.maven.aether.config

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.grails.resolve.maven.aether.AetherDependencyManager
import org.grails.resolve.maven.aether.config.DependenciesConfiguration
import org.grails.resolve.maven.aether.config.DependencyConfiguration

/**
 * Allows configuration of dependency management.
 *
 * @since 2.4.1
 * @author Graeme Rocher
 */
class DependencyManagementConfiguration extends DependenciesConfiguration {
    DependencyManagementConfiguration(AetherDependencyManager dependencyManager) {
        super(dependencyManager)
    }

    @Override
    protected addDependencyToManager(DependencyConfiguration dependencyConfig) {
        dependencyManager.addManagedDependency(dependencyConfig.dependency)
    }

    @Override
    protected addBuildDependencyToManager(DependencyConfiguration dependencyConfig) {
        dependencyManager.addManagedDependency(dependencyConfig.dependency)
    }

    void dependency(String pattern, @DelegatesTo(DependencyConfiguration) Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), null), customizer
    }

    void dependency(Map<String, String> properties, @DelegatesTo(DependencyConfiguration) Closure customizer = null) {
        addDependency(properties, null, customizer)
    }


}
