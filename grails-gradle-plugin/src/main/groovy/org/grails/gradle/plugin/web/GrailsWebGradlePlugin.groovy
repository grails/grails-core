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
package org.grails.gradle.plugin.web

import grails.util.Environment
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.grails.gradle.plugin.commands.ApplicationContextCommandTask
import org.grails.gradle.plugin.core.GrailsGradlePlugin

import javax.inject.Inject

/**
 * Adds web specific extensions
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GrailsWebGradlePlugin extends GrailsGradlePlugin {
    @Inject
    GrailsWebGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        TaskContainer taskContainer = project.tasks
        if (taskContainer.findByName("urlMappingsReport") == null) {
            taskContainer.create(name: "urlMappingsReport", type: ApplicationContextCommandTask, overwrite: true) {
                classpath = project.sourceSets.main.runtimeClasspath + project.configurations.console
                systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.name)
                command = 'url-mappings-report'
            }
        }

    }
}
