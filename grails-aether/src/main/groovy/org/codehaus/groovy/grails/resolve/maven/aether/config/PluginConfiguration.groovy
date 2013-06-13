/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven.aether.config

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.sonatype.aether.graph.Dependency

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class PluginConfiguration extends DependenciesConfiguration {

    PluginConfiguration(AetherDependencyManager dependencyManager) {
        super(dependencyManager)
    }

    @Override
    void addDependency(Dependency dependency, Closure customizer) {
        super.addDependency(dependency, customizer)
    }

    @Override
    void addBuildDependency(Dependency dependency, Closure customizer) {
        super.addBuildDependency(dependency, customizer)
    }

    @Override
    void build(String pattern, Closure customizer) {
        super.build(extractDependencyProperties(pattern), customizer)
    }

    @Override
    void compile(String pattern, Closure customizer) {
        super.compile(extractDependencyProperties(pattern), customizer)
    }

    @Override
    void runtime(String pattern, Closure customizer) {
        super.compile(extractDependencyProperties(pattern), customizer)
    }

    @Override
    void provided(String pattern, Closure customizer) {
        super.compile(extractDependencyProperties(pattern), customizer)
    }

    @Override
    void optional(String pattern, Closure customizer) {
        super.compile(extractDependencyProperties(pattern), customizer)
    }

    @Override
    void test(String pattern, Closure customizer) {
        super.compile(extractDependencyProperties(pattern), customizer)
    }

    @Override
    protected String getDefaultExtension() {
        'zip'
    }

    protected String getDefaultGroup() {
        'org.grails.plugins'
    }
}
