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
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.util.graph.PreorderNodeListGenerator

/**
 * Implementation of the {@link DependencyReport} interface that adapts Aether's PreorderNodeListGenerator
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDependencyReport implements DependencyReport {

    PreorderNodeListGenerator resolveResult
    String scope
    Throwable error
    List<File> pluginZips = []
    List<File> jarFiles = []

    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope) {
        this.resolveResult = resolveResult
        this.scope = scope
        this.jarFiles = findAndRemovePluginDependencies(resolveResult.files)
    }
    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope, Throwable error) {
        this.resolveResult = resolveResult
        this.scope = scope
        this.error = error
        this.jarFiles = findAndRemovePluginDependencies(resolveResult.files)
    }

    private List<File> findAndRemovePluginDependencies(Collection<File> jarFiles) {
        jarFiles = jarFiles?.findAll { File it -> it != null} ?: new ArrayList<File>()
        def zips = jarFiles.findAll { File it -> it.name.endsWith(".zip") }
        for (z in zips) {
            if (!pluginZips.contains(z)) {
                pluginZips.add(z)
            }
        }
        jarFiles = jarFiles.findAll { File it -> !it.name.endsWith(".zip") }
        return jarFiles as List
    }
    String getClasspath() {
        resolveResult.getClassPath()
    }

    List<ResolvedArtifactReport> getResolvedArtifacts() {
        List<ResolvedArtifactReport> reports = []
        for(DependencyNode node in resolveResult.nodes) {
            final dependency = node.dependency
            def f = dependency?.artifact?.file
            if (f) {
                final artifact = dependency.artifact
                final grailsDependency = new Dependency(artifact.groupId, artifact.artifactId, artifact.version)
                grailsDependency.classifier = artifact.classifier
                reports << new ResolvedArtifactReport(grailsDependency, f)
            }
        }
        return reports
    }

    @Override
    List<File> getAllArtifacts() {
        getFiles().toList()
    }

    @Override
    String getScope() {
        return scope
    }

    @Override
    boolean hasError() {
        return error != null
    }

    @Override
    Throwable getResolveError() {
        return error
    }

    File[] getFiles() {
        resolveResult.getFiles() as File[]
    }
}
