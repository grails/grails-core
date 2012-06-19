package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.module.id.ModuleRevisionId

/**
 * Exception thrown when dependencies fail to resolve
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ResolveException extends RuntimeException {

    ResolveReport resolveReport;

    public ResolveException(ResolveReport resolveReport) {
        this.resolveReport = resolveReport;
    }

    @Override
    public String getMessage() {
        def configurations = resolveReport.configurations
        def unresolvedDependencies = []
        for(conf in configurations) {
            final confReport = resolveReport.getConfigurationReport(conf)
            for(IvyNode node in confReport.getUnresolvedDependencies()) {
                unresolvedDependencies << node.id
            }
            def failedDownloads = confReport.getFailedArtifactsReports()
            if(failedDownloads) {
                for(ArtifactDownloadReport dl in failedDownloads) {
                    unresolvedDependencies << dl.artifact.moduleRevisionId
                }
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
