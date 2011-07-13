package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.report.ResolveReport

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
        IvyNode[] unresolvedDependencies = resolveReport.getUnresolvedDependencies();
        def dependencies = unresolvedDependencies.collect { IvyNode n ->
            def mid = n.id

            "- ${mid.organisation}:${mid.name}:${mid.revision}"
        }.join(System.getProperty("line.separator"))
        return """Failed to resolve dependencies (Set log level to 'warn' in BuildConfig.groovy for more information):

$dependencies

"""
    }
}
