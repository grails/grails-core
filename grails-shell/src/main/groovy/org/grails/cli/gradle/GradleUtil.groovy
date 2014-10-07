package org.grails.cli.gradle

import grails.io.SystemOutErrCapturer
import grails.io.SystemStreamsRedirector

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.DefaultCancellationTokenSource
import org.grails.cli.profile.ExecutionContext

class GradleUtil {
    public static ProjectConnection openGradleConnection(File baseDir) {
        SystemOutErrCapturer.withCapturedOutput {
            GradleConnector.newConnector().forProjectDirectory(baseDir).connect()
        }
    }
    
    public static <T> T withProjectConnection(File baseDir, boolean suppressOutput=true, Closure<T> closure) {
        ProjectConnection projectConnection=openGradleConnection(baseDir)
        try {
            if(suppressOutput) {
                SystemOutErrCapturer.withCapturedOutput {
                    closure(projectConnection)
                }
            } else {
                SystemStreamsRedirector.withOriginalIO {
                    closure(projectConnection)
                }
            }
        } finally {
            projectConnection.close()
        }
    }
    
    public static wireCancellationSupport(ExecutionContext context, BuildLauncher buildLauncher) {
        DefaultCancellationTokenSource cancellationTokenSource = new DefaultCancellationTokenSource()
        buildLauncher.withCancellationToken(cancellationTokenSource.token())
        context.addCancelledListener({ cancellationTokenSource.cancel() })
    }
}
