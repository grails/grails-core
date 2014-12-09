package org.grails.gradle.plugin.core

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

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

/**
 * A Gradle plugin for Grails plugins
 *
 * @author Graeme Rocher
 * @since 3.0
 *
 */
class GrailsPluginGradlePlugin extends GrailsGradlePlugin {

    @Override
    void apply(Project project) {
        super.apply(project)

        def providedConfig = project.configurations.create("provided")
        def sourceSets = project.sourceSets
        def mainSourceSet = sourceSets.main

        project.sourceSets {
            def providedFiles = project.files(providedConfig)
            ast {
                groovy {
                    compileClasspath += project.configurations.compile + providedFiles
                }
            }
            main {
                compileClasspath += providedFiles + sourceSets.ast.output
            }
            test {
                compileClasspath += providedFiles + sourceSets.ast.output
                runtimeClasspath += providedFiles
            }
        }

        def projectTasks = project.tasks
        def configScriptTask = projectTasks.create('configScript')

        def configFile = project.file("$project.buildDir/config.groovy")
        configScriptTask.outputs.file(configFile)

        def projectName = project.name
        def projectVersion = project.version
        configScriptTask.inputs.property('name', projectName)
        configScriptTask.inputs.property('version', projectVersion)
        configScriptTask.doLast {
            configFile.parentFile.mkdirs()
            configFile.text = """
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '$projectVersion')
        classNode.putNodeMetaData('projectName', '$projectName')
    }
}
"""
        }

        ProcessResources processResources = (ProcessResources) projectTasks.getByName('processResources')

        def copyCommands = project.task(type:Copy, "copyCommands") {
            from "${project.projectDir}/src/main/scripts"
            into "${processResources.destinationDir}/META-INF/commands"
        }

        def copyTemplates = project.task(type:Copy, "copyTemplates") {
            from "${project.projectDir}/src/main/templates"
            into "${processResources.destinationDir}/META-INF/templates"
        }

        def copyAstClasses = project.task(type:Copy, "copyAstClasses") {
            from sourceSets.ast.output
            into mainSourceSet.output.classesDir
        }
        processResources.dependsOn(copyCommands, copyTemplates)
        projectTasks.getByName('compileGroovy').dependsOn(configScriptTask)
        projectTasks.getByName('classes').dependsOn(copyAstClasses)
        project.processResources {
            rename "application.yml", "plugin.yml"
            exclude "logback.groovy"
            exclude "spring/resources.groovy"
        }
        project.compileGroovy {
            groovyOptions.configurationScript = configFile
        }


        def javadocTask = projectTasks.findByName('javadoc')
        if(javadocTask) {
            projectTasks.create("javadocJar", Jar).configure {
                classifier = 'javadoc'
                from 'build/docs/javadoc'
            }.dependsOn(javadocTask)
            javadocTask.configure {
                classpath += project.configurations.provided + sourceSets.ast.output
            }
        }

        def sourcesJar = projectTasks.create("sourcesJar", Jar).configure {
            classifier = 'sources'
            from mainSourceSet.allSource
        }

    }
}
