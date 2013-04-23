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
package org.codehaus.groovy.grails.resolve.maven.aether

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.codehaus.groovy.grails.resolve.ResolvedArtifactReport
import org.sonatype.aether.resolution.ArtifactResult

/**
 * Implementation of the {@link DependencyReport} interface that adapts Aether's ArtifactResult class
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherArtifactResultReport implements DependencyReport {

    String scope
    List<ArtifactResult> artifactResults

    AetherArtifactResultReport(String scope, List<ArtifactResult> artifactResults) {
        this.scope = scope
        this.artifactResults = artifactResults
    }

    String getClasspath() {
        jarFiles.join(File.pathSeparator)
    }

    List<ResolvedArtifactReport> getResolvedArtifacts() {
        List<ResolvedArtifactReport> reports = []

        for(ArtifactResult ar in artifactResults) {
            final artifact = ar.artifact
            if (artifact?.file) {
                final grailsDependency = new Dependency(artifact.groupId, artifact.artifactId, artifact.version)
                grailsDependency.classifier = artifact.classifier

                reports << new ResolvedArtifactReport(grailsDependency, artifact.file)
            }
        }

        return reports
    }

    List<File> getAllArtifacts() {
        return artifactResults.findAll { ArtifactResult ar -> ar.artifact?.file != null }.collect { ArtifactResult ar -> ar.artifact.file }
    }

    List<File> getJarFiles() {
        return allArtifacts.findAll { File f -> f.name.endsWith(".jar")} as List
    }

    List<File> getPluginZips() {
        return allArtifacts.findAll { File f -> f.name.endsWith(".zip")} as List
    }

    File[] getFiles() {
        allArtifacts as File[]
    }

    boolean hasError() { false }

    Throwable getResolveError() { null }
}
