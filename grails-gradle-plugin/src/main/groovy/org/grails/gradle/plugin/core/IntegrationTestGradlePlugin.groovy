/*
 * Copyright 2015 the original author or authors.
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

package org.grails.gradle.plugin.core

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.grails.gradle.plugin.util.SourceSets

/**
 * Gradle plugin for adding separate src/integration-test folder to hold integration tests
 *
 * Adds integrationTestCompile and integrationTestRuntime configurations that extend from testCompile and testRuntime
 *
 *
 */
class IntegrationTestGradlePlugin implements Plugin<Project> {
    boolean ideaIntegration = true
    String sourceFolderName = "src/integration-test"


    @Override
    @CompileStatic
    void apply(Project project) {
        File[] sourceDirs = findIntegrationTestSources(project)
        if(sourceDirs) {
            List<File> acceptedSourceDirs = []
            SourceSetContainer sourceSets = SourceSets.findSourceSets(project)
            SourceSet integrationTest = sourceSets.create("integrationTest")
            for(File srcDir in acceptedSourceDirs ) {
                if (integrationTest.hasProperty(srcDir.name)) {
                    registerSourceDir(integrationTest, srcDir)
                    acceptedSourceDirs.add srcDir
                }
            }
            DependencyHandler dependencies = project.dependencies
            dependencies.add("integrationTestCompile", SourceSets.findMainSourceSet(project).output)
            dependencies.add("integrationTestCompile", SourceSets.findSourceSet(project, SourceSet.TEST_SOURCE_SET_NAME).output)
            dependencies.add("integrationTestCompile", project.configurations.findByName("testCompile"))
            dependencies.add("integrationTestRuntime", project.configurations.findByName("testRuntime"))

            TaskContainer tasks = project.tasks
            Test integrationTestTask = tasks.create('integrationTest', Test)
            integrationTestTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
            integrationTestTask.setTestClassesDir( integrationTest.output.classesDir )
            integrationTestTask.classpath = integrationTest.runtimeClasspath
            integrationTestTask.maxParallelForks = 1
            integrationTestTask.reports.html.enabled = false
            integrationTestTask.shouldRunAfter("test")

            tasks.findByName("check")?.dependsOn(integrationTest)

            TestReport testReportTask = tasks.create("mergeTestReports", TestReport)
            testReportTask.destinationDir = project.file("$project.buildDir/reports/tests")

            // These must point to the binary test results directory generated by a Test task instance.
            // If Test task instances are specified directly, this task would depend on them and run them.
            testReportTask.reportOn project.files("$project.buildDir/test-results/binary/test", "$project.buildDir/test-results/binary/integrationTest"),
                    // different versions of Gradle store these results in different places. ugh.
                    project.files("$project.buildDir/test-results/test/binary", "$project.buildDir/test-results/integrationTest/binary")

            integrationTestTask.finalizedBy testReportTask

            if(ideaIntegration) {
                integrateIdea(project, acceptedSourceDirs)
            }
        }
    }

    private void registerSourceDir(SourceSet integrationTest, File srcDir) {
        integrationTest."${srcDir.name}".srcDir srcDir
    }

    private integrateIdea(Project project, List<File> acceptedSourceDirs) {
        project.afterEvaluate {
            if (project.convention.findByName('idea')) {
                // IDE integration for IDEA. Eclipse plugin already handles all source folders.
                project.idea {
                    module {
                        acceptedSourceDirs.each {
                            testSourceDirs += it
                        }
                    }
                }
            }
        }
    }

    @CompileStatic
    File[] findIntegrationTestSources(Project project) {
        project.file(sourceFolderName).listFiles({File file-> file.isDirectory() && !file.name.contains('.')} as FileFilter)
    }
}
