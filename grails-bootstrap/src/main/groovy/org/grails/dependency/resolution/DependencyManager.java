/*
 * Copyright 2013 the original author or authors.
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
package org.grails.dependency.resolution;

import groovy.lang.Closure;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * General interface for all dependency manager implementations to implement with common utility methods not tied to Ivy or Aether or any
 * dependency resolution engine
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public interface DependencyManager {
    String BUILD_SCOPE = "build";
    String COMPILE_SCOPE = "compile";
    String RUNTIME_SCOPE = "runtime";
    String TEST_SCOPE = "test";
    String PROVIDED_SCOPE = "provided";
    String BUILD_SCOPE_DESC = "Dependencies for the build system only";
    String COMPILE_SCOPE_DESC = "Dependencies placed on the classpath for compilation";
    String RUNTIME_SCOPE_DESC = "Dependencies needed at runtime but not for compilation";
    String TEST_SCOPE_DESC = "Dependencies needed for test compilation and execution but not at runtime";
    String PROVIDED_SCOPE_DESC = "Dependencies needed at development time, but not during deployment";
    Map<String, String> SCOPE_TO_DESC = new HashMap<String, String>(){{
            put(BUILD_SCOPE, BUILD_SCOPE_DESC);
            put(PROVIDED_SCOPE, PROVIDED_SCOPE_DESC);
            put(COMPILE_SCOPE, COMPILE_SCOPE_DESC);
            put(RUNTIME_SCOPE, RUNTIME_SCOPE_DESC);
            put(TEST_SCOPE, TEST_SCOPE_DESC);
    }};

    /**
     * URL to the central Grails plugin repository's global plugin list
     */
    String GRAILS_CENTRAL_PLUGIN_LIST = "http://grails.org/plugins/.plugin-meta/plugins-list.xml";

    /**
     * Downloads the Grails central plugin list and saves it to the given file. The file is then parsed and the resulting XML returned
     *
     * @param localFile The local file
     * @return The parsed XML
     */
    GPathResult downloadPluginList(File localFile);

    /**
     * Downloads information about a plugin from the -plugin.xml file
     *
     * @param pluginName The plugin name
     * @param pluginVersion The plugin version
     * @return The plugin.xml data or null if the plugin doesn't exit
     */
    GPathResult downloadPluginInfo(String pluginName, String pluginVersion);

    /**
     * Outputs the dependency graph to System.out
     */
    void produceReport();

    /**
     * Outputs the dependency graph to System.out
     *
     * @param scope The scope of the report
     */
    void produceReport(String scope);

    /**
     * Resolve dependencies for the given scope
     * @param scope The scope
     * @return The {@link DependencyReport} instance
     */
    DependencyReport resolve(String scope);

    /**
     * Resolve the JVM agent to be used for the forked JVM
     * @return The {@link DependencyReport} instance or null if no agent configured
     */
    DependencyReport resolveAgent();

    /**
     * Resolve dependencies for the default scope
     * @return The {@link DependencyReport} instance
     */
    DependencyReport resolve();

    /**
     * Resolves a single dependency.
     * @param dependency A {@link Dependency} instance.
     * @return The {@link DependencyReport} instance for the given dependency
     */
    DependencyReport resolveDependency(Dependency dependency);

    /**
     * The direct dependencies of the application, not including framework or dependencies inherited from plugins
     *
     * @return Direct application dependencies
     */
    Collection<Dependency> getApplicationDependencies();

    /**
     *
     * @return The plugin dependencies of the application
     */
    Collection<Dependency> getPluginDependencies();

    /**
     * All dependencies of the current application
     *
     * @return All application dependencies
     */
    Collection<Dependency> getAllDependencies();

    /**
     * The direct dependencies of the application, not including framework or dependencies inherited from plugins
     *
     * @param scope The scope of the dependencies
     * @return Direct application dependencies
     */
    Collection<Dependency> getApplicationDependencies(String scope);

    /**
     * The direct plugin dependencies of the application, not including framework or dependencies inherited from plugins
     *
     * @param scope The scope of the dependencies
     * @return Direct application dependencies
     */
    Collection<Dependency> getPluginDependencies(String scope);

    /**
     * All dependencies of the current application
     *
     * @param scope The scope of the dependencies
     * @return All application dependencies
     */
    Collection<Dependency> getAllDependencies(String scope);

    /**
     * @return Returns the exclude resolver for this dependency manager
     */
    ExcludeResolver getExcludeResolver();


    /**
     * Parse the dependency definition DSL
     *
     * @param callable The DSL definition
     */
    void parseDependencies(Closure callable);
}
