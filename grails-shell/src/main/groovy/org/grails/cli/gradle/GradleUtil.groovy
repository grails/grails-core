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

import grails.build.logging.GrailsConsole
import grails.io.SystemOutErrCapturer
import grails.io.SystemStreamsRedirector
import groovy.transform.CompileStatic

import org.gradle.tooling.*
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
    private static File preparedConnectionBaseDir = null

    public static ProjectConnection refreshConnection(File baseDir) {
        preparedConnection = openGradleConnection(baseDir)
        preparedConnectionBaseDir = baseDir.getAbsoluteFile()
        
        try {
            Runtime.addShutdownHook {
                clearPreparedConnection()
            }
        } catch (e) {
            // ignore
        }
        return preparedConnection
    }

    public static ProjectConnection prepareConnection(File baseDir) {
        if(preparedConnection == null || preparedConnectionBaseDir != baseDir.getAbsoluteFile()) {
            refreshConnection(baseDir)
        }
        return preparedConnection
    }
    
    public static clearPreparedConnection() {
        if(preparedConnection != null) {
            preparedConnection.close()
            preparedConnection = null
            preparedConnectionBaseDir = null
        }
    }

    public static ProjectConnection openGradleConnection(File baseDir) {
        SystemOutErrCapturer.withNullOutput {
            GradleConnector gradleConnector = GradleConnector.newConnector().forProjectDirectory(baseDir)
            if(System.getenv("GRAILS_GRADLE_HOME")) {
                gradleConnector.useInstallation(new File(System.getenv("GRAILS_GRADLE_HOME")))
            }
            gradleConnector.connect()
        }
    }
    
    public static <T> T withProjectConnection(File baseDir, boolean suppressOutput=true, Closure<T> closure) {
        boolean preparedConnectionExisted = preparedConnection != null
        ProjectConnection projectConnection = prepareConnection(baseDir)
        try {
            if(suppressOutput) {
                SystemOutErrCapturer.withNullOutput {
                    closure(projectConnection)
                }
            } else {
                SystemStreamsRedirector.withOriginalIO {
                    closure(projectConnection)
                }
            }
        } finally {
            if(!preparedConnectionExisted)
                clearPreparedConnection()
        }
    }
    
    public static void runBuildWithConsoleOutput(ExecutionContext context, Closure<?> buildLauncherCustomizationClosure) {
        GrailsConsole grailsConsole = context.getConsole()
        withProjectConnection(context.getBaseDir(), true) { ProjectConnection projectConnection ->
            BuildLauncher launcher = projectConnection.newBuild()
            launcher.colorOutput = grailsConsole.isAnsiEnabled()
            launcher.setStandardOutput(grailsConsole.out)
            wireCancellationSupport(context, launcher)
            buildLauncherCustomizationClosure.call(launcher)
            launcher.run()
        }
    }
    
    public static wireCancellationSupport(ExecutionContext context, BuildLauncher buildLauncher) {
        DefaultCancellationTokenSource cancellationTokenSource = new DefaultCancellationTokenSource()
        buildLauncher.withCancellationToken(cancellationTokenSource.token())
        context.addCancelledListener({
            cancellationTokenSource.cancel()
        })
    }
}
