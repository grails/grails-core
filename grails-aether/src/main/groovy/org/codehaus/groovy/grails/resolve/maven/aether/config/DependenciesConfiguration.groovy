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

import grails.build.logging.GrailsConsole
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.util.regex.Pattern

import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector
import grails.util.Environment

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DependenciesConfiguration {
    static final Pattern DEPENDENCY_PATTERN = Pattern.compile("([a-zA-Z0-9\\-/\\._+=]*?):([a-zA-Z0-9\\-/\\._+=]+?):([a-zA-Z0-9\\-/\\.,\\]\\[\\(\\)_+=]+)")
    public static final String SCOPE_COMPILE = "compile"
    public static final String SCOPE_RUNTIME = "runtime"
    public static final String SCOPE_PROVIDED = "provided"
    public static final String SCOPE_OPTIONAL = "optional"
    public static final String SCOPE_TEST = "test"
    public static final String SCOPE_BUILD = "build"
    public static final String ALL_SCOPES = [SCOPE_COMPILE, SCOPE_RUNTIME, SCOPE_PROVIDED, SCOPE_TEST, SCOPE_OPTIONAL,SCOPE_BUILD]

    AetherDependencyManager dependencyManager
    ExclusionDependencySelector exclusionDependencySelector

    DependenciesConfiguration(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    void addDependency(Dependency dependency, Closure customizer = null) {
        if (exclusionDependencySelector == null || exclusionDependencySelector.selectDependency(dependency)) {
            final dependencyConfig = customizeDependency(customizer, dependency)
            dependency = dependencyConfig.dependency
            dependencyManager.addDependency dependencyConfig.dependency, dependencyConfig
        }
    }

    void addBuildDependency(Dependency dependency, Closure customizer = null) {
        if (exclusionDependencySelector == null || exclusionDependencySelector.selectDependency(dependency)) {
            final dependencyConfig = customizeDependency(customizer, dependency)
            dependencyManager.addBuildDependency dependencyConfig.dependency, dependencyConfig
        }
    }

    protected void addDependency(Map<String, String> properties, String scope, Closure customizer = null) {
        Dependency d = createDependencyForProperties(properties, scope)
        addDependency(d, customizer)
    }

    protected void addBuildDependency(Map<String, String> properties, String scope, Closure customizer = null) {
        Dependency d = createDependencyForProperties(properties, scope)
        addBuildDependency(d, customizer)
    }

    Dependency createDependencyForProperties(Map<String, String> properties, String scope) {
        if (!properties.group) {
            properties.group = defaultGroup
        }
        if (!properties.extension) {
            properties.extension = defaultExtension
        }
        return new Dependency(new DefaultArtifact(properties.groupId, properties.artifactId, properties.classifier, properties.extension, properties.version), scope)
    }

    void addDependency(org.codehaus.groovy.grails.resolve.Dependency dependency, String scope) {
        dependencyManager.addDependency dependency, scope, exclusionDependencySelector
    }

    void addBuildDependency(org.codehaus.groovy.grails.resolve.Dependency dependency) {
        dependencyManager.addBuildDependency dependency
    }

    /**
     * Configure the JAR to use for the reloading agent
     *
     * @param pattern The version pattern
     */
    void agent(String pattern) {
        dependencyManager.setJvmAgent(new Dependency(new DefaultArtifact(pattern), SCOPE_COMPILE))
    }

    void build(String pattern, Closure customizer = null) {
        addBuildDependency new Dependency(new DefaultArtifact(pattern), SCOPE_COMPILE), customizer
    }

    void build(Map<String, String> properties, Closure customizer = null) {
        addBuildDependency(properties, SCOPE_COMPILE, customizer)
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

    def methodMissing(String name, args) {
        final console = GrailsConsole.getInstance()
        if (args == null || !ALL_SCOPES.contains(name)) {
            console.error("WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..")
            return null
        }

        def argsList = Arrays.asList((Object[])args)
        if (!argsList) {
            console.error("WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..")
            return null
        }



        if (isOnlyStrings(argsList)) {
            invokeForString(name, argsList)
        }
        else if (isStringsAndConfigurer(argsList)) {
            invokeForString(name, argsList[0..-2], (Closure) argsList[-1] )
        }
        else {
            console.error("WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..")
        }
    }

    /**
     * Environment support
     *
     * @param callable The callable
     * @return The result of the environments block
     */
    def environments(Closure callable) {
        final environmentCallable = Environment.getEnvironmentSpecificBlock(callable)
        if(environmentCallable) {
            environmentCallable.setDelegate(this)
            environmentCallable.call()
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void invokeForString(String scope, List<Object> objects, Closure configurer = null) {
        for(o in objects) {
            "$scope"(o.toString(), configurer)
        }
    }

    private boolean isOnlyStrings(List<Object> args) {
        for (Object arg in args) {
            if (!(arg instanceof CharSequence)) {
                return false
            }
        }
        return true
    }

    private boolean isStringsAndConfigurer(List<Object> args) {
        if (args.size() == 1) {
            return false
        }
        return isOnlyStrings(args[0..-2]) && args[-1] instanceof Closure
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
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>" )

        }
    }

    protected String getDefaultGroup() { "" }
    protected String getDefaultExtension() { null }

    protected DependencyConfiguration customizeDependency(Closure customizer, Dependency dependency) {
        def dc = new DependencyConfiguration(dependency)
        if (customizer) {
            customizer.setDelegate(dc)
            customizer.setResolveStrategy(Closure.DELEGATE_ONLY)
            customizer.call()
            dependency = dc.dependency
        }
        dc
    }
}
