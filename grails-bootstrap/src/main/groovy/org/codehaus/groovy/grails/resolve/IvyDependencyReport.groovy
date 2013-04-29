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
package org.codehaus.groovy.grails.resolve

import groovy.transform.CompileStatic

import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.report.ResolveReport

/**
 * A {@link DependencyReport} implementation for Ivy.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class IvyDependencyReport implements DependencyReport {

    ResolveReport resolveReport
    List<File> jarFiles = []
    List<File> pluginZips = []
    List<File> allArtifacts = []
    String scope

    private Collection<ArtifactDownloadReport> artifactDownloadReports

    IvyDependencyReport(String scope, ResolveReport resolveReport) {
        this.resolveReport = resolveReport
        this.scope = scope
        this.artifactDownloadReports = resolveReport.getArtifactsReports(null, false).findAll { ArtifactDownloadReport it -> it.downloadStatus.toString() != 'failed' }
        this.allArtifacts = artifactDownloadReports.collect { ArtifactDownloadReport it -> it.localFile }
        this.jarFiles = findAndRemovePluginDependencies(this.allArtifacts)
    }

    private List<File> findAndRemovePluginDependencies(Collection<File> jarFiles) {
        jarFiles = jarFiles?.findAll { File it -> it != null} ?: new ArrayList<File>()
        def zips = jarFiles.findAll { File it -> it.name.endsWith(".zip") }
        for (z in zips) {
            if (!pluginZips.contains(z)) {
                pluginZips.add(z)
            }
        }
        jarFiles = jarFiles.findAll { File it -> it.name.endsWith(".jar") }
        return jarFiles as List
    }

    List<ResolvedArtifactReport> getResolvedArtifacts() {
        List<ResolvedArtifactReport> reports = []
        for(ArtifactDownloadReport adr in artifactDownloadReports) {
            final id = adr.artifact.id.moduleRevisionId
            final file = adr.localFile

            final grailsDependency = new Dependency(id.organisation, id.name, id.revision)
            grailsDependency.classifier = id.getAttribute("m:classifier")
            reports << new ResolvedArtifactReport(grailsDependency, file)
        }
        return reports
    }

    @Override
    String getClasspath() {
        return jarFiles.join(File.pathSeparator)
    }

    @Override
    boolean hasError() {
        return resolveReport.hasError()
    }

    @Override
    Throwable getResolveError() {
        return new ResolveException(resolveReport)
    }
}
