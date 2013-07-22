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
package org.codehaus.groovy.grails.resolve;

import grails.util.BuildSettings;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.util.Collection;

/**
 * General interface for all dependency manager implementations to implement with common utility methods not tied to Ivy or Aether or any
 * dependency resolution engine
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public interface DependencyManager {

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
     * Creates a copy of this dependency manager with repository configuration retained but dependencies omitted.
     *
     * @param buildSettings The BuildSettings
     * @return The copy
     */
    DependencyManager createCopy(BuildSettings buildSettings);

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
}
