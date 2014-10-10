/*
 * Copyright 2014 original authors
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
package org.grails.cli.gradle

import grails.io.SystemOutErrCapturer
import grails.io.SystemStreamsRedirector
import groovy.transform.CompileStatic
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.DefaultCancellationTokenSource
import org.grails.cli.profile.ExecutionContext

/**
 * Utility methods for interacting with Gradle
 *
 * @since 3.0
 * @author Graeme Rocher
 * @author Lari Hotari
 */
@CompileStatic
class GradleUtil {

    private static ProjectConnection preparedConnection = null

    public static ProjectConnection refreshConnection(File baseDir) {
        preparedConnection = GradleConnector.newConnector().forProjectDirectory(baseDir).connect()

        Runtime.addShutdownHook {
            preparedConnection?.close()
        }
        return preparedConnection
    }

    public static ProjectConnection prepareConnection(File baseDir) {
        if(preparedConnection == null) {
            refreshConnection(baseDir)
        }
        return preparedConnection
    }

    public static ProjectConnection openGradleConnection(File baseDir) {
        SystemOutErrCapturer.withCapturedOutput {
            GradleConnector gradleConnector = GradleConnector.newConnector().forProjectDirectory(baseDir)
            if(System.getenv("GRAILS_GRADLE_HOME")) {
                gradleConnector.useInstallation(new File(System.getenv("GRAILS_GRADLE_HOME")))
            }
            gradleConnector.connect()
        }
    }
    
    public static <T> T withProjectConnection(File baseDir, boolean suppressOutput=true, Closure<T> closure) {
        ProjectConnection projectConnection= preparedConnection ?: openGradleConnection(baseDir)
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
            if(!preparedConnection)
                projectConnection.close()
        }
    }
    
    public static wireCancellationSupport(ExecutionContext context, BuildLauncher buildLauncher) {
        DefaultCancellationTokenSource cancellationTokenSource = new DefaultCancellationTokenSource()
        buildLauncher.withCancellationToken(cancellationTokenSource.token())
        context.addCancelledListener({ cancellationTokenSource.cancel() })
    }
}
