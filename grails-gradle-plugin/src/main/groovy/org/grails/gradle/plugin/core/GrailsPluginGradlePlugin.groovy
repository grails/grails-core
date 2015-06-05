package org.grails.gradle.plugin.core

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import javax.inject.Inject

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

    @Inject
    GrailsPluginGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        configureAstSources(project)

        configureProjectNameAndVersionASTMetadata(project)

        configurePluginResources(project)

        configurePluginJarTask(project)

        configureSourcesJarTask(project)

    }

    @Override
    protected Task createBuildPropertiesTask(Project project) {
        // no-op
    }

    protected void configureSourcesJarTask(Project project) {
        def sourcesJar = project.tasks.create("sourcesJar", Jar).configure {
            classifier = 'sources'
            from project.sourceSets.main.allSource
        }
    }

    protected void configureAstSources(Project project) {
        def sourceSets = project.sourceSets
        def mainSourceSet = sourceSets.main

        project.sourceSets {
            ast {
                groovy {
                    compileClasspath += project.configurations.compile
                }
            }
            main {
                compileClasspath += sourceSets.ast.output
            }
            test {
                compileClasspath += sourceSets.ast.output
            }
        }

        def copyAstClasses = project.task(type: Copy, "copyAstClasses") {
            from sourceSets.ast.output
            into mainSourceSet.output.classesDir
        }
        project.tasks.getByName('classes').dependsOn(copyAstClasses)

        project.tasks.withType(JavaExec) {
            classpath += sourceSets.ast.output
        }

        def javadocTask = project.tasks.findByName('javadoc')
        def groovydocTask = project.tasks.findByName('groovydoc')
        if (javadocTask) {
            javadocTask.configure {
                source += sourceSets.ast.allJava
            }
        }

        if (groovydocTask) {
            project.tasks.create("javadocJar", Jar).configure {
                classifier = 'javadoc'
                from groovydocTask.outputs
            }.dependsOn(javadocTask)

            groovydocTask.configure {
                source += sourceSets.ast.allJava
            }
        }
    }

    protected void configurePluginJarTask(Project project) {
        project.jar {
            exclude "logback.groovy"
        }
    }

    protected void configurePluginResources(Project project) {
        project.afterEvaluate() {
            ProcessResources processResources = (ProcessResources) project.tasks.getByName('processResources')
            GrailsExtension grailsExtension = project.extensions.findByType(GrailsExtension)

            def processResourcesDependencies = []
            if(grailsExtension.packageAssets) {
                processResourcesDependencies << project.task(type: Copy, "copyAssets") {
                    from "${project.projectDir}/grails-app/assets/javascripts"
                    from "${project.projectDir}/grails-app/assets/stylesheets"
                    from "${project.projectDir}/grails-app/assets/images"
                    into "${processResources.destinationDir}/META-INF/assets"
                }
            }


            processResourcesDependencies << project.task(type: Copy, "copyCommands") {
                from "${project.projectDir}/src/main/scripts"
                into "${processResources.destinationDir}/META-INF/commands"
            }

            processResourcesDependencies << project.task(type: Copy, "copyTemplates") {
                from "${project.projectDir}/src/main/templates"
                into "${processResources.destinationDir}/META-INF/templates"
            }

            processResources.dependsOn(*processResourcesDependencies)
            project.processResources {
                rename "application.yml", "plugin.yml"
                exclude "spring/resources.groovy"
            }
        }

    }

    protected void configureProjectNameAndVersionASTMetadata(Project project) {
        def configScriptTask = project.tasks.create('configScript')

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
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
"""
        }
        project.tasks.getByName('compileGroovy').dependsOn(configScriptTask)
        project.compileGroovy {
            groovyOptions.configurationScript = configFile
        }
    }
}
