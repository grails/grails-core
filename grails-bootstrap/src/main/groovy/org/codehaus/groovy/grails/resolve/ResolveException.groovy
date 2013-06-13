/*
 * Copyright 2011 the original author or authors.
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

import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.module.id.ModuleRevisionId

/**
 * Thrown when dependencies fail to resolve.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ResolveException extends RuntimeException {

    ResolveReport resolveReport

    ResolveException(ResolveReport resolveReport) {
        this.resolveReport = resolveReport
    }

    @Override
    String getMessage() {
        def unresolvedDependencies = []
        for (conf in resolveReport.configurations) {
            final confReport = resolveReport.getConfigurationReport(conf)
            for (IvyNode node in confReport.getUnresolvedDependencies()) {
                unresolvedDependencies << node.id
            }
            for (ArtifactDownloadReport dl in confReport.getFailedArtifactsReports()) {
                unresolvedDependencies << dl.artifact.moduleRevisionId
            }
        }
        def dependencies = unresolvedDependencies.collect { ModuleRevisionId mid ->
            "- ${mid.organisation}:${mid.name}:${mid.revision}"
        }.join(System.getProperty("line.separator"))
        return """Failed to resolve dependencies (Set log level to 'warn' in BuildConfig.groovy for more information):

$dependencies

"""
    }
}
