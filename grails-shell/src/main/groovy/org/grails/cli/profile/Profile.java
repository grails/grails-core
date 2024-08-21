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
package org.grails.cli.profile;

import jline.console.completer.Completer;
import org.eclipse.aether.graph.Dependency;
import org.grails.config.NavigableMap;
import org.grails.io.support.Resource;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * A Profile defines an active code generation and command execution policy. For example the "web" profile allows
 * the execution of code gen and build commands that relate to web applications
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 *
 * @since 3.0
 */
public interface Profile {

    /**
     * @return The name of the profile
     */
    String getName();

    /**
     * @return The version of the profile
     */
    String getVersion();

    /**
     * @return The description of the profile
     */
    String getDescription();

    /**
     * @return The list of file extensions which should be treated as binary
     */
    Set<String> getBinaryExtensions();

    /**
     * @return The list of file patterns which should be executable in the resulting application
     */
    Set<String> getExecutablePatterns();

    /**
     * @return Text to display after an application has been created with the profile
     */
    String getInstructions();

    /**
     * @return The features for this profile
     */
    Iterable<Feature> getFeatures();

    /**
     * @return The default features for this profile
     */
    Iterable<Feature> getDefaultFeatures();

    /**
     * @return The required features for this profile
     */
    Iterable<Feature> getRequiredFeatures();

    /**
     * The other {@link org.grails.cli.profile.Profile} instances that this {@link org.grails.cli.profile.Profile} extends
     * @return zero or many {@link org.grails.cli.profile.Profile} instance that this profile extends from
     */
    Iterable<Profile> getExtends();

    /**
     * @return The maven repository definitions for this profile
     */
    List<String> getRepositories();

    /**
     * @return The dependency definitions for this profile
     */
    List<Dependency> getDependencies();

    /**
     * @return The profiles configuration
     */
    NavigableMap getConfiguration();

    /**
     * @return The directory where the profile is located locally
     */
    Resource getProfileDir();

    /**
     * Obtain a template by path
     *
     * @param path The path to template
     * @return The resource or null if it doesn't exist
     */
    Resource getTemplate(String path);

    /**
     * Obtain a command by name
     *
     * @param name Obtain a command by name
     * @return The command
     */
    Command getCommand(ProjectContext context, String name);

    /**
     * The profile completers
     * @param context The {@link org.grails.cli.profile.ProjectContext} instance
     * @return An {@link java.lang.Iterable} of {@link jline.console.completer.Completer} instances
     */
    Iterable<Completer> getCompleters(ProjectContext context);

    /**
     * The profile {@link org.grails.cli.profile.Command} instances
     *
     * @param context The {@link ProjectContext} instance
     * @return An {@link java.lang.Iterable} of {@link org.grails.cli.profile.Command} instances
     */
    Iterable<Command> getCommands(ProjectContext context);

    /**
     * Whether a command executes for the given context and name
     * @param context The {@link org.grails.cli.profile.ProjectContext}
     * @param name The command name
     * @return True if the command does exist
     */
    boolean hasCommand(ProjectContext context, String name);
    /**
     * Obtains a {@link Command}
     *
     * @return True if the command was handled
     */
    boolean handleCommand(ExecutionContext context);

    /**
     * @return The buildscript maven repository definitions for this profile
     */
    List<String> getBuildRepositories();

    /**
     * @return The profile names to participate in build merge
     */
    List<String> getBuildMergeProfileNames();

    /**
     * @return The list of build plugins for this profile
     */
    List<String> getBuildPlugins();

    /**
     * @return The subfolder the parent profile(s) skeleton should be copied into
     */
    String getParentSkeletonDir();

    /**
     * @return The directory the parent profile(s) skeleton should be copied into
     */
    File getParentSkeletonDir(File parent);

    /**
     * @return A list of paths to exclude from the skeleton. Used in ant fileset exclude:
     */
    List<String> getSkeletonExcludes();
}
