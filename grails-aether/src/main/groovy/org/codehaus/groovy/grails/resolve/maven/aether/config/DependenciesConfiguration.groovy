/* Copyright 2012 the original author or authors.
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
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector

import java.util.regex.Pattern

/**
 * @author Graeme Rocher
 */
@CompileStatic
class DependenciesConfiguration {
    static final Pattern DEPENDENCY_PATTERN = Pattern.compile("([a-zA-Z0-9\\-/\\._+=]*?):([a-zA-Z0-9\\-/\\._+=]+?):([a-zA-Z0-9\\-/\\.,\\]\\[\\(\\)_+=]+)");
    public static final String SCOPE_COMPILE = "compile"
    public static final String SCOPE_RUNTIME = "runtime"
    public static final String SCOPE_PROVIDED = "provided"
    public static final String SCOPE_OPTIONAL = "optional"
    public static final String SCOPE_TEST = "test"

    AetherDependencyManager dependencyManager
    ExclusionDependencySelector exclusionDependencySelector

    DependenciesConfiguration(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    void addDependency(Dependency dependency, Closure customizer = null) {
        if (exclusionDependencySelector == null || !exclusionDependencySelector.selectDependency(dependency)) {
            dependency = customizeDependency(customizer, dependency)
            dependencyManager.dependencies << dependency
        }
    }


    void compile(String pattern, Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), SCOPE_COMPILE), customizer
    }

    void compile(Map<String, String> properties, Closure customizer = null) {
        addDependency(properties, SCOPE_COMPILE, customizer)
    }

    void runtime(String pattern, Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), SCOPE_RUNTIME), customizer
    }

    void runtime(Map<String, String> properties, Closure customizer = null) {
        addDependency properties, SCOPE_RUNTIME, customizer
    }

    void provided(String pattern, Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), SCOPE_PROVIDED), customizer
    }

    void provided(Map<String, String> properties, Closure customizer = null) {
        addDependency properties, SCOPE_PROVIDED, customizer
    }

    void optional(String pattern, Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), SCOPE_OPTIONAL), customizer
    }

    void optional(Map<String, String> properties, Closure customizer = null) {
        addDependency properties, SCOPE_OPTIONAL, customizer
    }

    void test(String pattern, Closure customizer = null) {
        addDependency new Dependency(new DefaultArtifact(pattern), SCOPE_TEST), customizer
    }

    void test(Map<String, String> properties, Closure customizer = null) {
        addDependency properties, SCOPE_TEST, customizer
    }

    protected Map extractDependencyProperties(String pattern) {
        def matcher = DEPENDENCY_PATTERN.matcher(pattern)
        if (matcher.matches()) {

            def properties = [:]
            properties.artifactId = matcher.group(2)
            properties.groupId = matcher.group(1) ?: getDefaultGroup()
            properties.version = matcher.group(3)
            properties
        }
        else {
            throw new IllegalArgumentException( "Bad artifact coordinates " + pattern
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>" );

        }
    }

    protected String getDefaultGroup() { "" }
    protected String getDefaultExtension() { null }

    protected void addDependency(Map<String, String> properties, String scope, Closure customizer = null) {
        if (!properties.group) {
            properties.group = defaultGroup
        }
        if (!properties.extension) {
            properties.extension = defaultExtension
        }
        def d = new Dependency(new DefaultArtifact(properties.groupId, properties.artifactId, properties.classifier, properties.extension, properties.version), scope)
        addDependency(d, customizer)
    }

    protected Dependency customizeDependency(Closure customizer, Dependency dependency) {
        if (customizer) {
            def dc = new DependencyConfiguration(dependency)
            customizer.setDelegate(dc)
            customizer.call()
            dependency = dc.dependency
        }
        dependency
    }

}
