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

import java.io.File;
import java.util.List;

/**
 * A dependency report returned by a {@link DependencyManager} instance
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public interface DependencyReport {

    /**
     * @return The classpath as a string
     */
    String getClasspath();

    /**
     * @return The ResolvedArtifactReport instances for all resolved artifacts
     */
    List<ResolvedArtifactReport> getResolvedArtifacts();

    /**
     *
     * @return All the artifacts
     */
    List<File> getAllArtifacts();

    /**
     * @return The JAR files
     */
    List<File> getJarFiles();

    /**
     * @return The plugin zip files
     */
    List<File> getPluginZips();

    /**
     * @return The scope of this report
     */
    String getScope();

    /**
     * @return Whether there was a resolve error
     */
    boolean hasError();
    /**
     * @return The resolve error if there was one, otherwise null if no error occured
     */
    Throwable getResolveError();
}
